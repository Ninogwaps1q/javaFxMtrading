package config;

import Model.VoucherDiscount;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class VoucherDataUtil {

    public static final String TYPE_PERCENT = "Percent";
    public static final String TYPE_FIXED = "Fixed Amount";

    private VoucherDataUtil() {
    }

    public static void ensureVoucherTable() {
        try (Connection conn = config.connectDB()) {
            if (conn == null) return;
            ensureVoucherTable(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ensureVoucherTable(Connection conn) throws SQLException {
        if (conn == null) return;

        String sql = "CREATE TABLE IF NOT EXISTS tbl_vouchers ("
                + "voucher_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "code TEXT NOT NULL UNIQUE, "
                + "discount_type TEXT NOT NULL, "
                + "discount_value REAL NOT NULL, "
                + "minimum_order REAL NOT NULL DEFAULT 0, "
                + "expires_at TEXT, "
                + "is_active INTEGER NOT NULL DEFAULT 1, "
                + "created_at TEXT NOT NULL DEFAULT (datetime('now'))"
                + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    public static VoucherDiscount validateVoucher(String rawCode, double grossTotal) {
        try (Connection conn = config.connectDB()) {
            if (conn == null) {
                return VoucherDiscount.invalid("Unable to connect to the voucher database.", grossTotal);
            }
            ensureVoucherTable(conn);
            return validateVoucher(conn, rawCode, grossTotal);
        } catch (Exception e) {
            e.printStackTrace();
            return VoucherDiscount.invalid("Unable to validate the voucher right now.", grossTotal);
        }
    }

    public static VoucherDiscount validateVoucher(Connection conn, String rawCode, double grossTotal) throws SQLException {
        String code = normalizeCode(rawCode);
        if (code.isEmpty()) {
            return VoucherDiscount.none(grossTotal);
        }

        String sql = "SELECT code, discount_type, discount_value, minimum_order, "
                + "COALESCE(expires_at, '') AS expires_at, COALESCE(is_active, 0) AS is_active "
                + "FROM tbl_vouchers WHERE UPPER(code) = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return VoucherDiscount.invalid("Voucher code was not found.", grossTotal);
                }

                if (rs.getInt("is_active") != 1) {
                    return VoucherDiscount.invalid("This voucher is inactive.", grossTotal);
                }

                double minimumOrder = rs.getDouble("minimum_order");
                if (grossTotal < minimumOrder) {
                    return VoucherDiscount.invalid(
                            String.format(Locale.ENGLISH,
                                    "This voucher needs a minimum order of PHP %.2f.", minimumOrder),
                            grossTotal);
                }

                String expiresAt = rs.getString("expires_at");
                if (!expiresAt.trim().isEmpty() && isExpired(expiresAt)) {
                    return VoucherDiscount.invalid("This voucher is already expired.", grossTotal);
                }

                String discountType = normalizeType(rs.getString("discount_type"));
                double discountValue = Math.max(0.0, rs.getDouble("discount_value"));
                double discountAmount;

                if (TYPE_PERCENT.equals(discountType)) {
                    discountAmount = grossTotal * (discountValue / 100.0);
                } else {
                    discountAmount = discountValue;
                }

                discountAmount = Math.max(0.0, Math.min(grossTotal, roundMoney(discountAmount)));
                double payableTotal = roundMoney(Math.max(0.0, grossTotal - discountAmount));

                return VoucherDiscount.valid(
                        rs.getString("code"),
                        discountType,
                        discountValue,
                        discountAmount,
                        minimumOrder,
                        grossTotal,
                        payableTotal
                );
            }
        }
    }

    public static String normalizeCode(String rawCode) {
        return rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ENGLISH);
    }

    private static String normalizeType(String type) {
        if (type == null) return TYPE_FIXED;
        String value = type.trim().toLowerCase(Locale.ENGLISH);
        if (value.contains("percent")) return TYPE_PERCENT;
        return TYPE_FIXED;
    }

    private static boolean isExpired(String rawDate) {
        LocalDate expiryDate = parseExpiryDate(rawDate);
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    private static LocalDate parseExpiryDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) return null;

        String value = rawDate.trim();
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            // Try the next format.
        }

        try {
            if (value.length() >= 10) {
                return LocalDate.parse(value.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (DateTimeParseException ignored) {
            // Try the next format.
        }

        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH));
        } catch (DateTimeParseException ignored) {
            // Give up.
        }
        return null;
    }

    private static double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
