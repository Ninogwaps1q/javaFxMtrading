package Controller;

import config.config;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class RegisterController implements Initializable {

    @FXML
    private TextField nameInput;
    @FXML
    private TextField emailInput;
    @FXML
    private TextField unameInput;
    @FXML
    private TextField passInput;
    @FXML
    private Button registerBtn;
    @FXML
    private Label goLogin;
    @FXML
    private ComboBox<String> typeCombo;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeCombo.getItems().addAll("Admin", "User", "Cashier");
        typeCombo.setPromptText("Select Type"); // Optional placeholder
    }

    @FXML
    private void handleGoLoginClick(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Scene sc = new Scene(root, 1000, 600);
        sc.getStylesheets().add(getClass().getResource("/css/Main.css").toExternalForm());

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }

    @FXML
    private void registerButtonAction(ActionEvent event) throws IOException {
        config con = new config();

        String name = nameInput.getText().trim();
        String email = emailInput.getText().trim();
        String uname = unameInput.getText().trim();
        String pass = passInput.getText().trim();
        String role = typeCombo.getValue(); // ✅ Use getValue() here

        if (name.isEmpty() || email.isEmpty() || uname.isEmpty() || pass.isEmpty() || role == null) {
            showAlert("All fields including type are required!", false);
            return;
        }

        String checkEmail = "SELECT 1 FROM tbl_acc WHERE u_email = ?";
        if (con.recordExists(checkEmail, email)) {
            showAlert("Email already exists!", false);
            return;
        }

        String checkUname = "SELECT 1 FROM tbl_acc WHERE u_uname = ?";
        if (con.recordExists(checkUname, uname)) {
            showAlert("Username already exists!", false);
            return;
        }

        String hashedPass = con.hashPassword(pass);

        String sql = "INSERT INTO tbl_acc " +
                "(u_name, u_email, u_uname, u_password, u_role, u_status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        con.addRecord(sql, name, email, uname, hashedPass, role, "Pending");

        showAlert("Registration successful!", true);

        nameInput.clear();
        emailInput.clear();
        unameInput.clear();
        passInput.clear();
        typeCombo.setValue(null); // Clear selection

        // Redirect to login
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Scene sc = new Scene(root, 1000, 600);
        sc.getStylesheets().add(getClass().getResource("/css/Main.css").toExternalForm());

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }

    private void showAlert(String message, boolean success) {
        Alert.AlertType type = success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
        Alert alert = new Alert(type);
        alert.setTitle(success ? "Success" : "Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
