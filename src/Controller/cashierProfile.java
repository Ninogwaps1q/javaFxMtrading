package Controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class cashierProfile implements Initializable {

    @FXML
    private HBox navPanel;
    @FXML
    private ImageView navLogo;
    @FXML
    private Label homeBtn;
    @FXML
    private Label aboutBtn;
    @FXML
    private Label logoutBtn;
    @FXML
    private VBox hero;

    // PROFILE LABELS
    @FXML
    private Label nameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Label profileBtn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // SAME SESSION AS ADMIN
        nameLabel.setText("Name: " + AdminSession.getName());
        emailLabel.setText("Email: " + AdminSession.getEmail());
        roleLabel.setText("Role: " + AdminSession.getRole());
    }

    @FXML
    private void homeHandleBtn(MouseEvent event) {
        // navigation if needed
    }

    @FXML
    private void aboutHandleBtn(MouseEvent event) {
        // navigation if needed
    }

    @FXML
    private void handleLogoutBtn(MouseEvent event) {
        AdminSession.clear();
        // load login scene if needed
    }

    @FXML
    private void profileHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/CashierFXML/CashierProfile.fxml"));
        Scene sc = new Scene(root, 800, 500);
        sc.getStylesheets().add(getClass().getResource("/css/cashier.css").toExternalForm());

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }
}
