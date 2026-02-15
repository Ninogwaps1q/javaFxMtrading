package Controller;

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
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;


public class LandingDashboard implements Initializable {

    @FXML
    private VBox hero;
    private ImageView heroLogo;
    @FXML
    private ImageView logo;
    @FXML
    private Button conBtn;

  
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        makeCircle(logo);
    }    

    private void makeCircle(ImageView logo) {
        double w = logo.getFitWidth();
        double h = logo.getFitHeight();
        double radius = Math.min(w, h) / 2.0;

        Circle clip = new Circle(w / 2.0, h / 2.0, radius);
        logo.setClip(clip);
    }

    @FXML
    private void continueHandleBtn(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Scene sc = new Scene(root, 1000, 600);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }
    
}
