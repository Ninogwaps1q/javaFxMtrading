package config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AuditLogService {

    private static final DateTimeFormatter DB_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuditLogService() {
    }

    public static void ensureAuditLogTable() {
        try (Connection conn = config.connectDB()) {
            ensureAuditLogTable(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ensureAuditLogTable(Connection conn) {
        if (conn == null) return;

        String sql = "CREATE TABLE IF NOT EXISTS tbl_login_logs ("
                + "log_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "u_id INTEGER, "
                + "u_name TEXT, "
                + "u_email TEXT, "
                + "u_role TEXT, "
                + "login_at TEXT NOT NULL, "
                + "logout_at TEXT"
                + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int recordLogin(int userId, String name, String email, String role) {
        try (Connection conn = config.connectDB()) {
            if (conn == null) return 0;
            return recordLogin(conn, userId, name, email, role);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static int recordLogin(Connection conn, int userId, String name, String email, String role) {
        if (conn == null) return 0;

        String sql = "INSERT INTO tbl_login_logs(u_id, u_name, u_email, u_role, login_at) "
                + "VALUES(?, ?, ?, ?, ?)";

        ensureAuditLogTable(conn);

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, safe(name));
            ps.setString(3, safe(email));
            ps.setString(4, safe(role));
            ps.setString(5, now());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static void recordLogout(int loginLogId) {
        if (loginLogId <= 0) return;

        try (Connection conn = config.connectDB()) {
            if (conn == null) return;
            recordLogout(conn, loginLogId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void recordLogout(Connection conn, int loginLogId) {
        if (conn == null || loginLogId <= 0) return;

        String sql = "UPDATE tbl_login_logs "
                + "SET logout_at = ? "
                + "WHERE log_id = ? AND (logout_at IS NULL OR TRIM(logout_at) = '')";

        ensureAuditLogTable(conn);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, now());
            ps.setInt(2, loginLogId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String now() {
        return LocalDateTime.now().format(DB_DATE_TIME);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
