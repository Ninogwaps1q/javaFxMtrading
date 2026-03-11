package cashierController;

import AdminController.AdminSession;
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
import java.util.Locale;
import java.util.ResourceBundle;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class cashierDashboard implements Initializable {

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
    private Button logoutBtn;
    @FXML
    private Label deliveredOrdersLabel;
    @FXML
    private Label productsSoldLabel;
    @FXML
    private Label totalRevenueLabel;
    @FXML
    private Label handledOrdersLabel;
    @FXML
    private TableView<OrderRow> recentOrdersTable;
    @FXML
    private TableColumn<OrderRow, String> recentOrderIdCol;
    @FXML
    private TableColumn<OrderRow, String> recentCustomerCol;
    @FXML
    private TableColumn<OrderRow, String> recentAmountCol;
    @FXML
    private TableColumn<OrderRow, String> recentStatusCol;
    @FXML
    private TableColumn<OrderRow, String> recentDateCol;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        makeCircle(logo);
        setupRecentOrdersTable();
        ensureOrderTrackingColumns();
        loadSummaryCards();
        loadRecentOrders();
    }

    private void setupRecentOrdersTable() {
        recentOrderIdCol.setCellValueFactory(d ->
                new SimpleStringProperty(String.format("#%06d", d.getValue().getOrderId())));
        recentCustomerCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCustomer()));
        recentAmountCol.setCellValueFactory(d ->
                new SimpleStringProperty(formatCurrency(d.getValue().getAmount())));
        recentStatusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus()));
        recentDateCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDate()));

        recentStatusCol.setCellFactory(col -> new TableCell<OrderRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || item.trim().isEmpty()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(item);
                badge.getStyleClass().add("status-badge");
                badge.getStyleClass().add(statusClass(item));
                setGraphic(badge);
                setText(null);
            }
        });

        recentOrdersTable.setPlaceholder(new Label("No orders handled yet."));
    }

    private void loadSummaryCards() {
        String cashierEmail = sessionEmail();

        if (cashierEmail.isEmpty()) {
            deliveredOrdersLabel.setText("0");
            productsSoldLabel.setText("0");
            handledOrdersLabel.setText("0");
            totalRevenueLabel.setText(formatCurrency(0.0));
            return;
        }

        int deliveredOrders = queryInt(
                "SELECT COUNT(*) FROM tbl_orders "
                + "WHERE LOWER(COALESCE(status,'')) LIKE '%deliver%' "
                + "AND LOWER(COALESCE(handled_by_email,'')) = LOWER(?)",
                cashierEmail
        );

        int handledOrders = queryInt(
                "SELECT COUNT(*) FROM tbl_orders "
                + "WHERE LOWER(COALESCE(handled_by_email,'')) = LOWER(?)",
                cashierEmail
        );

        int productsSold = queryInt(
                "SELECT COALESCE(SUM(oi.qty), 0) "
                + "FROM tbl_order_items oi "
                + "JOIN tbl_orders o ON o.o_id = oi.o_id "
                + "WHERE LOWER(COALESCE(o.status,'')) LIKE '%deliver%' "
                + "AND LOWER(COALESCE(o.handled_by_email,'')) = LOWER(?)",
                cashierEmail
        );

        double totalRevenue = queryDouble(
                "SELECT COALESCE(SUM(total), 0) FROM tbl_orders "
                + "WHERE LOWER(COALESCE(status,'')) LIKE '%deliver%' "
                + "AND LOWER(COALESCE(handled_by_email,'')) = LOWER(?)",
                cashierEmail
        );

        deliveredOrdersLabel.setText(String.format("%,d", deliveredOrders));
        productsSoldLabel.setText(String.format("%,d", productsSold));
        handledOrdersLabel.setText(String.format("%,d", handledOrders));
        totalRevenueLabel.setText(formatCurrency(totalRevenue));
    }

    private void loadRecentOrders() {
        ObservableList<OrderRow> rows = FXCollections.observableArrayList();
        String cashierEmail = sessionEmail();
        if (cashierEmail.isEmpty()) {
            recentOrdersTable.setItems(rows);
            return;
        }

        String sql = "SELECT o.o_id, COALESCE(a.u_name, 'Unknown') AS customer, o.total, o.status, "
                + "COALESCE(o.handled_at, o.created_at) AS display_date "
                + "FROM tbl_orders o "
                + "LEFT JOIN tbl_acc a ON a.u_id = o.u_id "
                + "WHERE LOWER(COALESCE(o.handled_by_email,'')) = LOWER(?) "
                + "ORDER BY datetime(COALESCE(o.handled_at, o.created_at)) DESC, o.o_id DESC "
                + "LIMIT 5";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cashierEmail);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new OrderRow(
                            rs.getInt("o_id"),
                            rs.getString("customer"),
                            rs.getDouble("total"),
                            rs.getString("status"),
                            formatDate(rs.getString("display_date"))
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        recentOrdersTable.setItems(rows);
    }

    private int queryInt(String sql, String value) {
        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double queryDouble(String sql, String value) {
        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
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

    private String sessionEmail() {
        String email = AdminSession.getEmail();
        return email == null ? "" : email.trim();
    }

    private void makeCircle(ImageView imageView) {
        double w = imageView.getFitWidth();
        double h = imageView.getFitHeight();
        double radius = Math.min(w, h) / 2.0;
        imageView.setClip(new Circle(w / 2.0, h / 2.0, radius));
    }

    private String formatCurrency(double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        String currency = format.format(value);

        if (currency.startsWith("PHP")) return currency.replaceFirst("PHP", "\u20B1");
        if (currency.startsWith("Php")) return currency.replaceFirst("Php", "\u20B1");
        return currency;
    }

    private String formatDate(String dbDate) {
        if (dbDate == null || dbDate.trim().isEmpty()) return "-";

        DateTimeFormatter in = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter out = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);

        try {
            return LocalDateTime.parse(dbDate.trim(), in).format(out);
        } catch (Exception e) {
            return dbDate;
        }
    }

    private String statusClass(String status) {
        String s = status.toLowerCase(Locale.ENGLISH);
        if (s.contains("deliver")) return "status-delivered";
        if (s.contains("ship")) return "status-shipped";
        if (s.contains("cancel")) return "status-cancelled";
        return "status-pending";
    }

    @FXML
    private void dashboardButtonAction(ActionEvent event) throws IOException {
        openScene((Node) event.getSource(), "/CashierFXML/CashierDashboard.fxml");
    }

    @FXML
    private void productButtonAction(ActionEvent event) throws IOException {
        openScene((Node) event.getSource(), "/CashierFXML/CashierOrders.fxml");
    }

    @FXML
    private void viewAllOrdersAction(ActionEvent event) throws IOException {
        openScene((Node) event.getSource(), "/CashierFXML/CashierOrders.fxml");
    }

    @FXML
    private void logoutButtonAction(ActionEvent event) throws IOException {
        AdminSession.clear();
        openScene((Node) event.getSource(), "/Main/Login.fxml");
    }

    private void openScene(Node source, String resource) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(resource));
        Stage stage = (Stage) source.getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }
}
