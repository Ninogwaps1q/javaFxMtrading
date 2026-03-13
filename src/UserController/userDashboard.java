
package UserController;

import config.SessionAuditUtil;
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


public class userDashboard implements Initializable {

    @FXML
    private HBox navPanel;
    @FXML
    private ImageView navLogo;
    @FXML
    private Label homeBtn;
    @FXML
    private Label aboutBtn;
    @FXML
    private Label profileBtn;
    @FXML
    private Label logoutBtn;
    @FXML
    private VBox hero;
    @FXML
    private ImageView heroLogo;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        makeCircle(navLogo);
        makeCircle(heroLogo);
    } 
    
    private void makeCircle(ImageView imageView) {
        double w = imageView.getFitWidth();
        double h = imageView.getFitHeight();
        double radius = Math.min(w, h) / 2.0;

        Circle clip = new Circle(w / 2.0, h / 2.0, radius);
        imageView.setClip(clip);
    }
    
    @FXML
    private void homeHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/UserDashboard.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void aboutHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/About.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void profileHandlebtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/UserProfile.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }
    

    @FXML
    private void handleLogoutBtn(MouseEvent event) throws IOException {
        SessionAuditUtil.logoutUserSession();
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void productHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/userProduct.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void orderHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/userOrder.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void shopHandlebtn(MouseEvent event) throws IOException {
         Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/userProduct.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    
}
