package UserController;

import Model.CartItem;
import Model.CheckoutPayment;
import config.SessionAuditUtil;
import config.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class userCart {

    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> cImage;
    @FXML private TableColumn<CartItem, String> cName;
    @FXML private TableColumn<CartItem, Double> cPrice;
    @FXML private TableColumn<CartItem, Integer> cQty;
    @FXML private TableColumn<CartItem, Double> cSubtotal;

    @FXML private Label totalLabel;
    @FXML private Label cartMsg;

    @FXML private Label cartBadge;
    @FXML private ImageView navLogo;
    @FXML private HBox navPanel;
    @FXML private Label homeBtn;
    @FXML private Label productBtn;
    @FXML private Label aboutBtn;
    @FXML private Label profileBtn;
    @FXML private Label logoutBtn;

    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private int userId;

    public void initialize() {
        userId = UserSession.getId();
        setupCartTable();
        loadCartFromDB();
        updateTotal();
        updateCartBadge();
        makeCircle(navLogo);
    }

    private void makeCircle(ImageView imageView) {
        if (imageView == null) return;

        imageView.setPreserveRatio(false);
        Runnable apply = () -> {
            double w = imageView.getBoundsInLocal().getWidth();
            double h = imageView.getBoundsInLocal().getHeight();

            if (w <= 0) w = imageView.getFitWidth();
            if (h <= 0) h = imageView.getFitHeight();
            if (w <= 0 || h <= 0) return;

            double r = Math.min(w, h) / 2.0;
            imageView.setClip(new Circle(w / 2.0, h / 2.0, r));
        };

        imageView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) javafx.application.Platform.runLater(apply);
        });
        imageView.boundsInLocalProperty().addListener((obs, oldB, newB) -> apply.run());
    }

    private void setupCartTable() {
        cImage.setCellValueFactory(new PropertyValueFactory<>("image"));
        cImage.setCellFactory(col -> new TableCell<CartItem, String>() {
            private final ImageView img = new ImageView();
            {
                img.setFitWidth(45);
                img.setFitHeight(45);
                img.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                img.setImage(loadImageSafe(path));
                setGraphic(img);
            }
        });

        cName.setCellValueFactory(new PropertyValueFactory<>("name"));
        cPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        cQty.setCellValueFactory(new PropertyValueFactory<>("qty"));
        cQty.setCellFactory(col -> new TableCell<CartItem, Integer>() {
            private final Button minus = new Button("-");
            private final Button plus = new Button("+");
            private final Label qtyLbl = new Label();
            private final HBox box = new HBox(6, minus, qtyLbl, plus);

            {
                box.setAlignment(Pos.CENTER);
                box.getStyleClass().add("qty-box");
                minus.getStyleClass().add("qty-btn");
                plus.getStyleClass().add("qty-btn");
                qtyLbl.getStyleClass().add("qty-value");
                minus.setPrefWidth(28);
                plus.setPrefWidth(28);

                minus.setOnAction(e -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    changeQty(item, -1);
                });

                plus.setOnAction(e -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    changeQty(item, +1);
                });
            }

            @Override
            protected void updateItem(Integer qty, boolean empty) {
                super.updateItem(qty, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                CartItem item = getTableView().getItems().get(getIndex());
                qtyLbl.setText(String.valueOf(item.getQty()));
                minus.setDisable(item.getQty() <= 1);
                plus.setDisable(item.getQty() >= item.getStock());
                setGraphic(box);
            }
        });

        cSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        cartTable.setItems(cartItems);
    }

    private Image loadImageSafe(String pathFromDb) {
        try {
            if (pathFromDb == null || pathFromDb.trim().isEmpty()) return null;

            Path p = Paths.get(pathFromDb);
            if (!p.isAbsolute()) {
                p = Paths.get(System.getProperty("user.dir")).resolve(pathFromDb);
            }

            File f = p.toFile();
            if (!f.exists()) return null;

            return new Image(f.toURI().toString(), true);
        } catch (Exception e) {
            return null;
        }
    }

    private void loadCartFromDB() {
        cartItems.clear();

        String sql =
            "SELECT p.p_id, p.p_name, p.p_price, p.p_stock, p.p_image, ci.qty " +
            "FROM tbl_cart c " +
            "JOIN tbl_cart_items ci ON ci.c_id = c.c_id " +
            "JOIN tbl_products p ON p.p_id = ci.p_id " +
            "WHERE c.u_id = ? " +
            "ORDER BY ci.ci_id DESC";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cartItems.add(new CartItem(
                    rs.getInt("p_id"),
                    rs.getString("p_name"),
                    rs.getDouble("p_price"),
                    rs.getInt("p_stock"),
                    rs.getInt("qty"),
                    rs.getString("p_image")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshCartUI() {
        loadCartFromDB();
        updateTotal();
        updateCartBadge();
    }

    private void updateTotal() {
        double total = 0;
        for (CartItem i : cartItems) total += i.getSubtotal();
        totalLabel.setText("PHP " + String.format("%.2f", total));
    }

    private void updateCartBadge() {
        if (cartBadge == null) return;

        int count = 0;
        for (CartItem i : cartItems) count += i.getQty();
        cartBadge.setText(String.valueOf(count));
        cartBadge.setVisible(count > 0);
    }

    private boolean ensureOrderProfileReady() {
        String message = OrderValidationUtil.getMissingContactMessage(userId);
        if (message == null) {
            return true;
        }

        cartMsg.setText(message);
        OrderValidationUtil.showProfileRequirementAlert(message);
        return false;
    }

    private void changeQty(CartItem item, int delta) {
        cartMsg.setText("");
        int newQty = item.getQty() + delta;

        if (newQty < 1) return;
        if (newQty > item.getStock()) {
            cartMsg.setText("Not enough stock.");
            return;
        }

        String sql =
            "UPDATE tbl_cart_items SET qty=? " +
            "WHERE c_id = (SELECT c_id FROM tbl_cart WHERE u_id=?) AND p_id=?";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, newQty);
            ps.setInt(2, userId);
            ps.setInt(3, item.getProductId());
            ps.executeUpdate();

            refreshCartUI();

        } catch (Exception e) {
            e.printStackTrace();
            cartMsg.setText("Failed to update qty.");
        }
    }

    @FXML
    private void removeSelected() {
        CartItem sel = cartTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        String del =
            "DELETE FROM tbl_cart_items " +
            "WHERE c_id = (SELECT c_id FROM tbl_cart WHERE u_id=?) AND p_id=?";

        try {
            int deleted = config.deleteRecord(del, userId, sel.getProductId());
            if (deleted <= 0) {
                cartMsg.setText("No cart item was removed.");
                return;
            }
            refreshCartUI();
            cartMsg.setText("Item removed from cart.");
        } catch (Exception e) {
            e.printStackTrace();
            cartMsg.setText("Failed to remove item.");
        }
    }

    @FXML
    private void checkout() {
        if (cartItems.isEmpty()) {
            cartMsg.setText("Your cart is empty.");
            return;
        }
        if (!ensureOrderProfileReady()) {
            return;
        }

        double total = 0;
        for (CartItem it : cartItems) total += it.getSubtotal();

        CheckoutPayment payment = PaymentDialogUtil.showPaymentDialog(total);
        if (payment == null) {
            cartMsg.setText("Payment cancelled.");
            return;
        }

        String createOrder = "INSERT INTO tbl_orders(u_id,total,status,payment_method,payment_ref) VALUES(?,?,?,?,?)";
        String addItem = "INSERT INTO tbl_order_items(o_id,p_id,qty,price) VALUES(?,?,?,?)";
        String deductStock = "UPDATE tbl_products SET p_stock = p_stock - ? WHERE p_id=? AND p_stock >= ?";
        String clearCart = "DELETE FROM tbl_cart_items WHERE c_id = (SELECT c_id FROM tbl_cart WHERE u_id=?)";

        try (Connection conn = config.connectDB()) {
            conn.setAutoCommit(false);
            OrderSchemaUtil.ensurePaymentColumns(conn);

            for (CartItem it : cartItems) {
                if (it.getQty() > it.getStock()) {
                    conn.rollback();
                    cartMsg.setText("Stock changed. Please update your cart.");
                    refreshCartUI();
                    return;
                }
            }

            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(createOrder, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setDouble(2, total);
                ps.setString(3, "Pending");
                ps.setString(4, payment.getMethod());
                ps.setString(5, payment.getReference());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                keys.next();
                orderId = keys.getInt(1);
            }

            for (CartItem it : cartItems) {
                try (PreparedStatement ps1 = conn.prepareStatement(addItem)) {
                    ps1.setInt(1, orderId);
                    ps1.setInt(2, it.getProductId());
                    ps1.setInt(3, it.getQty());
                    ps1.setDouble(4, it.getPrice());
                    ps1.executeUpdate();
                }

                try (PreparedStatement ps2 = conn.prepareStatement(deductStock)) {
                    ps2.setInt(1, it.getQty());
                    ps2.setInt(2, it.getProductId());
                    ps2.setInt(3, it.getQty());
                    int updated = ps2.executeUpdate();
                    if (updated == 0) {
                        conn.rollback();
                        cartMsg.setText("Not enough stock. Try again.");
                        refreshCartUI();
                        return;
                    }
                }
            }

            config.deleteRecord(conn, clearCart, userId);

            conn.commit();
            refreshCartUI();
            try {
                openOrderSuccessPage(orderId);
            } catch (IOException io) {
                io.printStackTrace();
                cartMsg.setText("Order placed. Open Orders to view details.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            cartMsg.setText("Checkout failed.");
        }
    }

    @FXML
    private void homeHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/UserDashboard.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void productHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/userProduct.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void aboutHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/About.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void profileHandlebtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/UserProfile.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void orderHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/userOrder.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void handleLogoutBtn(MouseEvent event) throws IOException {
        SessionAuditUtil.logoutUserSession();
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void openCartPage(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/userCart.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    private void openOrderSuccessPage(int orderId) throws IOException {
        OrderSuccessSession.setLastOrderId(orderId);
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/userOrderSuccess.fxml"));
        Stage stage = (Stage) cartTable.getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }
}
