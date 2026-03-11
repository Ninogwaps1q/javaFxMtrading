package UserController;

import config.config;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javafx.scene.control.Alert;

public final class OrderValidationUtil {

    private OrderValidationUtil() {
    }

    public static String getMissingContactMessage(int userId) {
        String sql = "SELECT TRIM(COALESCE(u_address, '')) AS u_address, "
                + "TRIM(COALESCE(u_phone, '')) AS u_phone "
                + "FROM tbl_acc WHERE u_id = ?";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return "Unable to verify your profile. Please sign in again.";
                }

                boolean missingAddress = rs.getString("u_address").isEmpty();
                boolean missingPhone = rs.getString("u_phone").isEmpty();

                if (missingAddress && missingPhone) {
                    return "Please add your address and phone number in Profile before placing an order.";
                }
                if (missingAddress) {
                    return "Please add your address in Profile before placing an order.";
                }
                if (missingPhone) {
                    return "Please add your phone number in Profile before placing an order.";
                }

                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Unable to verify your profile. Please try again.";
        }
    }

    public static void showProfileRequirementAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Profile Required");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
