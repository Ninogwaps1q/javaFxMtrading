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
import javafx.scene.shape.Circle;
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
    @FXML
    private ImageView profileImage;
    @FXML
    private Label navText;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        makeCircle(navLogo);
        // SAME SESSION AS ADMIN
        nameLabel.setText("Name: " + AdminSession.getName());
        emailLabel.setText("Email: " + AdminSession.getEmail());
        roleLabel.setText("Role: " + AdminSession.getRole());
    }
    
     private void makeCircle(ImageView imageView) {
        double radius = Math.min(imageView.getFitWidth(), imageView.getFitHeight()) / 2;
        Circle clip = new Circle(radius, radius, radius);
        imageView.setClip(clip);
    } 

    
    @FXML
    private void homeHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/CashierFXML/CashierDashboard.fxml"));
        Scene sc = new Scene(root, 800, 500);
        sc.getStylesheets().add(getClass().getResource("/css/cashier.css").toExternalForm());

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }

    @FXML
    private void aboutHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/CashierFXML/CashierAbout.fxml"));
        Scene sc = new Scene(root, 800, 500);
        sc.getStylesheets().add(getClass().getResource("/css/cashier.css").toExternalForm());

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }

    @FXML
    private void handleLogoutBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Scene sc = new Scene(root, 800, 500);
        sc.getStylesheets().add(getClass().getResource("/css/Main.css").toExternalForm());

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
            
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
