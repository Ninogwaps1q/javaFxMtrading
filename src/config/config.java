package config;

import AdminController.AdminSession;
import UserController.UserSession;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class config {

    // STATIC CONNECT (use everywhere: config.connectDB())
    public static Connection connectDB() {
        Connection con = null;
        try {
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:data.db"); // <-- your db file
            try (Statement stmt = con.createStatement()) {
                stmt.execute("PRAGMA busy_timeout = 5000");
            }
            System.out.println("Connection Successful");
        } catch (Exception e) {
            System.out.println("Connection Failed: " + e);
        }
        return con;
    }

    public void addRecord(String sql, Object... values) {
        try (Connection conn = connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            bindParameters(pstmt, values);

            pstmt.executeUpdate();
            System.out.println("Record added successfully!");
        } catch (SQLException e) {
            System.out.println("Error adding record: " + e.getMessage());
        }
    }

    public static int deleteRecord(String sql, Object... values) {
        try (Connection conn = connectDB()) {
            if (conn == null) return 0;
            return deleteRecord(conn, sql, values);
        } catch (SQLException e) {
            System.out.println("Error deleting record: " + e.getMessage());
            return 0;
        }
    }

    public static int deleteRecord(Connection conn, String sql, Object... values) throws SQLException {
        if (conn == null) return 0;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindParameters(pstmt, values);
            return pstmt.executeUpdate();
        }
    }

    // UPDATED LOGIN: saves USER session for User, AdminSession for Admin/Cashier
    public String login(String loginInput, String pass) {
        String role = null;
        Connection conn = null;

        String sql = "SELECT u_id, u_name, u_email, u_role, u_status " +
                     "FROM tbl_acc " +
                     "WHERE (u_uname = ? OR u_email = ?) AND u_password = ?";

        String hash = hashPassword(pass);

        try {
            conn = config.connectDB();
            if (conn == null) return null;

            conn.setAutoCommit(false);
            AuditLogService.ensureAuditLogTable(conn);

            int id;
            String name;
            String email;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, loginInput);
                pstmt.setString(2, loginInput);
                pstmt.setString(3, hash);

                try (ResultSet rs = pstmt.executeQuery()) {

                    if (!rs.next()) {
                        conn.rollback();
                        System.out.println("Invalid credentials.");
                        return null;
                    }

                    String status = rs.getString("u_status");
                    if (!"Approved".equalsIgnoreCase(status)) {
                        conn.rollback();
                        System.out.println("Account pending approval.");
                        return null;
                    }

                    id = rs.getInt("u_id");
                    name = rs.getString("u_name");
                    email = rs.getString("u_email");
                    role = rs.getString("u_role");
                }
            }

            SessionAuditUtil.closeAnyActiveSessions(conn);
            int loginLogId = AuditLogService.recordLogin(conn, id, name, email, role);
            conn.commit();

            if ("User".equalsIgnoreCase(role)) {
                UserSession.set(id, name, email, loginLogId);
            } else {
                AdminSession.setAdmin(id, name, email, role, loginLogId);
            }

            System.out.println("Login successful: " + name + " | " + email + " | " + role);

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.out.println("Login rollback error: " + rollbackEx.getMessage());
                }
            }
            System.out.println("Login error: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    System.out.println("Connection close error: " + closeEx.getMessage());
                }
            }
        }

        return role;
    }

    public boolean recordExists(String sql, Object... values) {
        try (Connection conn = connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            bindParameters(pstmt, values);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void updatePassword(String email, String hashedPassword) {
        String sql = "UPDATE tbl_acc SET u_password = ? WHERE u_email = ?";
        try (Connection conn = connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setString(2, email);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ⚠️ Security note: consider moving these to env later
    public boolean sendEmail(String to, String subject, String body) {
        final String from = "jaycavalidamanabat@gmail.com";
        final String password = "wvdb zgnn sgcb xejz";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from, "Melynal Trading"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int generateResetCode() {
        return (int)(Math.random() * 900000) + 100000;
    }

    public void sendResetCodeEmail(String fullname, String email, int code) {
        String subject = "Password Reset - Melynal Trading";
        String body = "Hello " + fullname + ",\n\n" +
                "You requested a password reset.\n" +
                "Your reset code is: " + code + "\n\n" +
                "If you did not request this, ignore this email.\n\n" +
                "Melynal Trading System";
        sendEmail(email, subject, body);
    }

    private static void bindParameters(PreparedStatement pstmt, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            pstmt.setObject(i + 1, values[i]);
        }
    }
}
