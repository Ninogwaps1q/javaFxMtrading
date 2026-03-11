/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AdminController;

import Table.OrderRow;
import config.config;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Pos;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.print.PageLayout;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
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
    @FXML
    private Label totalOrdersMetaLabel;
    @FXML
    private TableView<OrderRow> ordersTable;
    @FXML
    private TableColumn<OrderRow, String> orderIdCol;
    @FXML
    private TableColumn<OrderRow, String> customerCol;
    @FXML
    private TableColumn<OrderRow, String> amountCol;
    @FXML
    private TableColumn<OrderRow, String> statusCol;
    @FXML
    private TableColumn<OrderRow, String> dateCol;
    @FXML
    private ComboBox<String> statusTypeCombo;
    @FXML
    private Button updateStatusBtn;
    @FXML
    private Button printOrderBtn;
    @FXML
    private Label statusMessageLabel;

    private final ObservableList<String> orderStatusOptions =
            FXCollections.observableArrayList("Shipped", "Delivered", "Cancelled");
    private static final DateTimeFormatter DB_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter UI_DATE =
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter PRINT_DATE_TIME =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a", Locale.ENGLISH);

    private Integer selectedOrderId = null;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        makeCircle(logo);
        configureRoleView();
        ensureOrderTrackingColumns();
        setupOrdersTable();
        setupStatusEditor();
        loadOrders();
    }

    private void configureRoleView() {
        if (isCashierRole()) {
            textPanel.setText("Cashier Panel");
            if (productBtn != null) productBtn.setText("Orders");
            if (userBtn != null) {
                userBtn.setVisible(false);
                userBtn.setManaged(false);
            }
            return;
        }

        textPanel.setText("Admin Panel");
        if (productBtn != null) productBtn.setText("Products");
        if (userBtn != null) {
            userBtn.setVisible(true);
            userBtn.setManaged(true);
        }
    }

    private void setupOrdersTable() {
        orderIdCol.setCellValueFactory(d ->
                new SimpleStringProperty(String.format("#%06d", d.getValue().getOrderId())));
        customerCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCustomer()));
        amountCol.setCellValueFactory(d ->
                new SimpleStringProperty(formatCurrency(d.getValue().getAmount())));
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus()));
        dateCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDate()));

        statusCol.setCellFactory(col -> new TableCell<OrderRow, String>() {
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
                badge.getStyleClass().add(statusClass(item));
                setText(null);
                setGraphic(badge);
            }
        });

        ordersTable.setPlaceholder(new Label("No orders found."));
    }

    private void setupStatusEditor() {
        statusTypeCombo.setItems(orderStatusOptions);
        statusTypeCombo.getSelectionModel().selectFirst();
        updateStatusBtn.setDisable(true);
        if (printOrderBtn != null) printOrderBtn.setDisable(true);
        setStatusMessage("Select an order to update status or print details.", false);

        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            if (newRow == null) {
                selectedOrderId = null;
                updateStatusBtn.setDisable(true);
                if (printOrderBtn != null) printOrderBtn.setDisable(true);
                setStatusMessage("Select an order to update status or print details.", false);
                return;
            }

            selectedOrderId = newRow.getOrderId();
            updateStatusBtn.setDisable(false);
            if (printOrderBtn != null) printOrderBtn.setDisable(false);

            String normalized = normalizeOrderStatus(newRow.getStatus());
            if (orderStatusOptions.contains(normalized)) {
                statusTypeCombo.setValue(normalized);
            }

            setStatusMessage(String.format("Selected order #%06d.", selectedOrderId), false);
        });
    }

    private void loadOrders() {
        ObservableList<OrderRow> rows = FXCollections.observableArrayList();
        String sql = "SELECT o.o_id, COALESCE(a.u_name, 'Unknown') AS customer, o.total, o.status, o.created_at "
                + "FROM tbl_orders o "
                + "LEFT JOIN tbl_acc a ON a.u_id = o.u_id "
                + "ORDER BY datetime(o.created_at) DESC, o.o_id DESC";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new OrderRow(
                        rs.getInt("o_id"),
                        rs.getString("customer"),
                        rs.getDouble("total"),
                        rs.getString("status"),
                        formatDate(rs.getString("created_at"))
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ordersTable.setItems(rows);
        totalOrdersMetaLabel.setText("Total Orders: " + String.format("%,d", rows.size()));
        if (selectedOrderId != null) {
            selectOrderById(selectedOrderId);
        }
    }

    @FXML
    private void dashboardButtonAction(ActionEvent event) throws IOException {
        if (isCashierRole()) {
            openScene(event, "/CashierFXML/CashierDashboard.fxml");
            return;
        }
        openScene(event, "/AdminFXML/AdminDashboard.fxml");
    }

    @FXML
    private void productButtonAction(ActionEvent event) throws IOException {
        if (isCashierRole()) {
            openScene(event, "/AdminFXML/adminSale.fxml");
            return;
        }
        openScene(event, "/AdminFXML/adminProduct.fxml");
    }

    @FXML
    private void orderButtonAction(ActionEvent event) throws IOException {
        openScene(event, "/AdminFXML/adminSale.fxml");
    }

    @FXML
    private void userButtonAction(ActionEvent event) throws IOException {
        if (isCashierRole()) {
            setStatusMessage("Cashier account cannot access user management.", true);
            return;
        }
        openScene(event, "/AdminFXML/adminUser.fxml");
    }

    @FXML
    private void logoutButtonAction(ActionEvent event) throws IOException {
        AdminSession.clear();
        openScene(event, "/Main/Login.fxml");
    }

    private void makeCircle(ImageView logo) {
        double w = logo.getFitWidth();
        double h = logo.getFitHeight();
        double radius = Math.min(w, h) / 2.0;

        Circle clip = new Circle(w / 2.0, h / 2.0, radius);
        logo.setClip(clip);
    }

    @FXML
    private void updateOrderStatusAction(ActionEvent event) {
        if (selectedOrderId == null) {
            setStatusMessage("Please select an order first.", true);
            return;
        }

        String newStatus = statusTypeCombo.getValue();
        if (newStatus == null || newStatus.trim().isEmpty()) {
            setStatusMessage("Please choose a status.", true);
            return;
        }

        String sql = "UPDATE tbl_orders SET status=?, handled_by_email=?, handled_by_name=?, "
                + "handled_by_role=?, handled_at=datetime('now') WHERE o_id=?";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus.trim());
            ps.setString(2, sessionEmail());
            ps.setString(3, sessionName());
            ps.setString(4, sessionRole());
            ps.setInt(5, selectedOrderId);
            int updated = ps.executeUpdate();

            if (updated > 0) {
                int orderId = selectedOrderId;
                loadOrders();
                setStatusMessage(String.format("Order #%06d updated to %s.", orderId, newStatus), false);
            } else {
                setStatusMessage("No matching order was updated.", true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            setStatusMessage("Failed to update order status.", true);
        }
    }

    @FXML
    private void printOrderDetailsAction(ActionEvent event) {
        if (selectedOrderId == null) {
            setStatusMessage("Please select an order first.", true);
            return;
        }

        OrderPrintData printData = loadOrderPrintData(selectedOrderId);
        if (printData == null) {
            setStatusMessage("Unable to load order details for printing.", true);
            return;
        }

        String slipText = buildOrderSlipText(printData);
        PrintResult result = printSlip(slipText);

        if (result == PrintResult.SUCCESS) {
            setStatusMessage(String.format("Order #%06d sent to printer.", selectedOrderId), false);
        } else if (result == PrintResult.NO_PRINTER) {
            setStatusMessage("No default printer found. Connect/set a printer first.", true);
        } else {
            setStatusMessage("Printing failed. Check printer connection.", true);
        }
    }

    private void openScene(ActionEvent event, String resource) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(resource));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    private String formatCurrency(double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        String currency = format.format(value);

        if (currency.startsWith("PHP")) {
            return currency.replaceFirst("PHP", "\u20B1");
        }
        if (currency.startsWith("Php")) {
            return currency.replaceFirst("Php", "\u20B1");
        }
        return currency;
    }

    private String formatDate(String dbDate) {
        LocalDateTime date = parseDbDateTime(dbDate);
        if (date == null) return dbDate == null ? "-" : dbDate;
        return date.format(UI_DATE);
    }

    private String formatDateTime(String dbDate) {
        LocalDateTime date = parseDbDateTime(dbDate);
        if (date == null) return dbDate == null ? "-" : dbDate;
        return date.format(PRINT_DATE_TIME);
    }

    private LocalDateTime parseDbDateTime(String dbDate) {
        if (dbDate == null || dbDate.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(dbDate.trim(), DB_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dbDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private String statusClass(String status) {
        String s = status.toLowerCase(Locale.ENGLISH);
        if (s.contains("deliver")) return "status-delivered";
        if (s.contains("ship")) return "status-shipped";
        if (s.contains("cancel")) return "status-cancelled";
        return "status-pending";
    }

    private String normalizeOrderStatus(String status) {
        if (status == null) return "";
        String s = status.trim().toLowerCase(Locale.ENGLISH);
        if (s.contains("ship")) return "Shipped";
        if (s.contains("deliver")) return "Delivered";
        if (s.contains("cancel")) return "Cancelled";
        return status.trim();
    }

    private void selectOrderById(int orderId) {
        for (OrderRow row : ordersTable.getItems()) {
            if (row.getOrderId() == orderId) {
                ordersTable.getSelectionModel().select(row);
                ordersTable.scrollTo(row);
                return;
            }
        }
        ordersTable.getSelectionModel().clearSelection();
    }

    private void setStatusMessage(String message, boolean isError) {
        if (statusMessageLabel == null) return;
        statusMessageLabel.setText(message == null ? "" : message);
        if (isError) {
            statusMessageLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 800;");
        } else {
            statusMessageLabel.setStyle("-fx-text-fill: rgba(15, 23, 42, 0.62); -fx-font-weight: 800;");
        }
    }

    private boolean isCashierRole() {
        String role = AdminSession.getRole();
        return role != null && "Cashier".equalsIgnoreCase(role.trim());
    }

    private String sessionEmail() {
        String email = AdminSession.getEmail();
        return email == null ? "" : email.trim();
    }

    private String sessionName() {
        String name = AdminSession.getName();
        return name == null ? "" : name.trim();
    }

    private String sessionRole() {
        String role = AdminSession.getRole();
        return role == null ? "" : role.trim();
    }

    private void ensureOrderTrackingColumns() {
        try (Connection conn = config.connectDB()) {
            if (conn == null) return;

            addColumnIfMissing(conn, "tbl_orders", "handled_by_email", "TEXT");
            addColumnIfMissing(conn, "tbl_orders", "handled_by_name", "TEXT");
            addColumnIfMissing(conn, "tbl_orders", "handled_by_role", "TEXT");
            addColumnIfMissing(conn, "tbl_orders", "handled_at", "TEXT");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addColumnIfMissing(Connection conn, String table, String column, String type) throws Exception {
        if (columnExists(conn, table, column)) return;

        try (PreparedStatement ps = conn.prepareStatement(
                "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type)) {
            ps.executeUpdate();
        }
    }

    private boolean columnExists(Connection conn, String table, String column) {
        String sql = "PRAGMA table_info(" + table + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private OrderPrintData loadOrderPrintData(int orderId) {
        String orderSql = "SELECT o.o_id, o.total, o.status, o.created_at, "
                + "COALESCE(a.u_name, 'Unknown') AS customer, "
                + "COALESCE(a.u_phone, '-') AS phone, "
                + "COALESCE(a.u_address, '-') AS address "
                + "FROM tbl_orders o "
                + "LEFT JOIN tbl_acc a ON a.u_id = o.u_id "
                + "WHERE o.o_id = ?";

        String itemsSql = "SELECT COALESCE(p.p_name, 'Unknown Item') AS product_name, oi.qty, oi.price "
                + "FROM tbl_order_items oi "
                + "LEFT JOIN tbl_products p ON p.p_id = oi.p_id "
                + "WHERE oi.o_id = ? "
                + "ORDER BY oi.oi_id ASC";

        try (Connection conn = config.connectDB()) {
            if (conn == null) return null;

            OrderPrintData data = null;
            try (PreparedStatement orderPs = conn.prepareStatement(orderSql)) {
                orderPs.setInt(1, orderId);
                try (ResultSet rs = orderPs.executeQuery()) {
                    if (rs.next()) {
                        data = new OrderPrintData();
                        data.orderId = rs.getInt("o_id");
                        data.customerName = safeText(rs.getString("customer"));
                        data.customerPhone = safeText(rs.getString("phone"));
                        data.customerAddress = safeText(rs.getString("address"));
                        data.status = safeText(rs.getString("status"));
                        data.createdAt = safeText(formatDateTime(rs.getString("created_at")));
                        data.total = rs.getDouble("total");
                    }
                }
            }

            if (data == null) return null;

            try (PreparedStatement itemsPs = conn.prepareStatement(itemsSql)) {
                itemsPs.setInt(1, orderId);
                try (ResultSet rs = itemsPs.executeQuery()) {
                    while (rs.next()) {
                        String name = safeText(rs.getString("product_name"));
                        int qty = rs.getInt("qty");
                        double unitPrice = rs.getDouble("price");
                        data.items.add(new OrderPrintItem(name, qty, unitPrice));
                    }
                }
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String buildOrderSlipText(OrderPrintData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("MELYNAL TRADING").append('\n');
        sb.append("ORDER DETAILS").append('\n');
        sb.append(line('=', 42)).append('\n');
        sb.append(String.format(Locale.ENGLISH, "Order ID : #%06d%n", data.orderId));
        sb.append("Date     : ").append(data.createdAt).append('\n');
        sb.append(line('-', 42)).append('\n');
        sb.append("Customer").append('\n');
        sb.append("Name     : ").append(data.customerName).append('\n');
        sb.append("Phone    : ").append(data.customerPhone).append('\n');
        sb.append("Address  : ").append(data.customerAddress).append('\n');
        sb.append(line('-', 42)).append('\n');
        sb.append(String.format(Locale.ENGLISH, "%-20s %3s %14s%n", "Item", "Qty", "Subtotal"));
        sb.append(line('-', 42)).append('\n');

        if (data.items.isEmpty()) {
            sb.append("(No order items)").append('\n');
        } else {
            for (OrderPrintItem item : data.items) {
                sb.append(fitText(item.name, 20))
                        .append(' ')
                        .append(String.format(Locale.ENGLISH, "%3d %14s%n",
                                item.qty, formatCurrency(item.lineTotal())));
            }
        }

        sb.append(line('-', 42)).append('\n');
        sb.append(String.format(Locale.ENGLISH, "Total: %s%n", formatCurrency(data.total)));
        sb.append("Printed: ").append(LocalDateTime.now().format(PRINT_DATE_TIME)).append('\n');
        sb.append(line('=', 42)).append('\n');

        return sb.toString();
    }

    private PrintResult printSlip(String slipText) {
        Printer defaultPrinter = Printer.getDefaultPrinter();
        if (defaultPrinter == null) {
            return PrintResult.NO_PRINTER;
        }

        PrinterJob job = PrinterJob.createPrinterJob(defaultPrinter);
        if (job == null) {
            return PrintResult.FAILED;
        }

        job.getJobSettings().setJobName("Order-" + selectedOrderId);

        Text printText = new Text(slipText);
        printText.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-fill: #111111;");

        VBox paper = new VBox(10);
        paper.setPrefWidth(380);
        paper.setStyle("-fx-background-color: white; -fx-padding: 18;");
        paper.setAlignment(Pos.TOP_LEFT);

        ImageView logoView = createReceiptLogo();
        if (logoView != null) {
            HBox logoBox = new HBox(logoView);
            logoBox.setAlignment(Pos.CENTER);
            paper.getChildren().add(logoBox);
        }
        paper.getChildren().add(printText);

        Group root = new Group(paper);
        new Scene(root);
        root.applyCss();
        root.layout();

        PageLayout page = job.getJobSettings().getPageLayout();
        double printableWidth = page.getPrintableWidth() - 8;
        double nodeWidth = paper.getBoundsInParent().getWidth();
        if (printableWidth > 0 && nodeWidth > printableWidth) {
            double scale = printableWidth / nodeWidth;
            paper.setScaleX(scale);
            paper.setScaleY(scale);
            root.applyCss();
            root.layout();
        }

        boolean printed = job.printPage(root);
        if (!printed) {
            return PrintResult.FAILED;
        }

        boolean ended = job.endJob();
        return ended ? PrintResult.SUCCESS : PrintResult.FAILED;
    }

    private ImageView createReceiptLogo() {
        try {
            java.net.URL logoUrl = getClass().getResource("/image/image6.jpg");
            if (logoUrl == null) return null;

            Image logo = new Image(logoUrl.toExternalForm(), false);
            if (logo.isError()) return null;

            double diameter = 84.0;
            ImageView logoView = new ImageView(logo);
            logoView.setPreserveRatio(false);
            logoView.setFitWidth(diameter);
            logoView.setFitHeight(diameter);
            logoView.setSmooth(true);
            logoView.setClip(new Circle(diameter / 2.0, diameter / 2.0, diameter / 2.0));
            return logoView;
        } catch (Exception e) {
            return null;
        }
    }

    private String safeText(String value) {
        if (value == null) return "-";
        String cleaned = value.replace('\n', ' ').replace('\r', ' ').trim();
        return cleaned.isEmpty() ? "-" : cleaned;
    }

    private String fitText(String value, int width) {
        String text = safeText(value);
        if (text.length() > width) {
            text = text.substring(0, Math.max(0, width - 3)) + "...";
        }
        return String.format("%-" + width + "s", text);
    }

    private String line(char c, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(c);
        return sb.toString();
    }

    private enum PrintResult {
        SUCCESS,
        NO_PRINTER,
        FAILED
    }

    private static class OrderPrintData {
        int orderId;
        String customerName;
        String customerPhone;
        String customerAddress;
        String status;
        String createdAt;
        double total;
        List<OrderPrintItem> items = new ArrayList<>();
    }

    private static class OrderPrintItem {
        final String name;
        final int qty;
        final double unitPrice;

        OrderPrintItem(String name, int qty, double unitPrice) {
            this.name = name;
            this.qty = qty;
            this.unitPrice = unitPrice;
        }

        double lineTotal() {
            return unitPrice * qty;
        }
    }
    
}
