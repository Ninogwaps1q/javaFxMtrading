
package Controller;

import Table.User;
import config.config;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;


public class adminUser implements Initializable {

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
    @FXML
    private TableView<User> ViewUser;

    @FXML
    private TableColumn<User, Integer> id;
    @FXML
    private TableColumn<User, String> name;
    @FXML
    private TableColumn<User, String> email;
    @FXML
    private TableColumn<User, String> uname;
    @FXML
    private TableColumn<User, String> role;
    @FXML
    private TableColumn<User, String> status;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        makeCircle(logo);
        id.setCellValueFactory(new PropertyValueFactory<>("id"));
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        email.setCellValueFactory(new PropertyValueFactory<>("email"));
        uname.setCellValueFactory(new PropertyValueFactory<>("uname"));
        role.setCellValueFactory(new PropertyValueFactory<>("role"));
        status.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        loadUsers();
    }    
    
    
    
    private void loadUsers() {
        ObservableList<User> list = FXCollections.observableArrayList();
        String sql = "SELECT u_id, u_name, u_email, u_uname, u_role, u_status FROM tbl_acc";

        try (Connection conn = new config().connectDB();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int uid = rs.getInt("u_id");
                String unameVal = rs.getString("u_name");
                String emailVal = rs.getString("u_email");
                String username = rs.getString("u_uname");
                String roleVal = rs.getString("u_role");
                String statusVal = rs.getString("u_status");

                list.add(new User(uid, unameVal, emailVal, username, roleVal, statusVal));
            }
        } catch (SQLException ex) {
            System.err.println("Error loading users: " + ex.getMessage());
            ex.printStackTrace();
        }

        ViewUser.setItems(list);
    }
    
    private void makeCircle(ImageView logo) {
        double radius = Math.min(logo.getFitWidth(), logo.getFitHeight()) / 2;
        Circle clip = new Circle(radius, radius, radius);
        logo.setClip(clip);
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
    
}
