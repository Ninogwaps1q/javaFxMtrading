/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cashierController;

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
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Nino
 */
public class cashierDashboard implements Initializable {

    @FXML
    private AnchorPane AnchorPane;
    @FXML
    private VBox panel;
    @FXML
    private ImageView logo;
    @FXML
    private Label textPanel;
    @FXML
    private Button dashboard;
    @FXML
    private Button productBtn;
    @FXML
    private Button salesBtn;
    @FXML
    private Button logoutBtn;
    @FXML
    private ImageView userprofile;
    @FXML
    private Label nameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label roleLabel;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
         makeCircle(logo);
        makeCircle(userprofile);
    }    

    @FXML
    private void dashboardButtonAction(ActionEvent event) {
    }

    @FXML
    private void productButtonAction(ActionEvent event) {
    }

    @FXML
    private void saleHandleBtn(MouseEvent event) {
    }

    @FXML
    private void orderButtonAction(ActionEvent event) {
    }

    @FXML
    private void logoutButtonAction(ActionEvent event) throws IOException {
       Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 800, 500));
        stage.show();
    }

    private void makeCircle(ImageView imageView) {
        double radius = Math.min(imageView.getFitWidth(), imageView.getFitHeight()) / 2;
        Circle clip = new Circle(radius, radius, radius);
        imageView.setClip(clip);
    }
}
