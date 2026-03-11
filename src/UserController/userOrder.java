package UserController;

import Table.UserOrderRow;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class userOrder implements Initializable {

    @FXML private ImageView navLogo;
    @FXML private Label totalOrdersLabel;
    @FXML private Label totalSpendLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label shippedCountLabel;
    @FXML private Label deliveredCountLabel;
    @FXML private Label cancelledCountLabel;
    @FXML private Label ordersMsg;
    @FXML private TableView<UserOrderRow> ordersTable;
    @FXML private TableColumn<UserOrderRow, String> orderIdCol;
    @FXML private TableColumn<UserOrderRow, String> amountCol;
    @FXML private TableColumn<UserOrderRow, String> statusCol;
    @FXML private TableColumn<UserOrderRow, String> dateCol;

    private int userId;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userId = UserSession.getId();
        makeCircle(navLogo);
        setupTable();
        loadOrders();
    }

    private void makeCircle(ImageView imageView) {
        if (imageView == null) return;
        double w = imageView.getFitWidth();
        double h = imageView.getFitHeight();
        double radius = Math.min(w, h) / 2.0;
        imageView.setClip(new Circle(w / 2.0, h / 2.0, radius));
    }

    private void setupTable() {
        orderIdCol.setCellValueFactory(d ->
                new SimpleStringProperty(String.format("#%06d", d.getValue().getOrderId())));
        amountCol.setCellValueFactory(d ->
                new SimpleStringProperty(formatCurrency(d.getValue().getAmount())));
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus()));
        dateCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDate()));

        statusCol.setCellFactory(col -> new TableCell<UserOrderRow, String>() {
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

        ordersTable.setPlaceholder(new Label("No orders found."));
    }

    private void loadOrders() {
        resetSummary();

        if (userId <= 0) {
            ordersMsg.setText("Please login again.");
            ordersTable.setItems(FXCollections.observableArrayList());
            return;
        }

        ObservableList<UserOrderRow> rows = FXCollections.observableArrayList();
        double totalSpend = 0.0;
        int pending = 0;
        int shipped = 0;
        int delivered = 0;
        int cancelled = 0;

        String sql = "SELECT o_id, total, status, created_at "
                + "FROM tbl_orders "
                + "WHERE u_id = ? "
                + "ORDER BY datetime(created_at) DESC, o_id DESC";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    double amount = rs.getDouble("total");

                    rows.add(new UserOrderRow(
                            rs.getInt("o_id"),
                            amount,
                            status,
                            formatDate(rs.getString("created_at"))
                    ));

                    totalSpend += amount;

                    String normalized = normalizeStatus(status);
                    if ("delivered".equals(normalized)) delivered++;
                    else if ("shipped".equals(normalized)) shipped++;
                    else if ("cancelled".equals(normalized)) cancelled++;
                    else pending++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ordersMsg.setText("Failed to load your orders.");
            return;
        }

        ordersTable.setItems(rows);
        totalOrdersLabel.setText(String.format("%,d", rows.size()));
        totalSpendLabel.setText(formatCurrency(totalSpend));
        pendingCountLabel.setText(String.format("%,d", pending));
        shippedCountLabel.setText(String.format("%,d", shipped));
        deliveredCountLabel.setText(String.format("%,d", delivered));
        cancelledCountLabel.setText(String.format("%,d", cancelled));
        ordersMsg.setText(rows.isEmpty() ? "You have no orders yet." : "Your latest order updates are shown below.");
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
            return LocalDateTime.parse(dbDate, in).format(out);
        } catch (Exception e) {
            return dbDate;
        }
    }

    private String statusClass(String status) {
        String normalized = normalizeStatus(status);
        if ("delivered".equals(normalized)) return "status-delivered";
        if ("shipped".equals(normalized)) return "status-shipped";
        if ("cancelled".equals(normalized)) return "status-cancelled";
        return "status-pending";
    }

    private String normalizeStatus(String status) {
        if (status == null) return "pending";
        String s = status.toLowerCase(Locale.ENGLISH);
        if (s.contains("deliver")) return "delivered";
        if (s.contains("ship")) return "shipped";
        if (s.contains("cancel")) return "cancelled";
        return "pending";
    }

    private void resetSummary() {
        totalOrdersLabel.setText("0");
        totalSpendLabel.setText(formatCurrency(0.0));
        pendingCountLabel.setText("0");
        shippedCountLabel.setText("0");
        deliveredCountLabel.setText("0");
        cancelledCountLabel.setText("0");
    }

    @FXML
    private void refreshOrdersAction(ActionEvent event) {
        loadOrders();
        if (ordersTable.getItems() != null && !ordersTable.getItems().isEmpty()) {
            ordersMsg.setText("Order list refreshed.");
        }
    }

    private void openPage(String fxml, MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void homeHandleBtn(MouseEvent event) throws IOException {
        openPage("/UserFXML/UserDashboard.fxml", event);
    }

    @FXML
    private void productHandleBtn(MouseEvent event) throws IOException {
        openPage("/UserFXML/userProduct.fxml", event);
    }

    @FXML
    private void aboutHandleBtn(MouseEvent event) throws IOException {
        openPage("/UserFXML/About.fxml", event);
    }

    @FXML
    private void orderHandleBtn(MouseEvent event) throws IOException {
        openPage("/UserFXML/userOrder.fxml", event);
    }

    @FXML
    private void profileHandlebtn(MouseEvent event) throws IOException {
        openPage("/UserFXML/UserProfile.fxml", event);
    }

    @FXML
    private void handleLogoutBtn(MouseEvent event) throws IOException {
        UserSession.clear();
        openPage("/Main/Login.fxml", event);
    }
}
