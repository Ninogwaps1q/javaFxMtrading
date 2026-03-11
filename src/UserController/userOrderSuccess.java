package UserController;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class userOrderSuccess implements Initializable {

    @FXML private Label successMsg;
    @FXML private Label orderIdValue;
    @FXML private Label orderDateValue;
    @FXML private Label paymentMethodValue;
    @FXML private Label paymentRefValue;
    @FXML private Label totalValue;
    @FXML private Label statusValue;
    @FXML private Label itemCountValue;
    @FXML private VBox itemsBox;

    private int userId;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        userId = UserSession.getId();

        if (userId <= 0) {
            successMsg.setText("Please login again to view your order details.");
            return;
        }

        int orderId = OrderSuccessSession.getLastOrderId();
        if (orderId <= 0) {
            successMsg.setText("No recent order was found.");
            return;
        }

        loadOrder(orderId);
        OrderSuccessSession.clear();
    }

    private void loadOrder(int orderId) {
        String orderSql = "SELECT o_id, total, status, created_at, payment_method, payment_ref "
                + "FROM tbl_orders WHERE o_id = ? AND u_id = ?";

        try (Connection conn = config.connectDB();
             PreparedStatement orderPs = conn.prepareStatement(orderSql)) {

            orderPs.setInt(1, orderId);
            orderPs.setInt(2, userId);

            try (ResultSet rs = orderPs.executeQuery()) {
                if (!rs.next()) {
                    successMsg.setText("Order not found or access denied.");
                    return;
                }

                orderIdValue.setText(String.format("#%06d", rs.getInt("o_id")));
                totalValue.setText(formatCurrency(rs.getDouble("total")));
                statusValue.setText(safeText(rs.getString("status")));
                orderDateValue.setText(formatDateTime(rs.getString("created_at")));
                paymentMethodValue.setText(safeText(rs.getString("payment_method")));
                paymentRefValue.setText(safeText(rs.getString("payment_ref")));
                successMsg.setText("Payment validated. Your order is now pending fulfillment.");
            }

            loadItems(conn, orderId);

        } catch (Exception e) {
            e.printStackTrace();
            successMsg.setText("Failed to load order details.");
        }
    }

    private void loadItems(Connection conn, int orderId) throws Exception {
        String itemsSql = "SELECT p.p_name, p.p_image, oi.qty, oi.price "
                + "FROM tbl_order_items oi "
                + "JOIN tbl_products p ON p.p_id = oi.p_id "
                + "WHERE oi.o_id = ? "
                + "ORDER BY oi.oi_id ASC";

        itemsBox.getChildren().clear();
        int itemCount = 0;

        try (PreparedStatement ps = conn.prepareStatement(itemsSql)) {
            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("p_name");
                    String imagePath = rs.getString("p_image");
                    int qty = rs.getInt("qty");
                    double price = rs.getDouble("price");
                    itemCount += qty;

                    itemsBox.getChildren().add(buildItemCard(name, imagePath, qty, price));
                }
            }
        }

        if (itemsBox.getChildren().isEmpty()) {
            Label empty = new Label("No order items found.");
            empty.getStyleClass().add("hint-text");
            itemsBox.getChildren().add(empty);
        }

        itemCountValue.setText(String.valueOf(itemCount));
    }

    private HBox buildItemCard(String name, String imagePath, int qty, double unitPrice) {
        ImageView image = new ImageView(loadImageSafe(imagePath));
        image.setFitHeight(60);
        image.setFitWidth(60);
        image.setPreserveRatio(true);

        Label nameLabel = new Label(safeText(name));
        nameLabel.getStyleClass().add("order-item-name");

        Label metaLabel = new Label("Qty: " + qty + " | Unit: " + formatCurrency(unitPrice));
        metaLabel.getStyleClass().add("order-item-meta");

        VBox infoBox = new VBox(4, nameLabel, metaLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label subtotalLabel = new Label(formatCurrency(unitPrice * qty));
        subtotalLabel.getStyleClass().add("order-item-price");

        HBox row = new HBox(12, image, infoBox, subtotalLabel);
        row.getStyleClass().add("order-item-card");
        return row;
    }

    private Image loadImageSafe(String pathFromDb) {
        try {
            if (pathFromDb == null || pathFromDb.trim().isEmpty()) {
                return null;
            }

            Path path = Paths.get(pathFromDb);
            if (!path.isAbsolute()) {
                path = Paths.get(System.getProperty("user.dir")).resolve(pathFromDb);
            }

            File file = path.toFile();
            if (!file.exists()) {
                return null;
            }

            return new Image(file.toURI().toString(), true);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        String currency = format.format(amount);
        if (currency.startsWith("PHP")) return currency.replaceFirst("PHP", "\u20B1");
        if (currency.startsWith("Php")) return currency.replaceFirst("Php", "\u20B1");
        return currency;
    }

    private String formatDateTime(String dbDate) {
        if (dbDate == null || dbDate.trim().isEmpty()) {
            return "-";
        }

        DateTimeFormatter input = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter output = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a", Locale.ENGLISH);

        try {
            return LocalDateTime.parse(dbDate, input).format(output);
        } catch (Exception e) {
            return dbDate;
        }
    }

    private String safeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        return value;
    }

    @FXML
    private void goOrdersAction(ActionEvent event) throws IOException {
        openPage("/UserFXML/userOrder.fxml", event);
    }

    @FXML
    private void continueShoppingAction(ActionEvent event) throws IOException {
        openPage("/UserFXML/userProduct.fxml", event);
    }

    private void openPage(String fxml, ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }
}
