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
import java.util.Locale;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.ResourceBundle;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class AdminDashboard implements Initializable {

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
    private Button userBtn;
    @FXML
    private Button logoutBtn;
    @FXML
    private Button salesBtn;
    @FXML
    private Label totalProductsLabel;
    @FXML
    private Label totalOrdersLabel;
    @FXML
    private Label totalCustomersLabel;
    @FXML
    private Label totalRevenueLabel;
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

        recentOrdersTable.setPlaceholder(new Label("No orders yet."));
    }

    private void makeCircle(ImageView imageView) {
        double w = imageView.getFitWidth();
        double h = imageView.getFitHeight();
        double radius = Math.min(w, h) / 2.0;

        Circle clip = new Circle(w / 2.0, h / 2.0, radius);
        imageView.setClip(clip);
    }

    private void loadSummaryCards() {
        int totalProducts = queryInt("SELECT COUNT(*) FROM tbl_products");
        int totalOrders = queryInt("SELECT COUNT(*) FROM tbl_orders");
        int totalCustomers = queryInt("SELECT COUNT(*) FROM tbl_acc WHERE LOWER(u_role) = 'user'");
        double totalRevenue = queryDouble("SELECT COALESCE(SUM(total), 0) FROM tbl_orders");

        totalProductsLabel.setText(String.format("%,d", totalProducts));
        totalOrdersLabel.setText(String.format("%,d", totalOrders));
        totalCustomersLabel.setText(String.format("%,d", totalCustomers));
        totalRevenueLabel.setText(formatCurrency(totalRevenue));
    }

    private void loadRecentOrders() {
        ObservableList<OrderRow> rows = FXCollections.observableArrayList();
        String sql = "SELECT o.o_id, COALESCE(a.u_name, 'Unknown') AS customer, o.total, o.status, o.created_at "
                + "FROM tbl_orders o "
                + "LEFT JOIN tbl_acc a ON a.u_id = o.u_id "
                + "ORDER BY datetime(o.created_at) DESC, o.o_id DESC "
                + "LIMIT 5";

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

        recentOrdersTable.setItems(rows);
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

    private double queryDouble(String sql) {
        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
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
        if (dbDate == null || dbDate.trim().isEmpty()) return "-";

        DateTimeFormatter in = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter out = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);

        try {
            return LocalDateTime.parse(dbDate, in).format(out);
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
    private void orderButtonAction(ActionEvent event) throws IOException {
        openScene(event, "/AdminFXML/adminSale.fxml");
    }

    @FXML
    private void userButtonAction(ActionEvent event) throws IOException {
        openScene(event, "/AdminFXML/adminUser.fxml");
    }

    @FXML
    private void logoutButtonAction(ActionEvent event) throws IOException {
        AdminSession.clear();

        openScene(event, "/Main/Login.fxml");
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
    private void viewAllOrdersAction(ActionEvent event) throws IOException {
        openScene(event, "/AdminFXML/adminSale.fxml");
    }

    @FXML
    private void saleHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/AdminFXML/adminSale.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    private void openScene(ActionEvent event, String resource) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(resource));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }
}
