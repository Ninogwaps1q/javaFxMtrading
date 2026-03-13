package AdminController;

import Model.product;
import Table.InventoryRow;
import config.SessionAuditUtil;
import config.config;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class adminInventory implements Initializable {

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final DateTimeFormatter DB_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter UI_DATE_TIME =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a", Locale.ENGLISH);

    @FXML private AnchorPane AnchorPane;
    @FXML private VBox panel;
    @FXML private ImageView logo;
    @FXML private Label textPanel;
    @FXML private Button dashboard;
    @FXML private Button productBtn;
    @FXML private Button salesBtn;
    @FXML private Button userBtn;
    @FXML private Button logoutBtn;

    @FXML private Label inventoryMetaLabel;
    @FXML private Label totalAdjustedTodayLabel;
    @FXML private Label lowStockCountLabel;
    @FXML private Label outOfStockCountLabel;
    @FXML private Label selectedProductLabel;
    @FXML private Label currentStockLabel;
    @FXML private Label historyMetaLabel;
    @FXML private Label statusMessageLabel;

    @FXML private TextField searchField;
    @FXML private TextField quantityField;
    @FXML private ComboBox<String> adjustmentTypeCombo;
    @FXML private TextArea noteField;

    @FXML private TableView<product> productsTable;
    @FXML private TableColumn<product, Number> productIdCol;
    @FXML private TableColumn<product, String> productNameCol;
    @FXML private TableColumn<product, String> productTypeCol;
    @FXML private TableColumn<product, Number> productStockCol;
    @FXML private TableColumn<product, String> productStatusCol;

    @FXML private TableView<InventoryRow> historyTable;
    @FXML private TableColumn<InventoryRow, String> historyDateCol;
    @FXML private TableColumn<InventoryRow, String> historyProductCol;
    @FXML private TableColumn<InventoryRow, String> historyTypeCol;
    @FXML private TableColumn<InventoryRow, Number> historyQtyCol;
    @FXML private TableColumn<InventoryRow, String> historyStockCol;
    @FXML private TableColumn<InventoryRow, String> historyByCol;
    @FXML private TableColumn<InventoryRow, String> historyNoteCol;

    private final ObservableList<product> allProducts = FXCollections.observableArrayList();
    private Integer selectedProductId = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        makeCircle(logo);
        adjustmentTypeCombo.setItems(FXCollections.observableArrayList("Stock In", "Stock Out"));
        adjustmentTypeCombo.getSelectionModel().selectFirst();

        setupProductsTable();
        setupHistoryTable();
        setupSelection();
        setupSearch();
        ensureInventoryTable();
        loadProducts();
        loadInventoryHistory();
    }

    private void setupProductsTable() {
        productIdCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()));
        productNameCol.setCellValueFactory(d ->
                new SimpleStringProperty(safe(d.getValue().getName())));
        productTypeCol.setCellValueFactory(d ->
                new SimpleStringProperty(safe(d.getValue().getType())));
        productStockCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getStock()));
        productStatusCol.setCellValueFactory(d ->
                new SimpleStringProperty(stockStatus(d.getValue().getStock())));

        productStatusCol.setCellFactory(col -> new TableCell<product, String>() {
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

        productsTable.setPlaceholder(new Label("No products available for stock adjustment."));
    }

    private void setupHistoryTable() {
        historyDateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt()));
        historyProductCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProductName()));
        historyTypeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAdjustmentType()));
        historyQtyCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantity()));
        historyStockCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStockBefore() + " -> " + d.getValue().getStockAfter()));
        historyByCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAdjustedBy()));
        historyNoteCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNote()));

        historyTypeCol.setCellFactory(col -> new TableCell<InventoryRow, String>() {
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
                if (item.toLowerCase(Locale.ENGLISH).contains("out")) {
                    badge.getStyleClass().add("status-low-stock");
                } else {
                    badge.getStyleClass().add("status-in-stock");
                }
                setText(null);
                setGraphic(badge);
            }
        });

        historyTable.setPlaceholder(new Label("No inventory adjustments recorded yet."));
    }

    private void setupSelection() {
        productsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                selectedProductId = null;
                selectedProductLabel.setText("Selected Product: None");
                currentStockLabel.setText("Current Stock: -");
                return;
            }

            selectedProductId = newValue.getId();
            selectedProductLabel.setText("Selected Product: " + safe(newValue.getName()));
            currentStockLabel.setText("Current Stock: " + String.format("%,d", Math.max(0, newValue.getStock())));
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshProductsTable());
    }

    private void ensureInventoryTable() {
        String sql = "CREATE TABLE IF NOT EXISTS tbl_inventory ("
                + "inv_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "p_id INTEGER NOT NULL, "
                + "p_name TEXT NOT NULL, "
                + "adjustment_type TEXT NOT NULL, "
                + "qty INTEGER NOT NULL, "
                + "stock_before INTEGER NOT NULL, "
                + "stock_after INTEGER NOT NULL, "
                + "note TEXT, "
                + "adjusted_by_name TEXT, "
                + "adjusted_by_email TEXT, "
                + "created_at TEXT NOT NULL"
                + ")";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            setStatusMessage("Failed to prepare tbl_inventory.", true);
        }
    }

    private void loadProducts() {
        allProducts.clear();
        String sql = "SELECT * FROM tbl_products ORDER BY LOWER(COALESCE(p_name,'')) ASC, p_id DESC";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql);
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
        } catch (Exception e) {
            e.printStackTrace();
            setStatusMessage("Failed to load products.", true);
        }

        refreshProductsTable();
        updateSummaryCards();
    }

    private void refreshProductsTable() {
        ObservableList<product> filtered = FXCollections.observableArrayList();
        String keyword = safe(searchField.getText()).toLowerCase(Locale.ENGLISH);

        for (product item : allProducts) {
            String name = safe(item.getName()).toLowerCase(Locale.ENGLISH);
            String type = safe(item.getType()).toLowerCase(Locale.ENGLISH);
            String desc = safe(item.getDesc()).toLowerCase(Locale.ENGLISH);

            if (keyword.isEmpty() || name.contains(keyword) || type.contains(keyword) || desc.contains(keyword)) {
                filtered.add(item);
            }
        }

        productsTable.setItems(filtered);
        inventoryMetaLabel.setText(String.format("Showing %,d of %,d products", filtered.size(), allProducts.size()));
        restoreSelection(filtered);
    }

    private void updateSummaryCards() {
        int lowStock = 0;
        int outOfStock = 0;
        for (product item : allProducts) {
            int stock = Math.max(0, item.getStock());
            if (stock <= 0) outOfStock++;
            else if (stock <= LOW_STOCK_THRESHOLD) lowStock++;
        }

        lowStockCountLabel.setText(String.format("%,d", lowStock));
        outOfStockCountLabel.setText(String.format("%,d", outOfStock));
        totalAdjustedTodayLabel.setText(String.format("%,d", queryInt(
                "SELECT COALESCE(SUM(qty), 0) FROM tbl_inventory WHERE date(created_at) = date('now')")));
    }

    private void loadInventoryHistory() {
        ObservableList<InventoryRow> rows = FXCollections.observableArrayList();
        String sql = "SELECT inv_id, p_id, p_name, adjustment_type, qty, stock_before, stock_after, "
                + "COALESCE(note, '') AS note, COALESCE(adjusted_by_name, adjusted_by_email, '-') AS adjusted_by, "
                + "created_at "
                + "FROM tbl_inventory "
                + "ORDER BY datetime(created_at) DESC, inv_id DESC";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new InventoryRow(
                        rs.getInt("inv_id"),
                        rs.getInt("p_id"),
                        rs.getString("p_name"),
                        rs.getString("adjustment_type"),
                        rs.getInt("qty"),
                        rs.getInt("stock_before"),
                        rs.getInt("stock_after"),
                        safeOrDash(rs.getString("note")),
                        safeOrDash(rs.getString("adjusted_by")),
                        formatDateTime(rs.getString("created_at"))
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            setStatusMessage("Failed to load inventory history.", true);
        }

        historyTable.setItems(rows);
        historyMetaLabel.setText("Total Adjustments: " + String.format("%,d", rows.size()));
    }

    @FXML
    private void applyAdjustmentAction(ActionEvent event) {
        product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatusMessage("Select a product first.", true);
            return;
        }

        String type = safe(adjustmentTypeCombo.getValue());
        if (type.isEmpty()) {
            setStatusMessage("Select an adjustment type.", true);
            return;
        }

        Integer qty = parseWholeNumber(quantityField.getText(), "Quantity");
        if (qty == null || qty <= 0) return;

        int currentStock = Math.max(0, selected.getStock());
        int newStock = type.equalsIgnoreCase("Stock Out")
                ? currentStock - qty
                : currentStock + qty;

        if (newStock < 0) {
            setStatusMessage("Stock out cannot go below zero.", true);
            return;
        }

        String updateStockSql = "UPDATE tbl_products SET p_stock=? WHERE p_id=?";
        String insertInventorySql = "INSERT INTO tbl_inventory("
                + "p_id, p_name, adjustment_type, qty, stock_before, stock_after, note, adjusted_by_name, adjusted_by_email, created_at"
                + ") VALUES(?,?,?,?,?,?,?,?,?,datetime('now'))";

        boolean saved = false;
        try (Connection conn = config.connectDB()) {
            if (conn == null) return;

            conn.setAutoCommit(false);

            try {
                try (PreparedStatement updatePs = conn.prepareStatement(updateStockSql);
                     PreparedStatement insertPs = conn.prepareStatement(insertInventorySql)) {

                    updatePs.setInt(1, newStock);
                    updatePs.setInt(2, selected.getId());
                    int updated = updatePs.executeUpdate();
                    if (updated <= 0) {
                        throw new Exception("Product stock update failed.");
                    }

                    insertPs.setInt(1, selected.getId());
                    insertPs.setString(2, safe(selected.getName()));
                    insertPs.setString(3, type);
                    insertPs.setInt(4, qty);
                    insertPs.setInt(5, currentStock);
                    insertPs.setInt(6, newStock);
                    insertPs.setString(7, safe(noteField.getText()));
                    insertPs.setString(8, safe(AdminSession.getName()));
                    insertPs.setString(9, safe(AdminSession.getEmail()));
                    int inserted = insertPs.executeUpdate();
                    if (inserted <= 0) {
                        throw new Exception("Inventory log insert failed.");
                    }
                }

                conn.commit();
                saved = true;
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (Exception rollbackError) {
                    rollbackError.printStackTrace();
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            setStatusMessage("Failed to save stock adjustment.", true);
        }

        if (!saved) return;

        quantityField.clear();
        noteField.clear();
        loadProducts();
        loadInventoryHistory();
        setStatusMessage(type + " saved to tbl_inventory.", false);
    }

    @FXML
    private void clearAdjustmentAction(ActionEvent event) {
        adjustmentTypeCombo.getSelectionModel().selectFirst();
        quantityField.clear();
        noteField.clear();
        productsTable.getSelectionModel().clearSelection();
        selectedProductLabel.setText("Selected Product: None");
        currentStockLabel.setText("Current Stock: -");
        setStatusMessage("Adjustment form cleared.", false);
    }

    @FXML
    private void refreshInventoryAction(ActionEvent event) {
        ensureInventoryTable();
        loadProducts();
        loadInventoryHistory();
        setStatusMessage("Inventory page refreshed.", false);
    }

    private void restoreSelection(ObservableList<product> filtered) {
        if (selectedProductId == null) {
            productsTable.getSelectionModel().clearSelection();
            return;
        }

        for (product item : filtered) {
            if (item.getId() == selectedProductId) {
                productsTable.getSelectionModel().select(item);
                productsTable.scrollTo(item);
                return;
            }
        }

        productsTable.getSelectionModel().clearSelection();
    }

    private Integer parseWholeNumber(String raw, String label) {
        String value = safe(raw);
        if (value.isEmpty()) {
            setStatusMessage(label + " is required.", true);
            return null;
        }

        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                setStatusMessage(label + " must be greater than zero.", true);
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            setStatusMessage(label + " must be a whole number.", true);
            return null;
        }
    }

    private int queryInt(String sql) {
        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String formatDateTime(String dbDate) {
        if (dbDate == null || dbDate.trim().isEmpty()) return "-";

        try {
            return LocalDateTime.parse(dbDate.trim(), DB_DATE_TIME).format(UI_DATE_TIME);
        } catch (Exception e) {
            return dbDate;
        }
    }

    private String stockStatus(int stock) {
        if (stock <= 0) return "Out of Stock";
        if (stock <= LOW_STOCK_THRESHOLD) return "Low Stock";
        return "In Stock";
    }

    private String stockStatusClass(String status) {
        String normalized = safe(status).toLowerCase(Locale.ENGLISH);
        if (normalized.contains("out")) return "status-out-stock";
        if (normalized.contains("low")) return "status-low-stock";
        return "status-in-stock";
    }

    private void setStatusMessage(String message, boolean isError) {
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

    private String safeOrDash(String value) {
        String cleaned = safe(value);
        return cleaned.isEmpty() ? "-" : cleaned;
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
        openScene(event, "/AdminFXML/AdminDashboard.fxml");
    }

    @FXML
    private void productButtonAction(ActionEvent event) throws IOException {
        openScene(event, "/AdminFXML/adminProduct.fxml");
    }

    @FXML
    private void inventoryButtonAction(ActionEvent event) throws IOException {
        openScene(event, "/AdminFXML/adminInventory.fxml");
    }

    @FXML
    private void userButtonAction(ActionEvent event) throws IOException {
        openScene(event, "/AdminFXML/adminUser.fxml");
    }

    @FXML
    private void logsButtonAction(ActionEvent event) throws IOException {
        openScene(event, "/AdminFXML/adminLogs.fxml");
    }

    @FXML
    private void logoutButtonAction(ActionEvent event) throws IOException {
        SessionAuditUtil.logoutAdminSession();
        openScene(event, "/Main/Login.fxml");
    }

    @FXML
    private void saleHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminSale.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1300, 800));
        stage.show();
    }

    private void openScene(ActionEvent event, String resource) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(resource));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        int width = "/Main/Login.fxml".equals(resource) ? 1000 : 1300;
        int height = "/Main/Login.fxml".equals(resource) ? 600 : 800;
        stage.setScene(new Scene(root, width, height));
        stage.show();
    }
}
