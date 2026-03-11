package UserController;

import Model.CartItem;
import Model.CheckoutPayment;
import Model.product;
import config.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class userProduct {

    // products cards
    @FXML private FlowPane productFlow;

    // search
    @FXML private TextField searchField;

    // cart table
    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> cImage;
    @FXML private TableColumn<CartItem, String> cName;
    @FXML private TableColumn<CartItem, Double> cPrice;
    @FXML private TableColumn<CartItem, Integer> cQty;
    @FXML private TableColumn<CartItem, Double> cSubtotal;

    @FXML private Label totalLabel;
    @FXML private Label cartMsg;

    // nav
    @FXML private Label cartBadge;
    @FXML private HBox navPanel;
    @FXML private ImageView navLogo;
    @FXML private Label homeBtn;
    @FXML private Label productBtn;
    @FXML private Label aboutBtn;
    @FXML private Label profileBtn;
    @FXML private Label logoutBtn;

    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private final ObservableList<product> allProducts = FXCollections.observableArrayList();
    private int userId;

    public void initialize() {
        userId = UserSession.getId();
        loadProductsFromDB();
        loadCartFromDB();
        updateCartBadge();
        makeCircle(navLogo);

        // live search
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> filterProducts(newV));
        }
    }

    // circle logo
    // circle logo (perfect circle + safe on load)
    private void makeCircle(ImageView imageView) {
        if (imageView == null) return;

        // Make sure it's square so the circle is perfect
        imageView.setPreserveRatio(false);

        // Apply now + whenever size changes
        Runnable apply = () -> {
            double w = imageView.getBoundsInLocal().getWidth();
            double h = imageView.getBoundsInLocal().getHeight();

            // fallback to fit sizes if bounds are still 0
            if (w <= 0) w = imageView.getFitWidth();
            if (h <= 0) h = imageView.getFitHeight();

            if (w <= 0 || h <= 0) return;

            double r = Math.min(w, h) / 2.0;

            Circle clip = new Circle(w / 2.0, h / 2.0, r);
            imageView.setClip(clip);
        };

        // Apply after the node is rendered
        imageView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                javafx.application.Platform.runLater(apply);
            }
        });

        // Re-apply if size changes
        imageView.boundsInLocalProperty().addListener((obs, oldB, newB) -> apply.run());
    }


    // =========================================================
    // CART TABLE SETUP
    // =========================================================
    private void setupCartTable() {
        if (cartTable == null || cImage == null || cName == null || cPrice == null || cQty == null || cSubtotal == null) {
            return;
        }

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
                if (empty) { setGraphic(null); return; }
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

                if (empty) { setGraphic(null); return; }

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

    // =========================================================
    // PRODUCTS LOAD (CARDS)
    // =========================================================
    private void loadProductsFromDB() {
        productFlow.getChildren().clear();
        allProducts.clear();

        String sql = "SELECT * FROM tbl_products ORDER BY p_id DESC";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                product p = new product(
                        rs.getInt("p_id"),
                        rs.getString("p_name"),
                        rs.getString("p_type"),
                        rs.getString("p_desc"),
                        rs.getInt("p_stock"),
                        rs.getDouble("p_price"),
                        rs.getString("p_image")
                );
                allProducts.add(p);
            }

            renderProducts(allProducts);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderProducts(ObservableList<product> list) {
        productFlow.getChildren().clear();
        for (product p : list) {
            productFlow.getChildren().add(makeProductCard(p));
        }
    }

    private void filterProducts(String keyword) {
        if (keyword == null) keyword = "";
        String k = keyword.trim().toLowerCase();

        if (k.isEmpty()) {
            renderProducts(allProducts);
            return;
        }

        ObservableList<product> filtered = FXCollections.observableArrayList();
        for (product p : allProducts) {
            String name = p.getName() == null ? "" : p.getName().toLowerCase();
            String type = p.getType() == null ? "" : p.getType().toLowerCase();
            String desc = p.getDesc() == null ? "" : p.getDesc().toLowerCase();

            if (name.contains(k) || type.contains(k) || desc.contains(k)) {
                filtered.add(p);
            }
        }

        renderProducts(filtered);
    }

    @FXML
    private void clearSearch() {
        if (searchField != null) searchField.clear();
    }

    private VBox makeProductCard(product p) {
        VBox card = new VBox(8);
        card.setPrefWidth(220);
        card.setPadding(new Insets(12));
        card.getStyleClass().addAll("product-card", "surface");

        ImageView img = new ImageView();
        img.setFitWidth(220);
        img.setFitHeight(140);
        img.setPreserveRatio(false);
        img.setSmooth(true);
        img.setImage(loadImageSafe(p.getImage()));
        img.getStyleClass().add("product-image");

        Rectangle clip = new Rectangle(220, 140);
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        img.setClip(clip);

        Label name = new Label(p.getName());
        name.getStyleClass().add("product-title");

        Label price = new Label("PHP " + String.format("%.2f", p.getPrice()));
        price.getStyleClass().add("product-price");

        Label stock = new Label(p.getStock() > 0 ? ("Stock: " + p.getStock()) : "Out of stock");
        stock.getStyleClass().add("product-meta");
        if (p.getStock() <= 0) stock.getStyleClass().add("is-out");

        Button add = new Button("Add to Cart");
        add.setMaxWidth(Double.MAX_VALUE);
        add.getStyleClass().add("btn-primary");
        add.setDisable(p.getStock() <= 0);

        Button details = new Button("View Details");
        details.setMaxWidth(Double.MAX_VALUE);
        details.getStyleClass().add("btn-secondary");

        add.setOnAction(e -> addToCart(p.getId(), 1));
        details.setOnAction(e -> openDetailsModal(p));

        // Prevent the card click handler from firing when buttons are clicked.
        add.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> e.consume());
        details.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> e.consume());

        card.setOnMouseClicked(e -> openDetailsModal(p));

        HBox btnRow = new HBox(8, add, details);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.getStyleClass().add("product-actions");
        HBox.setHgrow(add, Priority.ALWAYS);
        HBox.setHgrow(details, Priority.ALWAYS);

        card.getChildren().addAll(img, name, price, stock, btnRow);
        return card;
    }

    // =========================================================
    // DETAILS MODAL (GLASS + ANIM + SLIDER + BUY NOW)
    // =========================================================
    private void openDetailsModal(product p) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Product Details");

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-overlay");

        BorderPane glass = new BorderPane();
        glass.setMaxWidth(900);
        glass.setMaxHeight(650);
        glass.setPadding(new Insets(20));
        glass.getStyleClass().addAll("surface", "details-panel");

        ImageView img = new ImageView(loadImageSafe(p.getImage()));
        img.setFitWidth(360);
        img.setFitHeight(360);
        img.setPreserveRatio(true);
        img.setSmooth(true);
        makeRoundedImage(img, 22);

        VBox left = new VBox(img);
        left.setAlignment(Pos.TOP_CENTER);
        left.setPadding(new Insets(10));

        Label title = new Label(p.getName());
        title.getStyleClass().add("details-title");

        Label type = new Label("Type: " + p.getType());
        type.getStyleClass().add("details-meta");

        Label price = new Label("PHP " + String.format("%.2f", p.getPrice()));
        price.getStyleClass().add("details-price");

        Label stock = new Label("Stock: " + p.getStock());
        stock.getStyleClass().add("details-meta");

        Label descTitle = new Label("Description");
        descTitle.getStyleClass().add("details-section-title");

        Label desc = new Label(p.getDesc() == null ? "" : p.getDesc());
        desc.setWrapText(true);
        desc.getStyleClass().add("details-desc-text");

        ScrollPane descScroll = new ScrollPane(desc);
        descScroll.setFitToWidth(true);
        descScroll.setPrefHeight(180);
        descScroll.getStyleClass().add("details-desc");

        Label qtyLabel = new Label("Quantity:");
        qtyLabel.getStyleClass().add("details-section-title");

        Label qtyValue = new Label("1");
        qtyValue.getStyleClass().add("details-qty-value");

        Slider qtySlider = new Slider(1, Math.max(1, p.getStock()), 1);
        qtySlider.setMajorTickUnit(1);
        qtySlider.setMinorTickCount(0);
        qtySlider.setSnapToTicks(true);
        qtySlider.setShowTickMarks(false);
        qtySlider.setShowTickLabels(false);
        qtySlider.setDisable(p.getStock() <= 0);

        qtySlider.valueProperty().addListener((obs, oldV, newV) -> {
            int val = (int) Math.round(newV.doubleValue());
            qtySlider.setValue(val);
            qtyValue.setText(String.valueOf(val));
        });

        HBox qtyTop = new HBox(10, qtyLabel, qtyValue);
        qtyTop.setAlignment(Pos.CENTER_LEFT);

        VBox qtyBox = new VBox(6, qtyTop, qtySlider);

        Button add = new Button("Add to Cart");
        add.getStyleClass().add("btn-primary");
        add.setDisable(p.getStock() <= 0);

        Button buyNow = new Button("Buy Now");
        buyNow.getStyleClass().add("btn-success");
        buyNow.setDisable(p.getStock() <= 0);

        Button close = new Button("Close");
        close.getStyleClass().add("btn-secondary");

        add.setOnAction(e -> {
            int qty = (int) qtySlider.getValue();
            addToCart(p.getId(), qty);
            modal.close();
        });

        buyNow.setOnAction(e -> {
            int qty = (int) qtySlider.getValue();
            boolean ok = buyNowSingleProduct(p, qty);
            if (ok) modal.close();
        });

        close.setOnAction(e -> modal.close());

        HBox actions = new HBox(12, add, buyNow, close);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox right = new VBox(12, title, type, price, stock, descTitle, descScroll, qtyBox, actions);
        right.setPadding(new Insets(10));
        right.setAlignment(Pos.TOP_LEFT);

        glass.setLeft(left);
        glass.setCenter(right);
        overlay.getChildren().add(glass);

        Scene scene = new Scene(overlay, 1000, 800);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/css/user.css").toExternalForm());

        modal.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        modal.setScene(scene);

        glass.setOpacity(0);
        glass.setScaleX(0.92);
        glass.setScaleY(0.92);

        FadeTransition ft = new FadeTransition(Duration.millis(180), glass);
        ft.setFromValue(0);
        ft.setToValue(1);

        ScaleTransition st = new ScaleTransition(Duration.millis(180), glass);
        st.setFromX(0.92);
        st.setFromY(0.92);
        st.setToX(1);
        st.setToY(1);

        ft.play();
        st.play();

        modal.showAndWait();
    }

    private boolean buyNowSingleProduct(product p, int qty) {
        if (qty <= 0) return false;
        if (qty > p.getStock()) {
            cartMsg.setText("Not enough stock.");
            return false;
        }
        if (!ensureOrderProfileReady()) {
            return false;
        }

        double total = p.getPrice() * qty;
        CheckoutPayment payment = PaymentDialogUtil.showPaymentDialog(total);
        if (payment == null) {
            cartMsg.setText("Payment cancelled.");
            return false;
        }

        String createOrder = "INSERT INTO tbl_orders(u_id,total,status,payment_method,payment_ref) VALUES(?,?,?,?,?)";
        String addItem = "INSERT INTO tbl_order_items(o_id,p_id,qty,price) VALUES(?,?,?,?)";
        String deductStock = "UPDATE tbl_products SET p_stock = p_stock - ? WHERE p_id=? AND p_stock >= ?";

        try (Connection conn = config.connectDB()) {
            conn.setAutoCommit(false);
            OrderSchemaUtil.ensurePaymentColumns(conn);

            try (PreparedStatement ps2 = conn.prepareStatement(deductStock)) {
                ps2.setInt(1, qty);
                ps2.setInt(2, p.getId());
                ps2.setInt(3, qty);
                int updated = ps2.executeUpdate();
                if (updated == 0) {
                    conn.rollback();
                    cartMsg.setText("Not enough stock. Try again.");
                    loadProductsFromDB();
                    return false;
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

            try (PreparedStatement ps1 = conn.prepareStatement(addItem)) {
                ps1.setInt(1, orderId);
                ps1.setInt(2, p.getId());
                ps1.setInt(3, qty);
                ps1.setDouble(4, p.getPrice());
                ps1.executeUpdate();
            }

            conn.commit();

            loadProductsFromDB();
            loadCartFromDB();
            updateTotal();
            updateCartBadge();
            try {
                openOrderSuccessPage(orderId);
            } catch (IOException io) {
                io.printStackTrace();
                cartMsg.setText("Order placed. Open Orders to view details.");
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            cartMsg.setText("Buy Now failed.");
            return false;
        }
    }

    private void makeRoundedImage(ImageView img, double radius) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        clip.widthProperty().bind(img.fitWidthProperty());
        clip.heightProperty().bind(img.fitHeightProperty());
        img.setClip(clip);

        img.layoutBoundsProperty().addListener((obs, old, val) -> {
            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.TRANSPARENT);
            WritableImage wi = img.snapshot(sp, null);
            img.setImage(wi);
        });
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

    // =========================================================
    // CART HELPERS
    // =========================================================
    private int ensureCartId(Connection conn) throws SQLException {
        String find = "SELECT c_id FROM tbl_cart WHERE u_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(find)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("c_id");
        }

        String insert = "INSERT INTO tbl_cart(u_id) VALUES(?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        }

        throw new SQLException("Failed to create cart");
    }

    private int getStock(Connection conn, int productId) throws SQLException {
        String sql = "SELECT p_stock FROM tbl_products WHERE p_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("p_stock");
        }
        return 0;
    }

    private int getCurrentQty(Connection conn, int cartId, int productId) throws SQLException {
        String sql = "SELECT qty FROM tbl_cart_items WHERE c_id=? AND p_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cartId);
            ps.setInt(2, productId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("qty");
        }
        return 0;
    }

    private void addToCart(int productId, int addQty) {
        cartMsg.setText("");

        String insertItem = "INSERT INTO tbl_cart_items(c_id,p_id,qty) VALUES(?,?,?)";
        String updateItem = "UPDATE tbl_cart_items SET qty=? WHERE c_id=? AND p_id=?";

        try (Connection conn = config.connectDB()) {
            conn.setAutoCommit(false);

            int cartId = ensureCartId(conn);

            int stock = getStock(conn, productId);
            int currentQty = getCurrentQty(conn, cartId, productId);

            if (currentQty + addQty > stock) {
                conn.rollback();
                cartMsg.setText("Not enough stock for this item.");
                return;
            }

            if (currentQty == 0) {
                try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                    ps.setInt(1, cartId);
                    ps.setInt(2, productId);
                    ps.setInt(3, addQty);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(updateItem)) {
                    ps.setInt(1, currentQty + addQty);
                    ps.setInt(2, cartId);
                    ps.setInt(3, productId);
                    ps.executeUpdate();
                }
            }

            conn.commit();

            loadCartFromDB();
            updateTotal();
            updateCartBadge();
            cartMsg.setText("Added to cart. Click the cart icon to review.");

        } catch (Exception e) {
            e.printStackTrace();
            cartMsg.setText("Failed to add to cart.");
        }
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

            loadCartFromDB();
            updateTotal();
            updateCartBadge();

        } catch (Exception e) {
            e.printStackTrace();
            cartMsg.setText("Failed to update qty.");
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

    private void updateTotal() {
        if (totalLabel == null) return;
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

    // =========================================================
    // CART ACTIONS
    // =========================================================
    @FXML
    private void removeSelected() {
        CartItem sel = cartTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        String del =
            "DELETE FROM tbl_cart_items " +
            "WHERE c_id = (SELECT c_id FROM tbl_cart WHERE u_id=?) AND p_id=?";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(del)) {

            ps.setInt(1, userId);
            ps.setInt(2, sel.getProductId());
            ps.executeUpdate();

            loadCartFromDB();
            updateTotal();
            updateCartBadge();

        } catch (Exception e) {
            e.printStackTrace();
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
                    loadProductsFromDB();
                    loadCartFromDB();
                    updateTotal();
                    updateCartBadge();
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
                        loadProductsFromDB();
                        loadCartFromDB();
                        updateTotal();
                        updateCartBadge();
                        return;
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(clearCart)) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }

            conn.commit();

            loadProductsFromDB();
            loadCartFromDB();
            updateTotal();
            updateCartBadge();
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

    private void openOrderSuccessPage(int orderId) throws IOException {
        OrderSuccessSession.setLastOrderId(orderId);
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/userOrderSuccess.fxml"));
        Stage stage = (Stage) productFlow.getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    // =========================================================
    // NAV
    // =========================================================
    @FXML private void homeHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/UserDashboard.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML private void productHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/userProduct.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void aboutHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/About.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML private void profileHandlebtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/UserProfile.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void orderHandleBtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/userOrder.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML private void handleLogoutBtn(MouseEvent event) throws IOException {
        UserSession.clear();
        Parent root = FXMLLoader.load(getClass().getResource("/Main/Login.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }

    @FXML
    private void openCartPage(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/userCart.fxml"));
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
    }
}
