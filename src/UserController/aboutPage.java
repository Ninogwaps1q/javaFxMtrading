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
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class aboutPage implements Initializable {

    @FXML private ImageView navLogo;
    @FXML private ImageView aboutLogo;
    @FXML private Label homeBtn;
    @FXML private Label productBtn;
    @FXML private Label aboutBtn;
    @FXML private Label profileBtn;
    @FXML private Label logoutBtn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        makeCircle(navLogo);
        makeCircle(aboutLogo);
    }

    private void makeCircle(ImageView imageView) {
        if (imageView == null) return;

        double w = imageView.getFitWidth();
        double h = imageView.getFitHeight();
        double radius = Math.min(w, h) / 2.0;

        Circle clip = new Circle(w / 2.0, h / 2.0, radius);
        imageView.setClip(clip);
    }

    private void openPage(String fxml, MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void homeHandleBtn(MouseEvent event) throws IOException {
        openPage("/UserFXML/UserDashboard.fxml", event);
    }

    @FXML
    private void productHandleBtn(MouseEvent event) throws IOException {
        openPage("/UserFXML/userProduct.fxml", event);
    }

    @FXML
    private void aboutHandleBtn(MouseEvent event) throws IOException {
        openPage("/UserFXML/About.fxml", event);
    }

    @FXML
    private void profileHandlebtn(MouseEvent event) throws IOException {
        openPage("/UserFXML/UserProfile.fxml", event);
    }

    @FXML
    private void orderHandleBtn(MouseEvent event) throws IOException {
        openPage("/UserFXML/userOrder.fxml", event);
    }

    @FXML
    private void handleLogoutBtn(MouseEvent event) throws IOException {
        SessionAuditUtil.logoutUserSession();
        openPage("/Main/Login.fxml", event);
    }
}
