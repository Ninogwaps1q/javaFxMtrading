package UserController;

import Table.OrderReviewRow;
import config.OrderStatusUtil;
import config.ReviewDataUtil;
import config.config;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
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
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class userOrderSuccess implements Initializable {

    @FXML private Label successMsg;
    @FXML private Label orderIdValue;
    @FXML private Label orderDateValue;
    @FXML private Label paymentMethodValue;
    @FXML private Label paymentRefValue;
    @FXML private Label voucherCodeValue;
    @FXML private Label discountValue;
    @FXML private Label grossTotalValue;
    @FXML private Label totalValue;
    @FXML private Label statusValue;
    @FXML private Label itemCountValue;
    @FXML private VBox itemsBox;
    @FXML private Label reviewHelpLabel;

    private int userId;
    private int currentOrderId;
    private String currentOrderStatus = OrderStatusUtil.STATUS_PENDING;
    private String entryMessage = "";
    private final List<OrderReviewRow> currentItems = new ArrayList<>();

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

        entryMessage = OrderSuccessSession.getEntryMessage();
        currentOrderId = orderId;
        loadOrder(orderId);
        OrderSuccessSession.clear();
    }

    private void loadOrder(int orderId) {
        String orderSql = "SELECT o_id, total, status, created_at, payment_method, payment_ref, "
                + "COALESCE(gross_total, total) AS gross_total, "
                + "COALESCE(discount_amount, 0) AS discount_amount, "
                + "COALESCE(voucher_code, '') AS voucher_code "
                + "FROM tbl_orders WHERE o_id = ? AND u_id = ?";

        try (Connection conn = config.connectDB()) {

            ReviewDataUtil.ensureReviewTable(conn);
            OrderSchemaUtil.ensurePaymentColumns(conn);

            try (PreparedStatement orderPs = conn.prepareStatement(orderSql)) {
                orderPs.setInt(1, orderId);
                orderPs.setInt(2, userId);

                try (ResultSet rs = orderPs.executeQuery()) {
                    if (!rs.next()) {
                        successMsg.setText("Order not found or access denied.");
                        return;
                    }

                    currentOrderId = rs.getInt("o_id");
                    currentOrderStatus = OrderStatusUtil.normalizeDisplayStatus(rs.getString("status"));

                    orderIdValue.setText(String.format("#%06d", currentOrderId));
                    totalValue.setText(formatCurrency(rs.getDouble("total")));
                    statusValue.setText(currentOrderStatus);
                    orderDateValue.setText(formatDateTime(rs.getString("created_at")));
                    paymentMethodValue.setText(safeText(rs.getString("payment_method")));
                    paymentRefValue.setText(safeText(rs.getString("payment_ref")));
                    voucherCodeValue.setText(safeTextOrDash(rs.getString("voucher_code")));
                    discountValue.setText(formatCurrency(rs.getDouble("discount_amount")));
                    grossTotalValue.setText(formatCurrency(rs.getDouble("gross_total")));

                    String initialMessage = entryMessage == null ? "" : entryMessage.trim();
                    entryMessage = "";

                    if (!initialMessage.isEmpty()) {
                        successMsg.setText(initialMessage);
                    } else if (OrderStatusUtil.isDelivered(currentOrderStatus)) {
                        successMsg.setText("Order delivered. You can now review each product with stars, feedback, and an image.");
                    } else {
                        successMsg.setText("Order details loaded successfully.");
                    }
                    updateReviewHelp();
                }
            }

            loadItems(conn, currentOrderId);

        } catch (Exception e) {
            e.printStackTrace();
            successMsg.setText("Failed to load order details.");
        }
    }

    private void loadItems(Connection conn, int orderId) throws Exception {
        String itemsSql = "SELECT p.p_id, p.p_name, p.p_image, oi.qty, oi.price, "
                + "r.rating, COALESCE(r.review_text, '') AS review_text, "
                + "r.review_image, r.created_at AS reviewed_at "
                + "FROM tbl_order_items oi "
                + "JOIN tbl_products p ON p.p_id = oi.p_id "
                + "LEFT JOIN tbl_review r ON r.o_id = oi.o_id AND r.p_id = oi.p_id AND r.u_id = ? "
                + "WHERE oi.o_id = ? "
                + "ORDER BY oi.oi_id ASC";

        itemsBox.getChildren().clear();
        currentItems.clear();
        int itemCount = 0;

        try (PreparedStatement ps = conn.prepareStatement(itemsSql)) {
            ps.setInt(1, userId);
            ps.setInt(2, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int qty = rs.getInt("qty");
                    itemCount += qty;

                    OrderReviewRow row = new OrderReviewRow(
                            rs.getInt("p_id"),
                            rs.getString("p_name"),
                            rs.getString("p_image"),
                            qty,
                            rs.getDouble("price"),
                            (Integer) rs.getObject("rating"),
                            rs.getString("review_text"),
                            rs.getString("review_image"),
                            rs.getString("reviewed_at")
                    );
                    currentItems.add(row);
                    itemsBox.getChildren().add(buildItemCard(row));
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

    private VBox buildItemCard(OrderReviewRow item) {
        ImageView image = new ImageView(loadImageSafe(item.getProductImage()));
        image.setFitHeight(60);
        image.setFitWidth(60);
        image.setPreserveRatio(true);

        Label nameLabel = new Label(safeText(item.getProductName()));
        nameLabel.getStyleClass().add("order-item-name");

        Label metaLabel = new Label("Qty: " + item.getQuantity() + " | Unit: " + formatCurrency(item.getUnitPrice()));
        metaLabel.getStyleClass().add("order-item-meta");

        VBox infoBox = new VBox(4, nameLabel, metaLabel);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label subtotalLabel = new Label(formatCurrency(item.getUnitPrice() * item.getQuantity()));
        subtotalLabel.getStyleClass().add("order-item-price");

        HBox row = new HBox(12, image, infoBox, subtotalLabel);

        VBox card = new VBox(10);
        card.getStyleClass().add("order-item-card");
        card.getChildren().add(row);

        if (OrderStatusUtil.isDelivered(currentOrderStatus)) {
            card.getChildren().add(buildReviewSection(item));
        }

        return card;
    }

    private VBox buildReviewSection(OrderReviewRow item) {
        VBox reviewBox = new VBox(8);
        reviewBox.getStyleClass().add("review-box");

        Label title = new Label("Product Review");
        title.getStyleClass().add("review-title");
        reviewBox.getChildren().add(title);

        if (item.hasReview()) {
            Label starsLabel = new Label(formatStars(item.getRating()) + "  " + item.getRating() + "/5");
            starsLabel.getStyleClass().add("review-saved-label");
            reviewBox.getChildren().add(starsLabel);

            if (item.hasReviewText()) {
                Label reviewTextLabel = new Label(item.getReviewText());
                reviewTextLabel.setWrapText(true);
                reviewTextLabel.getStyleClass().add("review-copy");
                reviewBox.getChildren().add(reviewTextLabel);
            }

            if (item.getReviewedAt() != null && !item.getReviewedAt().trim().isEmpty()) {
                Label reviewedAtLabel = new Label("Reviewed: " + formatDateTime(item.getReviewedAt()));
                reviewedAtLabel.getStyleClass().add("order-item-meta");
                reviewBox.getChildren().add(reviewedAtLabel);
            }

            if (item.getReviewImage() != null && !item.getReviewImage().trim().isEmpty()) {
                ImageView reviewImage = new ImageView(loadImageSafe(item.getReviewImage()));
                reviewImage.setFitHeight(92);
                reviewImage.setFitWidth(92);
                reviewImage.setPreserveRatio(true);
                reviewImage.getStyleClass().add("image-preview");
                reviewBox.getChildren().add(reviewImage);
            }

            Label savedLabel = new Label("Review submitted.");
            savedLabel.getStyleClass().add("hint-text");
            reviewBox.getChildren().add(savedLabel);
            return reviewBox;
        }

        ComboBox<String> ratingCombo = new ComboBox<>();
        ratingCombo.setPromptText("Choose Star Rating");
        ratingCombo.setPrefWidth(190);
        for (int i = 1; i <= 5; i++) {
            ratingCombo.getItems().add(i + (i == 1 ? " Star " : " Stars ") + formatStars(i));
        }

        TextArea reviewTextArea = new TextArea();
        reviewTextArea.setPromptText("Write your review for this product");
        reviewTextArea.setWrapText(true);
        reviewTextArea.setPrefRowCount(3);
        reviewTextArea.getStyleClass().add("review-input");

        Label imageLabel = new Label("Optional image: none selected");
        imageLabel.getStyleClass().add("order-item-meta");

        final String[] selectedImagePath = new String[] { "" };

        Button chooseImageBtn = new Button("Choose Review Image");
        chooseImageBtn.getStyleClass().add("btn-secondary");
        chooseImageBtn.setOnAction(e -> {
            String path = chooseReviewImage();
            if (path.isEmpty()) return;

            selectedImagePath[0] = path;
            imageLabel.setText("Optional image: " + Paths.get(path).getFileName());
        });

        Button submitReviewBtn = new Button("Submit Review");
        submitReviewBtn.getStyleClass().add("btn-primary");
        submitReviewBtn.setOnAction(e ->
                submitReview(item, ratingCombo.getValue(), reviewTextArea.getText(), selectedImagePath[0]));

        HBox actions = new HBox(8, chooseImageBtn, submitReviewBtn);
        reviewBox.getChildren().addAll(ratingCombo, reviewTextArea, imageLabel, actions);
        return reviewBox;
    }

    private void submitReview(OrderReviewRow item, String ratingValue, String reviewTextValue, String selectedImagePath) {
        if (item == null) return;
        if (!OrderStatusUtil.isDelivered(currentOrderStatus)) {
            successMsg.setText("Reviews are only available after the order is delivered.");
            return;
        }

        int rating = parseRating(ratingValue);
        if (rating <= 0) {
            successMsg.setText("Please choose a star rating first.");
            return;
        }

        String reviewText = normalizeReviewText(reviewTextValue);
        if (reviewText.length() < 3) {
            successMsg.setText("Please write a short review before submitting.");
            return;
        }
        if (reviewText.length() > 500) {
            successMsg.setText("Review text is too long. Please keep it within 500 characters.");
            return;
        }

        String storedImagePath = copyReviewImage(selectedImagePath);
        String imagePath = selectedImagePath == null ? "" : selectedImagePath.trim();
        if (!imagePath.isEmpty() && storedImagePath.isEmpty()) {
            successMsg.setText("Failed to save the selected review image.");
            return;
        }

        String sql = "INSERT INTO tbl_review(o_id, p_id, u_id, rating, review_text, review_image, created_at) "
                + "VALUES(?,?,?,?,?,?,datetime('now'))";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ReviewDataUtil.ensureReviewTable(conn);
            ps.setInt(1, currentOrderId);
            ps.setInt(2, item.getProductId());
            ps.setInt(3, userId);
            ps.setInt(4, rating);
            ps.setString(5, reviewText);
            ps.setString(6, storedImagePath);
            ps.executeUpdate();

            successMsg.setText("Review added for " + safeText(item.getProductName()) + ".");
            loadOrder(currentOrderId);
        } catch (Exception e) {
            e.printStackTrace();
            successMsg.setText("Failed to save product review.");
        }
    }

    private int parseRating(String ratingValue) {
        if (ratingValue == null || ratingValue.trim().isEmpty()) return 0;
        char first = ratingValue.trim().charAt(0);
        return Character.isDigit(first) ? Character.getNumericValue(first) : 0;
    }

    private String chooseReviewImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File file = fc.showOpenDialog(null);
        if (file == null) return "";
        return file.getAbsolutePath();
    }

    private String normalizeReviewText(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ");
    }

    private String copyReviewImage(String sourcePath) {
        String pathValue = sourcePath == null ? "" : sourcePath.trim();
        if (pathValue.isEmpty()) return "";

        try {
            Path source = Paths.get(pathValue);
            if (!source.toFile().exists()) return "";

            File uploadsDir = new File("uploads/reviews");
            if (!uploadsDir.exists()) uploadsDir.mkdirs();

            String fileName = source.getFileName().toString();
            String ext = "";
            int dot = fileName.lastIndexOf(".");
            if (dot >= 0) ext = fileName.substring(dot);

            String newFileName = "review_" + System.currentTimeMillis() + ext;
            Path target = Paths.get(uploadsDir.getAbsolutePath(), newFileName);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            return "uploads/reviews/" + newFileName;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void updateReviewHelp() {
        if (reviewHelpLabel == null) return;
        if (OrderStatusUtil.isDelivered(currentOrderStatus)) {
            reviewHelpLabel.setText("Delivered items can now be reviewed with stars, feedback, and an optional image.");
        } else {
            reviewHelpLabel.setText("Reviews unlock after the order is marked as delivered.");
        }
    }

    private String formatStars(Integer rating) {
        int stars = rating == null ? 0 : Math.max(0, Math.min(5, rating));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            sb.append('\u2605');
        }
        return sb.toString();
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

    private String safeTextOrDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        return value.trim();
    }

    @FXML
    private void exportReceiptAction(ActionEvent event) {
        if (currentOrderId <= 0) {
            successMsg.setText("No order is loaded to export.");
            return;
        }

        try {
            Path exportDir = Paths.get(System.getProperty("user.dir"), "exports", "receipts");
            Files.createDirectories(exportDir);

            String fileName = "receipt_order_" + String.format("%06d", currentOrderId) + ".txt";
            Path target = exportDir.resolve(fileName);
            Files.write(target, buildReceiptText().getBytes());

            successMsg.setText("Receipt exported to " + target.toString());
        } catch (Exception e) {
            e.printStackTrace();
            successMsg.setText("Failed to export receipt.");
        }
    }

    private String buildReceiptText() {
        StringBuilder sb = new StringBuilder();
        sb.append("MELYNAL TRADING").append('\n');
        sb.append("ORDER RECEIPT").append('\n');
        sb.append(line('=', 44)).append('\n');
        sb.append("Order ID : ").append(orderIdValue.getText()).append('\n');
        sb.append("Date     : ").append(orderDateValue.getText()).append('\n');
        sb.append("Status   : ").append(statusValue.getText()).append('\n');
        sb.append("Payment  : ").append(paymentMethodValue.getText()).append('\n');
        sb.append("Reference: ").append(paymentRefValue.getText()).append('\n');
        sb.append("Voucher  : ").append(voucherCodeValue.getText()).append('\n');
        sb.append(line('-', 44)).append('\n');
        sb.append(String.format(Locale.ENGLISH, "%-22s %3s %15s%n", "Item", "Qty", "Subtotal"));
        sb.append(line('-', 44)).append('\n');

        if (currentItems.isEmpty()) {
            sb.append("(No order items)").append('\n');
        } else {
            for (OrderReviewRow item : currentItems) {
                sb.append(fitText(safeText(item.getProductName()), 22))
                        .append(' ')
                        .append(String.format(Locale.ENGLISH, "%3d %15s%n",
                                item.getQuantity(),
                                formatCurrency(item.getQuantity() * item.getUnitPrice())));
            }
        }

        sb.append(line('-', 44)).append('\n');
        sb.append("Gross Total : ").append(grossTotalValue.getText()).append('\n');
        sb.append("Discount    : ").append(discountValue.getText()).append('\n');
        sb.append("Payable     : ").append(totalValue.getText()).append('\n');
        sb.append(line('=', 44)).append('\n');
        return sb.toString();
    }

    private String fitText(String value, int width) {
        String text = value == null ? "-" : value.trim();
        if (text.length() > width) {
            text = text.substring(0, Math.max(0, width - 3)) + "...";
        }
        return String.format("%-" + width + "s", text);
    }

    private String line(char c, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(c);
        }
        return sb.toString();
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
        stage.setScene(new Scene(root, 1300, 800));
        stage.show();
    }
}
