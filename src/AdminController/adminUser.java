package AdminController;

import Table.User;
import config.config;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class adminUser implements Initializable {

    @FXML private VBox panel;
    @FXML private ImageView logo;

    @FXML private Button dashboard, productBtn, salesBtn, userBtn, logoutBtn;

    @FXML private TableView<User> ViewUser;
    @FXML private TableColumn<User, Integer> id;
    @FXML private TableColumn<User, String> name, email, uname, role, status;

    // FORM
    @FXML private TextField nameField, emailField, usernameField;
    @FXML private ComboBox<String> roleCombo, statusCombo;
    @FXML private ImageView profileImage;

    private String imagePath = "";
    private int selectedId = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        makeCircle(logo);

        roleCombo.getItems().addAll("Admin", "User", "Cashier");
        statusCombo.getItems().addAll("Approved", "Pending");

        id.setCellValueFactory(new PropertyValueFactory<>("id"));
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        email.setCellValueFactory(new PropertyValueFactory<>("email"));
        uname.setCellValueFactory(new PropertyValueFactory<>("uname"));
        role.setCellValueFactory(new PropertyValueFactory<>("role"));
        status.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadUsers();

        ViewUser.setOnMouseClicked(e -> setForm());
    }

    private void makeCircle(ImageView img) {
        double radius = Math.min(img.getFitWidth(), img.getFitHeight()) / 2;
        img.setClip(new Circle(radius, radius, radius));
    }

    // ================= LOAD =================
    private void loadUsers() {
        ObservableList<User> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM tbl_acc";

        try (Connection conn = new config().connectDB();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new User(
                        rs.getInt("u_id"),
                        rs.getString("u_name"),
                        rs.getString("u_email"),
                        rs.getString("u_uname"),
                        rs.getString("u_role"),
                        rs.getString("u_status"),
                        rs.getString("u_image")
                ));
            }

            ViewUser.setItems(list);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= TABLE CLICK =================
    private void setForm() {
        User u = ViewUser.getSelectionModel().getSelectedItem();
        if (u == null) return;

        selectedId = u.getId();
        nameField.setText(u.getName());
        emailField.setText(u.getEmail());
        usernameField.setText(u.getUname());
        roleCombo.setValue(u.getRole());
        statusCombo.setValue(u.getStatus());

        imagePath = u.getImage();

        if (imagePath != null && !imagePath.isEmpty()) {
            profileImage.setImage(new Image("file:" + imagePath));
        }
    }

    // ================= IMAGE =================
    @FXML
    private void chooseImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fc.showOpenDialog(null);
        if (file != null) {
            imagePath = file.getAbsolutePath();
            profileImage.setImage(new Image("file:" + imagePath));
        }
    }

    // ================= ADD =================
    @FXML
    private void addUser(ActionEvent event) {

        String sql = "INSERT INTO tbl_acc(u_name,u_email,u_uname,u_role,u_status,u_image) VALUES(?,?,?,?,?,?)";

        try (Connection conn = new config().connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nameField.getText());
            ps.setString(2, emailField.getText());
            ps.setString(3, usernameField.getText());
            ps.setString(4, roleCombo.getValue());
            ps.setString(5, statusCombo.getValue());
            ps.setString(6, imagePath);

            ps.executeUpdate();
            loadUsers();
            clear();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= UPDATE =================
    @FXML
    private void updateUser(ActionEvent event) {

        String sql = "UPDATE tbl_acc SET u_name=?,u_email=?,u_uname=?,u_role=?,u_status=?,u_image=? WHERE u_id=?";

        try (Connection conn = new config().connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nameField.getText());
            ps.setString(2, emailField.getText());
            ps.setString(3, usernameField.getText());
            ps.setString(4, roleCombo.getValue());
            ps.setString(5, statusCombo.getValue());
            ps.setString(6, imagePath);
            ps.setInt(7, selectedId);

            ps.executeUpdate();
            loadUsers();
            clear();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= DELETE =================
    @FXML
    private void deleteUser(ActionEvent event) {

        String sql = "DELETE FROM tbl_acc WHERE u_id=?";

        try (Connection conn = new config().connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, selectedId);
            ps.executeUpdate();
            loadUsers();
            clear();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clear() {
        nameField.clear();
        emailField.clear();
        usernameField.clear();
        roleCombo.setValue(null);
        statusCombo.setValue(null);
        profileImage.setImage(null);
        imagePath = "";
        selectedId = 0;
    }

    // ================= NAVIGATION =================
    @FXML
    private void dashboardButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/AdminDashboard.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 800, 500));
    }

    @FXML
    private void productButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminProduct.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 800, 500));
    }

    @FXML
    private void saleHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminSale.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 800, 500));
    }

    @FXML
    private void logoutButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 800, 500));
    }

    @FXML
    private void userButtonAction(ActionEvent event) {
    }
}
