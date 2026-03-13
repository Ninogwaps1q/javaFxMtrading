package config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ReviewDataUtil {

    private ReviewDataUtil() {
    }

    public static void ensureReviewTable() {
        try (Connection conn = config.connectDB()) {
            if (conn == null) return;
            ensureReviewTable(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ensureReviewTable(Connection conn) throws SQLException {
        if (conn == null) return;

        createReviewTable(conn);
        addColumnIfMissing(conn, "tbl_review", "review_text", "TEXT");
        addColumnIfMissing(conn, "tbl_review", "review_image", "TEXT");
        addColumnIfMissing(conn, "tbl_review", "created_at", "TEXT");
        migrateLegacyReviewTable(conn);
    }

    private static void createReviewTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS tbl_review ("
                + "review_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "o_id INTEGER NOT NULL, "
                + "p_id INTEGER NOT NULL, "
                + "u_id INTEGER NOT NULL, "
                + "rating INTEGER NOT NULL, "
                + "review_text TEXT, "
                + "review_image TEXT, "
                + "created_at TEXT NOT NULL, "
                + "UNIQUE(o_id, p_id, u_id)"
                + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private static void migrateLegacyReviewTable(Connection conn) throws SQLException {
        if (!tableExists(conn, "tbl_reviews")) return;

        String reviewTextExpr = columnExists(conn, "tbl_reviews", "review_text")
                ? "COALESCE(review_text, '')"
                : "''";
        String reviewImageExpr = columnExists(conn, "tbl_reviews", "review_image")
                ? "COALESCE(review_image, '')"
                : "''";
        String createdAtExpr = columnExists(conn, "tbl_reviews", "created_at")
                ? "COALESCE(created_at, datetime('now'))"
                : "datetime('now')";

        String sql = "INSERT OR IGNORE INTO tbl_review("
                + "o_id, p_id, u_id, rating, review_text, review_image, created_at"
                + ") "
                + "SELECT o_id, p_id, u_id, COALESCE(rating, 0), "
                + reviewTextExpr + ", "
                + reviewImageExpr + ", "
                + createdAtExpr + " "
                + "FROM tbl_reviews";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String type) throws SQLException {
        if (columnExists(conn, table, column)) return;

        try (PreparedStatement ps = conn.prepareStatement(
                "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type)) {
            ps.executeUpdate();
        }
    }

    private static boolean tableExists(Connection conn, String table) throws SQLException {
        String sql = "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws SQLException {
        String sql = "PRAGMA table_info(" + table + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
