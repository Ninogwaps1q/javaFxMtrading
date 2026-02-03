
package config;

import Controller.AdminSession;
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
    
    public static Connection connectDB() {
        Connection con = null;
        try {
            Class.forName("org.sqlite.JDBC"); // Load the SQLite JDBC driver
            con = DriverManager.getConnection("jdbc:sqlite:data.db"); // Establish connection
            System.out.println("Connection Successful");
        } catch (Exception e) {
            System.out.println("Connection Failed: " + e);
        }
        return con;
    }
    
    public void addRecord(String sql, Object... values) {
        try (Connection conn = connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < values.length; i++) {
                pstmt.setObject(i + 1, values[i]);
            }

            pstmt.executeUpdate();
            System.out.println("Record added successfully!");
        } catch (SQLException e) {
            System.out.println("Error adding record: " + e.getMessage());
        }
    }

    public String login(String loginInput, String pass) {
    String role = null;

    String sql = "SELECT u_name, u_email, u_role, u_status " +
                 "FROM tbl_acc " +
                 "WHERE (u_uname = ? OR u_email = ?) AND u_password = ?";

    String hash = hashPassword(pass);

    try (Connection conn = this.connectDB();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, loginInput);
        pstmt.setString(2, loginInput);
        pstmt.setString(3, hash);

        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {

                String status = rs.getString("u_status");

                if ("Approved".equalsIgnoreCase(status)) {

                    String name  = rs.getString("u_name");
                    String email = rs.getString("u_email");
                    role         = rs.getString("u_role");

                    // ✅ SAVE LOGGED-IN USER
                    AdminSession.setAdmin(name, email, role);

                    System.out.println("Login successful!");
                    System.out.println(name + " | " + email + " | " + role);

                } else {
                    System.out.println("Account pending approval.");
                    return null;
                }

            } else {
                System.out.println("Invalid credentials.");
            }
        }

    } catch (SQLException e) {
        System.out.println("Login error: " + e.getMessage());
    }

    return role;
}

    
    public boolean recordExists(String sql, Object... values) {
        try (Connection conn = connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < values.length; i++) {
                pstmt.setObject(i + 1, values[i]);
            }

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
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
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
    
    public void sendEmail(String to, String subject, String body) {
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
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ================== RESET CODE ==================
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
}
