package AdminController;

import Table.User;
import config.ImageStorageUtil;
import config.SessionAuditUtil;
import config.config;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
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

    // New columns
    @FXML private TableColumn<User, String> phone, address;

    // FORM
    @FXML private TextField nameField, emailField, usernameField;
    @FXML private TextField phoneField, addressField;
    @FXML private ComboBox<String> roleCombo, statusCombo;
    @FXML private ImageView profileImage;

    private String imagePath = "";
    private int selectedId = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        makeCircle(logo);

        roleCombo.getItems().addAll("Admin", "User", "Cashier");
        statusCombo.getItems().addAll("Approved", "Pending");

        // Table columns mapping
        id.setCellValueFactory(new PropertyValueFactory<>("id"));
        name.setCellValueFactory(new PropertyValueFactory<>("name"));
        email.setCellValueFactory(new PropertyValueFactory<>("email"));
        uname.setCellValueFactory(new PropertyValueFactory<>("uname"));
        role.setCellValueFactory(new PropertyValueFactory<>("role"));
        status.setCellValueFactory(new PropertyValueFactory<>("status"));

        // New mapping
        phone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        address.setCellValueFactory(new PropertyValueFactory<>("address"));

        loadUsers();

        ViewUser.setOnMouseClicked(e -> setForm());
    }

    private void makeCircle(ImageView img) {
        if (img == null) return;
        double w = img.getFitWidth() > 0 ? img.getFitWidth() : 60;
        double h = img.getFitHeight() > 0 ? img.getFitHeight() : 60;
        double r = Math.min(w, h) / 2.0;
        img.setClip(new Circle(w / 2.0, h / 2.0, r));
    }

    // ================= LOAD USERS =================
    private void loadUsers() {
        ObservableList<User> list = FXCollections.observableArrayList();
        String sql = "SELECT u_id,u_name,u_email,u_uname,u_role,u_status,u_image,u_phone,u_address FROM tbl_acc ORDER BY u_id DESC";

        try (Connection conn = config.connectDB();
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
                        rs.getString("u_image"),
                        rs.getString("u_phone"),
                        rs.getString("u_address")
                ));
            }

            ViewUser.setItems(list);

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load users. Check DB columns u_phone/u_address.").showAndWait();
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

        phoneField.setText(u.getPhone());
        addressField.setText(u.getAddress());

        imagePath = u.getImage();
        loadImageToView(imagePath);
    }

    private void loadImageToView(String pathFromDb) {
        try {
            if (pathFromDb == null || pathFromDb.trim().isEmpty()) {
                profileImage.setImage(null);
                return;
            }

            Path p = Paths.get(pathFromDb);
            if (!p.isAbsolute()) {
                p = Paths.get(System.getProperty("user.dir")).resolve(pathFromDb);
            }

            File f = p.toFile();
            if (f.exists()) {
                profileImage.setImage(new Image(f.toURI().toString(), true));
            } else {
                profileImage.setImage(null);
            }

        } catch (Exception e) {
            profileImage.setImage(null);
        }
    }

    // ================= IMAGE UPLOAD =================
    @FXML
    private void chooseImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File file = fc.showOpenDialog(null);
        if (file == null) return;

        try {
            imagePath = ImageStorageUtil.copyProfileImage(file);
            loadImageToView(imagePath);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to copy image!").showAndWait();
        }
    }

    // ================= ADD USER =================
    @FXML
    private void addUser(ActionEvent event) {
        UserFormInput input = readValidatedUserForm(false);
        if (input == null) return;

        String sql = "INSERT INTO tbl_acc(u_name,u_email,u_uname,u_role,u_status,u_image,u_phone,u_address) VALUES(?,?,?,?,?,?,?,?)";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, input.name);
            ps.setString(2, input.email);
            ps.setString(3, input.username);
            ps.setString(4, input.role);
            ps.setString(5, input.status);
            ps.setString(6, input.imagePath);
            ps.setString(7, input.phone);
            ps.setString(8, input.address);

            ps.executeUpdate();
            loadUsers();
            clear();

            new Alert(Alert.AlertType.INFORMATION, "User Added!").showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to add user!").showAndWait();
        }
    }

    // ================= UPDATE USER =================
    @FXML
    private void updateUser(ActionEvent event) {
        if (selectedId == 0) {
            new Alert(Alert.AlertType.WARNING, "Please select a user from the table.").showAndWait();
            return;
        }

        UserFormInput input = readValidatedUserForm(true);
        if (input == null) return;

        String sql = "UPDATE tbl_acc SET u_name=?,u_email=?,u_uname=?,u_role=?,u_status=?,u_image=?,u_phone=?,u_address=? WHERE u_id=?";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, input.name);
            ps.setString(2, input.email);
            ps.setString(3, input.username);
            ps.setString(4, input.role);
            ps.setString(5, input.status);
            ps.setString(6, input.imagePath);
            ps.setString(7, input.phone);
            ps.setString(8, input.address);
            ps.setInt(9, selectedId);

            ps.executeUpdate();
            loadUsers();
            clear();

            new Alert(Alert.AlertType.INFORMATION, "User Updated!").showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update user!").showAndWait();
        }
    }

    // ================= DELETE USER =================
    @FXML
    private void deleteUser(ActionEvent event) {
        if (selectedId == 0) {
            new Alert(Alert.AlertType.WARNING, "Please select a user from the table.").showAndWait();
            return;
        }

        String sql = "DELETE FROM tbl_acc WHERE u_id=?";

        try {
            int deleted = config.deleteRecord(sql, selectedId);
            if (deleted <= 0) {
                new Alert(Alert.AlertType.WARNING, "No user was deleted.").showAndWait();
                return;
            }
            loadUsers();
            clear();
            new Alert(Alert.AlertType.INFORMATION, "User Deleted!").showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to delete user!").showAndWait();
        }
    }

    private void clear() {
        nameField.clear();
        emailField.clear();
        usernameField.clear();
        phoneField.clear();
        addressField.clear();
        roleCombo.setValue(null);
        statusCombo.setValue(null);
        profileImage.setImage(null);
        imagePath = "";
        selectedId = 0;
    }

    private UserFormInput readValidatedUserForm(boolean isUpdate) {
        config con = new config();

        String name = safe(nameField.getText());
        String email = safe(emailField.getText()).toLowerCase(Locale.ENGLISH);
        String username = safe(usernameField.getText());
        String phone = safe(phoneField.getText());
        String address = safe(addressField.getText());
        String role = safe(roleCombo.getValue());
        String status = safe(statusCombo.getValue());

        if (name.isEmpty() || email.isEmpty() || username.isEmpty()
                || phone.isEmpty() || address.isEmpty() || role.isEmpty() || status.isEmpty()) {
            showValidationAlert("All fields except the image are required.");
            return null;
        }

        if (name.length() < 2) {
            showValidationAlert("Full name must be at least 2 characters.");
            return null;
        }

        if (!isValidGmailAddress(email)) {
            showValidationAlert("Email must be a valid @gmail.com address.");
            return null;
        }

        if (!username.matches("^[A-Za-z0-9._-]{3,20}$")) {
            showValidationAlert("Username must be 3-20 characters and use only letters, numbers, dot, underscore, or dash.");
            return null;
        }

        if (!phone.matches("^09\\d{9}$")) {
            showValidationAlert("Phone number must be 11 digits and start with 09.");
            return null;
        }

        if (address.length() < 5) {
            showValidationAlert("Address must be at least 5 characters.");
            return null;
        }

        String emailCheck = isUpdate
                ? "SELECT 1 FROM tbl_acc WHERE LOWER(u_email) = LOWER(?) AND u_id <> ?"
                : "SELECT 1 FROM tbl_acc WHERE LOWER(u_email) = LOWER(?)";
        if (isUpdate ? con.recordExists(emailCheck, email, selectedId) : con.recordExists(emailCheck, email)) {
            showValidationAlert("Email already exists.");
            return null;
        }

        String usernameCheck = isUpdate
                ? "SELECT 1 FROM tbl_acc WHERE LOWER(u_uname) = LOWER(?) AND u_id <> ?"
                : "SELECT 1 FROM tbl_acc WHERE LOWER(u_uname) = LOWER(?)";
        if (isUpdate ? con.recordExists(usernameCheck, username, selectedId) : con.recordExists(usernameCheck, username)) {
            showValidationAlert("Username already exists.");
            return null;
        }

        return new UserFormInput(name, email, username, phone, address, role, status, imagePath);
    }

    private boolean isValidGmailAddress(String email) {
        return email.matches("^[A-Za-z0-9._%+-]+@gmail\\.com$");
    }

    private void showValidationAlert(String message) {
        new Alert(Alert.AlertType.WARNING, message).showAndWait();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    // ================= NAVIGATION =================
    @FXML
    private void dashboardButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/AdminDashboard.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void productButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminProduct.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void inventoryButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminInventory.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void saleHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminSale.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void logsButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminLogs.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void logoutButtonAction(ActionEvent event) throws IOException {
        SessionAuditUtil.logoutAdminSession();
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void userButtonAction(ActionEvent event) {
        // already here
    }

    private static class UserFormInput {
        final String name;
        final String email;
        final String username;
        final String phone;
        final String address;
        final String role;
        final String status;
        final String imagePath;

        UserFormInput(String name, String email, String username, String phone,
                String address, String role, String status, String imagePath) {
            this.name = name;
            this.email = email;
            this.username = username;
            this.phone = phone;
            this.address = address;
            this.role = role;
            this.status = status;
            this.imagePath = imagePath == null ? "" : imagePath.trim();
        }
    }
}
