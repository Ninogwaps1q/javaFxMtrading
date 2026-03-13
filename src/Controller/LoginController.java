
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
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;


public class LoginController implements Initializable {

    @FXML
    private ImageView logo;
    @FXML
    private TextField UsernameLogin;
    @FXML
    private Button loginBtn;
    @FXML
    private Label goRegister;
    @FXML
    private PasswordField passwordLogin;
    @FXML
    private Label forgotBtn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        makeCircle(logo);
    }    
    
     private void makeCircle(ImageView imageView) {
        double w = imageView.getFitWidth();
        double h = imageView.getFitHeight();
        double radius = Math.min(w, h) / 2.0;

        Circle clip = new Circle(w / 2.0, h / 2.0, radius);
        imageView.setClip(clip);
    }

    @FXML
    private void handleForgotClick(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Main/ForgotPassword.fxml"));
        Scene sc = new Scene(root, 1000, 600);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }

    @FXML
    private void handleCreataAcc(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Register.fxml"));
        Scene sc = new Scene(root, 1000, 600);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }

    @FXML
    private void loginButtonAction(ActionEvent event) {
        String loginInput = UsernameLogin.getText().trim();
        String pass = passwordLogin.getText().trim();

        config con = new config();

        if (loginInput.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Login Error", "Please enter both username/email and password.");
            return;
        }

        String role = con.login(loginInput, pass);

        if (role == null) {
            showAlert(Alert.AlertType.ERROR, "Login Failed",
                    "Invalid username/email or password, or account not approved yet.");
            return;
        }

        String fxmlFile;
        if ("Admin".equalsIgnoreCase(role)) {
            fxmlFile = "/AdminFXML/AdminDashboard.fxml";
        } else if ("Cashier".equalsIgnoreCase(role)) {
            fxmlFile = "/CashierFXML/CashierDashboard.fxml";
        } else {
            fxmlFile = "/UserFXML/UserDashboard.fxml";
        }

        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Scene scene = new Scene(root, 1300, 800);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load dashboard.");
        }
    }


    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
}
