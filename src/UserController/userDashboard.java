/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package UserController;

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

/**
 * FXML Controller class
 *
 * @author Nino
 */
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
        // TODO
    }    

    @FXML
    private void homeHandleBtn(MouseEvent event) {
    }

    @FXML
    private void aboutHandleBtn(MouseEvent event) {
    }

    @FXML
    private void profileHandlebtn(MouseEvent event) {
    }

    @FXML
    private void handleLogoutBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 800, 500));
        stage.show();
    }
    
}
