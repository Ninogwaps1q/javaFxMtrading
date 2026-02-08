/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AdminController;

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
public class adminSale implements Initializable {

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
    private Button userBtn;
    @FXML
    private Button logoutBtn;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        makeCircle(logo);
    }    

    @FXML
    private void dashboardButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/AdminDashboard.fxml"));
        Scene sc = new Scene(root, 800, 500);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }

    @FXML
    private void productButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminProduct.fxml"));
        Scene sc = new Scene(root, 800, 500);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }

    @FXML
    private void orderButtonAction(ActionEvent event) {
    }

    @FXML
    private void userButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminUser.fxml"));
        Scene sc = new Scene(root, 800, 500);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }

    @FXML
    private void logoutButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Scene sc = new Scene(root, 800, 500);
        sc.getStylesheets().add(getClass().getResource("/css/Main.css").toExternalForm());

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }

    private void makeCircle(ImageView logo) {
       double radius = Math.min(logo.getFitWidth(), logo.getFitHeight()) / 2;
        Circle clip = new Circle(radius, radius, radius);
        logo.setClip(clip);
    }

    @FXML
    private void saleHandlleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminSale.fxml"));
        Scene sc = new Scene(root, 800, 500);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(sc);
        stage.show();
    }
    
}
