package AdminController;

import Model.product;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class adminProduct implements Initializable {

    @FXML private VBox panel;
    @FXML private ImageView logo;
    @FXML private Label textPanel;
    @FXML private Button dashboard;
    @FXML private Button productBtn;
    @FXML private Button salesBtn;
    @FXML private Button userBtn;
    @FXML private Button logoutBtn;

    // ================= PRODUCT FORM =================
    @FXML private TextField nameField, priceField, stockField;
    @FXML private TextArea descField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ImageView productImage;

    // ================= TABLE =================
    @FXML private TableView<product> table;
    @FXML private TableColumn<product, Integer> colId;
    @FXML private TableColumn<product, String> colName, colType;
    @FXML private TableColumn<product, Integer> colStock;
    @FXML private TableColumn<product, Double> colPrice;

    private Connection conn;
    private String imagePath = "";

    // =================================================
    // INITIALIZE
    // =================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        makeCircle(logo);

        conn = config.connectDB();

        typeCombo.setItems(FXCollections.observableArrayList(
                "Skincare", "Body Wash", "Makeup"
        ));

        // table mapping
        colId.setCellValueFactory(d ->
                new javafx.beans.property.SimpleIntegerProperty(d.getValue().getId()).asObject());
        colName.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getName()));
        colType.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getType()));
        colStock.setCellValueFactory(d ->
                new javafx.beans.property.SimpleIntegerProperty(d.getValue().getStock()).asObject());
        colPrice.setCellValueFactory(d ->
                new javafx.beans.property.SimpleDoubleProperty(d.getValue().getPrice()).asObject());

        loadProducts();

        // click → autofill
        table.setOnMouseClicked(e -> {
            product p = table.getSelectionModel().getSelectedItem();
            if (p != null) {
                nameField.setText(p.getName());
                priceField.setText(String.valueOf(p.getPrice()));
                stockField.setText(String.valueOf(p.getStock()));
                descField.setText(p.getDesc());
                typeCombo.setValue(p.getType());

                if (p.getImage() != null && !p.getImage().isEmpty()) {
                    productImage.setImage(new Image("file:" + p.getImage()));
                    imagePath = p.getImage();
                }
            }
        });
    }

    // =================================================
    // LOAD
    // =================================================
    private void loadProducts() {
        ObservableList<product> list = FXCollections.observableArrayList();

        try {
            String sql = "SELECT * FROM tbl_products";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new product(
                        rs.getInt("p_id"),
                        rs.getString("p_name"),
                        rs.getString("p_type"),
                        rs.getString("p_desc"),
                        rs.getInt("p_stock"),
                        rs.getDouble("p_price"),
                        rs.getString("p_image")
                ));
            }

            table.setItems(list);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =================================================
    // IMAGE
    // =================================================
    @FXML
    private void chooseImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(null);

        if (file != null) {
            imagePath = file.getAbsolutePath();
            productImage.setImage(new Image(file.toURI().toString()));
        }
    }

    // =================================================
    // ADD
    // =================================================
    @FXML
    private void addProduct(ActionEvent event) {
        try {
            String sql = "INSERT INTO tbl_products(p_name,p_type,p_desc,p_stock,p_price,p_image) VALUES(?,?,?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, nameField.getText());
            ps.setString(2, typeCombo.getValue());
            ps.setString(3, descField.getText());
            ps.setInt(4, Integer.parseInt(stockField.getText()));
            ps.setDouble(5, Double.parseDouble(priceField.getText()));
            ps.setString(6, imagePath);

            ps.executeUpdate();

            loadProducts();
            clear();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =================================================
    // UPDATE
    // =================================================
    @FXML
    private void updateProduct(ActionEvent event) {
        product p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;

        try {
            String sql = "UPDATE tbl_products SET p_name=?, p_type=?, p_desc=?, p_stock=?, p_price=?, p_image=? WHERE p_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, nameField.getText());
            ps.setString(2, typeCombo.getValue());
            ps.setString(3, descField.getText());
            ps.setInt(4, Integer.parseInt(stockField.getText()));
            ps.setDouble(5, Double.parseDouble(priceField.getText()));
            ps.setString(6, imagePath);
            ps.setInt(7, p.getId());

            ps.executeUpdate();

            loadProducts();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =================================================
    // DELETE
    // =================================================
    @FXML
    private void deleteProduct(ActionEvent event) {
        product p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;

        try {
            String sql = "DELETE FROM tbl_products WHERE p_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, p.getId());
            ps.executeUpdate();

            loadProducts();
            clear();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =================================================
    // CLEAR
    // =================================================
    private void clear() {
        nameField.clear();
        priceField.clear();
        stockField.clear();
        descField.clear();
        typeCombo.setValue(null);
        productImage.setImage(null);
        imagePath = "";
    }

    // =================================================
    // SIDEBAR NAVIGATION
    // =================================================
    private void makeCircle(ImageView img) {
        double radius = Math.min(img.getFitWidth(), img.getFitHeight()) / 2;
        Circle clip = new Circle(radius, radius, radius);
        img.setClip(clip);
    }

    @FXML
    private void dashboardButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/AdminDashboard.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 800, 500));
        stage.show();
    }

    @FXML
    private void productButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminProduct.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 800, 500));
        stage.show();
    }

    @FXML
    private void userButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminUser.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 800, 500));
        stage.show();
    }

    @FXML
    private void logoutButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 800, 500));
        stage.show();
    }

    @FXML
    private void saleHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminSale.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 800, 500));
        stage.show();
    }
}
