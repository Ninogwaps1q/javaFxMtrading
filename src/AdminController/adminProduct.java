package AdminController;

import Model.product;
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
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class adminProduct implements Initializable {

    private static final int LOW_STOCK_THRESHOLD = 10;

    @FXML private VBox panel;
    @FXML private ImageView logo;
    @FXML private Label textPanel;
    @FXML private Button dashboard;
    @FXML private Button productBtn;
    @FXML private Button salesBtn;
    @FXML private Button userBtn;
    @FXML private Button logoutBtn;

    @FXML private Label inventoryMetaLabel;
    @FXML private Label totalSkuLabel;
    @FXML private Label totalUnitsLabel;
    @FXML private Label lowStockCountLabel;
    @FXML private Label outOfStockCountLabel;
    @FXML private Label selectedProductLabel;
    @FXML private Label statusMessageLabel;

    @FXML private TextField searchField;
    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private TextField adjustQtyField;
    @FXML private TextArea descField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> stockFilterCombo;
    @FXML private ImageView productImage;

    @FXML private TableView<product> table;
    @FXML private TableColumn<product, Number> colId;
    @FXML private TableColumn<product, String> colName;
    @FXML private TableColumn<product, String> colType;
    @FXML private TableColumn<product, Number> colStock;
    @FXML private TableColumn<product, String> colStatus;
    @FXML private TableColumn<product, String> colPrice;

    private final ObservableList<product> allProducts = FXCollections.observableArrayList();
    private String imagePath = "";
    private Integer selectedProductId = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        makeCircle(logo);

        if (typeCombo != null) {
            typeCombo.setItems(FXCollections.observableArrayList(
                    "Skincare", "Body Wash", "Makeup"
            ));
        }

        if (stockFilterCombo != null) {
            stockFilterCombo.setItems(FXCollections.observableArrayList(
                    "All Products", "In Stock", "Low Stock", "Out of Stock"
            ));
            stockFilterCombo.getSelectionModel().selectFirst();
        }

        setupTable();
        setupFilters();
        setupSelection();
        loadProducts();
    }

    private void setupTable() {
        colId.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()));
        colName.setCellValueFactory(d ->
                new SimpleStringProperty(safe(d.getValue().getName())));
        colType.setCellValueFactory(d ->
                new SimpleStringProperty(safe(d.getValue().getType())));
        colStock.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getStock()));
        colPrice.setCellValueFactory(d ->
                new SimpleStringProperty(formatCurrency(d.getValue().getPrice())));

        if (colStatus != null) {
            colStatus.setCellValueFactory(d ->
                    new SimpleStringProperty(stockStatus(d.getValue().getStock())));

            colStatus.setCellFactory(col -> new TableCell<product, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null || item.trim().isEmpty()) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    Label badge = new Label(item);
                    badge.getStyleClass().add("status-badge");
                    badge.getStyleClass().add(stockStatusClass(item));
                    setText(null);
                    setGraphic(badge);
                }
            });
        }

        table.setPlaceholder(new Label("No inventory items found."));
    }

    private void setupFilters() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshInventoryView());
        }
        if (stockFilterCombo != null) {
            stockFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshInventoryView());
        }
    }

    private void setupSelection() {
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                clearSelectionState();
                return;
            }
            populateForm(newValue);
        });
    }

    private void loadProducts() {
        allProducts.clear();

        try (Connection conn = config.connectDB()) {
            if (conn == null) return;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM tbl_products ORDER BY LOWER(COALESCE(p_name,'')) ASC, p_id DESC");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    allProducts.add(new product(
                            rs.getInt("p_id"),
                            rs.getString("p_name"),
                            rs.getString("p_type"),
                            rs.getString("p_desc"),
                            rs.getInt("p_stock"),
                            rs.getDouble("p_price"),
                            rs.getString("p_image")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            setStatusMessage("Failed to load inventory.", true);
        }

        refreshInventoryView();
    }

    private void refreshInventoryView() {
        ObservableList<product> filtered = FXCollections.observableArrayList();
        String keyword = searchField == null ? "" : safe(searchField.getText()).toLowerCase(Locale.ENGLISH);
        String filter = stockFilterCombo == null ? "All Products" : safe(stockFilterCombo.getValue());

        for (product item : allProducts) {
            if (!matchesSearch(item, keyword)) continue;
            if (!matchesStockFilter(item, filter)) continue;
            filtered.add(item);
        }

        table.setItems(filtered);
        if (inventoryMetaLabel != null) {
            inventoryMetaLabel.setText(String.format("Showing %,d of %,d products", filtered.size(), allProducts.size()));
        }
        updateSummaryCards();
        restoreSelection(filtered);
    }

    private boolean matchesSearch(product item, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return true;

        String name = safe(item.getName()).toLowerCase(Locale.ENGLISH);
        String type = safe(item.getType()).toLowerCase(Locale.ENGLISH);
        String desc = safe(item.getDesc()).toLowerCase(Locale.ENGLISH);

        return name.contains(keyword) || type.contains(keyword) || desc.contains(keyword);
    }

    private boolean matchesStockFilter(product item, String filter) {
        if (filter == null || filter.trim().isEmpty() || "All Products".equalsIgnoreCase(filter)) {
            return true;
        }

        int stock = item.getStock();
        if ("Out Of Stock".equalsIgnoreCase(filter) || "Out of Stock".equalsIgnoreCase(filter)) {
            return stock <= 0;
        }
        if ("Low Stock".equalsIgnoreCase(filter)) {
            return stock > 0 && stock <= LOW_STOCK_THRESHOLD;
        }
        if ("In Stock".equalsIgnoreCase(filter)) {
            return stock > LOW_STOCK_THRESHOLD;
        }
        return true;
    }

    private void updateSummaryCards() {
        if (totalSkuLabel == null || totalUnitsLabel == null
                || lowStockCountLabel == null || outOfStockCountLabel == null) {
            return;
        }

        int totalUnits = 0;
        int lowStockCount = 0;
        int outOfStockCount = 0;

        for (product item : allProducts) {
            int stock = Math.max(0, item.getStock());
            totalUnits += stock;

            if (stock <= 0) outOfStockCount++;
            else if (stock <= LOW_STOCK_THRESHOLD) lowStockCount++;
        }

        totalSkuLabel.setText(String.format("%,d", allProducts.size()));
        totalUnitsLabel.setText(String.format("%,d", totalUnits));
        lowStockCountLabel.setText(String.format("%,d", lowStockCount));
        outOfStockCountLabel.setText(String.format("%,d", outOfStockCount));
    }

    private void restoreSelection(ObservableList<product> filtered) {
        if (selectedProductId == null) {
            table.getSelectionModel().clearSelection();
            return;
        }

        for (product item : filtered) {
            if (item.getId() == selectedProductId) {
                table.getSelectionModel().select(item);
                table.scrollTo(item);
                return;
            }
        }

        table.getSelectionModel().clearSelection();
    }

    private void populateForm(product item) {
        selectedProductId = item.getId();
        nameField.setText(safe(item.getName()));
        priceField.setText(String.format(Locale.ENGLISH, "%.2f", item.getPrice()));
        stockField.setText(String.valueOf(item.getStock()));
        descField.setText(safe(item.getDesc()));

        String type = safe(item.getType());
        if (!type.isEmpty() && !typeCombo.getItems().contains(type)) {
            typeCombo.getItems().add(type);
        }
        typeCombo.setValue(type);

        imagePath = safe(item.getImage());
        loadImageToView(imagePath);
        if (selectedProductLabel != null) {
            selectedProductLabel.setText(String.format("Selected Product: %s (Current Stock: %,d)",
                    safe(item.getName()), Math.max(0, item.getStock())));
        }
    }

    private void loadImageToView(String pathFromDb) {
        try {
            if (pathFromDb == null || pathFromDb.trim().isEmpty()) {
                productImage.setImage(null);
                return;
            }

            Path path = Paths.get(pathFromDb);
            if (!path.isAbsolute()) {
                path = Paths.get(System.getProperty("user.dir")).resolve(pathFromDb);
            }

            File file = path.toFile();
            if (!file.exists()) {
                productImage.setImage(null);
                return;
            }

            productImage.setImage(new Image(file.toURI().toString(), true));
        } catch (Exception e) {
            productImage.setImage(null);
        }
    }

    @FXML
    private void chooseImage(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File file = fc.showOpenDialog(null);
        if (file == null) return;

        try {
            imagePath = ImageStorageUtil.copyProductImage(file);
            loadImageToView(imagePath);
        } catch (Exception e) {
            e.printStackTrace();
            setStatusMessage("Failed to copy product image.", true);
        }
    }

    @FXML
    private void addProduct(ActionEvent event) {
        ProductFormInput input = readProductForm();
        if (input == null) return;

        String sql = "INSERT INTO tbl_products(p_name,p_type,p_desc,p_stock,p_price,p_image) VALUES(?,?,?,?,?,?)";

        try (Connection conn = config.connectDB()) {
            if (conn == null) return;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, input.name);
                ps.setString(2, input.type);
                ps.setString(3, input.description);
                ps.setInt(4, input.stock);
                ps.setDouble(5, input.price);
                ps.setString(6, imagePath);
                ps.executeUpdate();
            }

            loadProducts();
            clearFormState();
            setStatusMessage("Product added to inventory.", false);
        } catch (Exception e) {
            e.printStackTrace();
            setStatusMessage("Failed to add product.", true);
        }
    }

    @FXML
    private void updateProduct(ActionEvent event) {
        product selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatusMessage("Select a product before updating.", true);
            return;
        }

        ProductFormInput input = readProductForm();
        if (input == null) return;

        String sql = "UPDATE tbl_products SET p_name=?, p_type=?, p_desc=?, p_stock=?, p_price=?, p_image=? WHERE p_id=?";

        try (Connection conn = config.connectDB()) {
            if (conn == null) return;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, input.name);
                ps.setString(2, input.type);
                ps.setString(3, input.description);
                ps.setInt(4, input.stock);
                ps.setDouble(5, input.price);
                ps.setString(6, imagePath);
                ps.setInt(7, selected.getId());
                ps.executeUpdate();
            }

            selectedProductId = selected.getId();
            loadProducts();
            setStatusMessage("Inventory item updated.", false);
        } catch (Exception e) {
            e.printStackTrace();
            setStatusMessage("Failed to update product.", true);
        }
    }

    @FXML
    private void deleteProduct(ActionEvent event) {
        product selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatusMessage("Select a product before deleting.", true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Product");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete " + safe(selected.getName()) + " from inventory?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (!result.isPresent() || result.get() != ButtonType.OK) return;

        try {
            int deleted = config.deleteRecord("DELETE FROM tbl_products WHERE p_id=?", selected.getId());
            if (deleted <= 0) {
                setStatusMessage("No product was deleted.", true);
                return;
            }
            loadProducts();
            clearFormState();
            setStatusMessage("Product deleted from inventory.", false);
        } catch (Exception e) {
            e.printStackTrace();
            setStatusMessage("Failed to delete product.", true);
        }
    }

    @FXML
    private void clearFormAction(ActionEvent event) {
        clearFormState();
        setStatusMessage("Inventory form cleared.", false);
    }

    @FXML
    private void refreshInventoryAction(ActionEvent event) {
        loadProducts();
        setStatusMessage("Inventory refreshed.", false);
    }

    @FXML
    private void restockAction(ActionEvent event) {
        adjustStock(true);
    }

    @FXML
    private void deductStockAction(ActionEvent event) {
        adjustStock(false);
    }

    private void adjustStock(boolean increase) {
        product selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatusMessage("Select a product before adjusting stock.", true);
            return;
        }

        int qty = parseWholeNumber(adjustQtyField.getText(), "Adjustment quantity");
        if (qty <= 0) return;

        int currentStock = Math.max(0, selected.getStock());
        int newStock = increase ? currentStock + qty : currentStock - qty;
        if (newStock < 0) {
            setStatusMessage("Cannot deduct more than the current stock.", true);
            return;
        }

        try (Connection conn = config.connectDB()) {
            if (conn == null) return;

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE tbl_products SET p_stock=? WHERE p_id=?")) {
                ps.setInt(1, newStock);
                ps.setInt(2, selected.getId());
                ps.executeUpdate();
            }

            selectedProductId = selected.getId();
            loadProducts();
            adjustQtyField.clear();
            setStatusMessage((increase ? "Stock added to " : "Stock deducted from ")
                    + safe(selected.getName()) + ".", false);
        } catch (Exception e) {
            e.printStackTrace();
            setStatusMessage("Failed to adjust stock.", true);
        }
    }

    private ProductFormInput readProductForm() {
        String name = safe(nameField.getText());
        String type = safe(typeCombo.getValue());
        String description = safe(descField.getText());

        if (name.isEmpty()) {
            setStatusMessage("Product name is required.", true);
            return null;
        }
        if (type.isEmpty()) {
            setStatusMessage("Product type is required.", true);
            return null;
        }

        Integer stock = parseWholeNumberNullable(stockField.getText(), "Stock");
        if (stock == null) return null;

        Double price = parseAmount(priceField.getText(), "Price");
        if (price == null) return null;

        return new ProductFormInput(name, type, description, stock, price);
    }

    private Integer parseWholeNumberNullable(String raw, String label) {
        String value = safe(raw);
        if (value.isEmpty()) {
            setStatusMessage(label + " is required.", true);
            return null;
        }

        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                setStatusMessage(label + " cannot be negative.", true);
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            setStatusMessage(label + " must be a whole number.", true);
            return null;
        }
    }

    private int parseWholeNumber(String raw, String label) {
        Integer parsed = parseWholeNumberNullable(raw, label);
        return parsed == null ? -1 : parsed;
    }

    private Double parseAmount(String raw, String label) {
        String value = safe(raw);
        if (value.isEmpty()) {
            setStatusMessage(label + " is required.", true);
            return null;
        }

        try {
            double parsed = Double.parseDouble(value);
            if (parsed < 0) {
                setStatusMessage(label + " cannot be negative.", true);
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            setStatusMessage(label + " must be a valid number.", true);
            return null;
        }
    }

    private void clearFormState() {
        table.getSelectionModel().clearSelection();
        clearFormFields();
        clearSelectionState();
    }

    private void clearFormFields() {
        nameField.clear();
        priceField.clear();
        stockField.clear();
        descField.clear();
        typeCombo.setValue(null);
        productImage.setImage(null);
        imagePath = "";
    }

    private void clearSelectionState() {
        selectedProductId = null;
        if (selectedProductLabel != null) {
            selectedProductLabel.setText("Selected Product: None");
        }
        if (adjustQtyField != null) {
            adjustQtyField.clear();
        }
        if (table.getSelectionModel().getSelectedItem() == null) {
            clearFormFields();
        }
    }

    private String stockStatus(int stock) {
        if (stock <= 0) return "Out of Stock";
        if (stock <= LOW_STOCK_THRESHOLD) return "Low Stock";
        return "In Stock";
    }

    private String stockStatusClass(String status) {
        if (status == null) return "status-pending";
        String normalized = status.toLowerCase(Locale.ENGLISH);
        if (normalized.contains("out")) return "status-out-stock";
        if (normalized.contains("low")) return "status-low-stock";
        return "status-in-stock";
    }

    private String formatCurrency(double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        String currency = format.format(value);
        if (currency.startsWith("PHP")) return currency.replaceFirst("PHP", "\u20B1");
        if (currency.startsWith("Php")) return currency.replaceFirst("Php", "\u20B1");
        return currency;
    }

    private void setStatusMessage(String message, boolean isError) {
        if (statusMessageLabel == null) return;

        statusMessageLabel.setText(safe(message));
        if (isError) {
            statusMessageLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 800;");
        } else {
            statusMessageLabel.setStyle("-fx-text-fill: #0f766e; -fx-font-weight: 800;");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void makeCircle(ImageView img) {
        if (img == null) return;

        double w = img.getFitWidth();
        double h = img.getFitHeight();
        double radius = Math.min(w, h) / 2.0;
        img.setClip(new Circle(w / 2.0, h / 2.0, radius));
    }

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
    private void userButtonAction(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminUser.fxml"));
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
    private void saleHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminSale.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    private static class ProductFormInput {
        final String name;
        final String type;
        final String description;
        final int stock;
        final double price;

        ProductFormInput(String name, String type, String description, int stock, double price) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.stock = stock;
            this.price = price;
        }
    }
}
