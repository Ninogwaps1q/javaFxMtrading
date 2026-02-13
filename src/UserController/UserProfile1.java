package UserController;

import config.config;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class UserProfile1 implements Initializable {

    @FXML private ImageView profileImage;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;

    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;

    @FXML private TextArea addressField;
    @FXML private TextField phoneField;

    @FXML private PasswordField oldPass;
    @FXML private PasswordField newPass;

    private String imagePath = "";
    private int userId;
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
@Override
    public void initialize(URL url, ResourceBundle rb) {

        userId = UserSession.getId();

        // ✅ Make logo circular
        if (navLogo != null) {
            makeCircle(navLogo);
        }

        // ✅ Make profile image circular
        if (profileImage != null) {
            makeCircle(profileImage);
        }

        if (userId <= 0) {
            System.out.println("No session userId=" + userId);
            return;
        }

        loadUserProfile();
    }
    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    
    private void makeCircle(ImageView imageView) {
        double radius = Math.min(imageView.getFitWidth(), imageView.getFitHeight()) / 2;

        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(
                radius, radius, radius
        );

        imageView.setClip(clip);
    }


    private void loadUserProfile() {
        String sql = "SELECT u_name, u_uname, u_email, u_image, u_address, u_phone FROM tbl_acc WHERE u_id=?";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    fullNameField.setText(rs.getString("u_name"));
                    usernameField.setText(rs.getString("u_uname"));
                    emailField.setText(rs.getString("u_email"));

                    addressField.setText(rs.getString("u_address"));
                    phoneField.setText(rs.getString("u_phone"));

                    nameLabel.setText(rs.getString("u_name"));
                    emailLabel.setText(rs.getString("u_email"));

                    imagePath = rs.getString("u_image");
                    loadProfileImage(imagePath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadProfileImage(String pathFromDb) {
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
                System.out.println("Image not found: " + f.getAbsolutePath());
                profileImage.setImage(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            profileImage.setImage(null);
        }
    }

    @FXML
    private void changePicture(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File file = fc.showOpenDialog(null);
        if (file == null) return;

        try {
            Path uploadsDir = Paths.get(System.getProperty("user.dir"), "uploads");
            if (!Files.exists(uploadsDir)) Files.createDirectories(uploadsDir);

            String ext = "";
            String n = file.getName();
            int dot = n.lastIndexOf(".");
            if (dot >= 0) ext = n.substring(dot);

            String newFileName = "img_" + System.currentTimeMillis() + ext;
            Path targetPath = uploadsDir.resolve(newFileName);

            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // ✅ save relative path
            imagePath = "uploads/" + newFileName;

            profileImage.setImage(new Image(targetPath.toUri().toString(), true));
            updateUserImage(imagePath);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to upload image!").showAndWait();
        }
    }

    private void updateUserImage(String newPath) {
        String sql = "UPDATE tbl_acc SET u_image=? WHERE u_id=?";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPath);
            ps.setInt(2, userId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void updateProfile(ActionEvent event) {
        String sql = "UPDATE tbl_acc SET u_name=?, u_uname=?, u_email=?, u_address=?, u_phone=? WHERE u_id=?";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fullNameField.getText().trim());
            ps.setString(2, usernameField.getText().trim());
            ps.setString(3, emailField.getText().trim());
            ps.setString(4, addressField.getText().trim()); // address
            ps.setString(5, phoneField.getText().trim());   // phone
            ps.setInt(6, userId);

            ps.executeUpdate();

            nameLabel.setText(fullNameField.getText());
            emailLabel.setText(emailField.getText());

            new Alert(Alert.AlertType.INFORMATION, "Profile updated!").showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update profile!").showAndWait();
        }
    }

    @FXML
    private void updatePassword(ActionEvent event) {

        String oldPassword = oldPass.getText().trim();
        String newPassword = newPass.getText().trim();

        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please fill Old Password and New Password.");
            return;
        }

        if (newPassword.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "New password must be at least 6 characters.");
            return;
        }

        String getSql = "SELECT u_password FROM tbl_acc WHERE u_id = ?";
        String updateSql = "UPDATE tbl_acc SET u_password = ? WHERE u_id = ?";

        try (Connection conn = config.connectDB();
             PreparedStatement getPs = conn.prepareStatement(getSql)) {

            getPs.setInt(1, userId);

            try (ResultSet rs = getPs.executeQuery()) {

                if (!rs.next()) {
                    showAlert(Alert.AlertType.ERROR, "User not found!");
                    return;
                }

                String dbHashedPassword = rs.getString("u_password");

                // hash entered old password and compare
                config con = new config();
                String oldHashed = con.hashPassword(oldPassword);

                if (dbHashedPassword == null || !dbHashedPassword.equals(oldHashed)) {
                    showAlert(Alert.AlertType.ERROR, "Old password is incorrect!");
                    return;
                }

                // hash new password and update
                String newHashed = con.hashPassword(newPassword);

                try (PreparedStatement upPs = conn.prepareStatement(updateSql)) {
                    upPs.setString(1, newHashed);
                    upPs.setInt(2, userId);
                    upPs.executeUpdate();
                }

                // clear fields
                oldPass.clear();
                newPass.clear();

                showAlert(Alert.AlertType.INFORMATION, "Password updated successfully!");

            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Failed to update password!");
        }
    }


    @FXML private void homeHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/UserDashboard.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }
    @FXML private void aboutHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root,1000, 600));
        stage.show();
    }
    @FXML private void profileHandlebtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/UserProfile.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }
    @FXML private void handleLogoutBtn(MouseEvent event) throws IOException { UserSession.clear(); 
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void productHandleBtn(MouseEvent event) throws IOException {
         Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/userProduct.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root,1000,600));
        stage.show();
    }
}
