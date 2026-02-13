package Controller;

import config.config;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ForgotPasswordController {

    @FXML private TextField emailInput;
    @FXML private TextField codeInput;
    @FXML private PasswordField newPassInput;

    private config con = new config();
    private int generatedCode;
    private String currentEmail;
    @FXML
    private Button sendCodeBtn;
    @FXML
    private Button resetBtn;

    @FXML
    private void sendCodeAction(ActionEvent event) {
        String email = emailInput.getText().trim();
        if(email.isEmpty()) { showAlert("Enter your email!"); return; }

        // Check if email exists
        String sql = "SELECT u_name FROM tbl_acc WHERE u_email = ?";
        if(!con.recordExists(sql, email)) { showAlert("Email not registered!"); return; }

        // Generate code and send email
        generatedCode = con.generateResetCode();
        currentEmail = email;

        // Get fullname
        String fullname = "";
        try (Connection conn = con.connectDB();
             PreparedStatement ps = conn.prepareStatement("SELECT u_name FROM tbl_acc WHERE u_email=?")) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) fullname = rs.getString("u_name");
        } catch(Exception e) { e.printStackTrace(); }

        con.sendResetCodeEmail(fullname, email, generatedCode);
        showAlert("Reset code sent to your email!");
    }

    @FXML
        private void resetPasswordAction(ActionEvent event) {
            if(currentEmail == null) { 
                showAlert("Send code first!"); 
                return; 
            }

            String codeStr = codeInput.getText().trim();
            String newPass = newPassInput.getText().trim();

            if(codeStr.isEmpty() || newPass.isEmpty()) { 
                showAlert("Fill all fields!"); 
                return; 
            }

            int code;
            try { 
                code = Integer.parseInt(codeStr); 
            } catch(Exception e) { 
                showAlert("Code must be numeric!"); 
                return; 
            }

            if(code != generatedCode) { 
                showAlert("Invalid code!"); 
                return; 
            }

            // Update password
            con.updatePassword(currentEmail, con.hashPassword(newPass));
            showAlert("Password reset successfully!");

            // Clear fields
            codeInput.clear();
            newPassInput.clear();
            emailInput.clear();
            currentEmail = null;

            // ================= Redirect to Login.fxml =================
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
                Scene sc = new Scene(root, 1000, 600);
                sc.getStylesheets().add(getClass().getResource("/css/Main.css").toExternalForm());

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(sc);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error opening Login screen!");
            }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @FXML
    private void backHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Scene sc = new Scene(root, 1000, 600);
        sc.getStylesheets().add(getClass().getResource("/css/Main.css").toExternalForm());

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }
}
