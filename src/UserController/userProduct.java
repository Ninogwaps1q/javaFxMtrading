package UserController;

import Model.CartItem;
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
import javafx.scene.effect.GaussianBlur;
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

    // ✅ search
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
        setupCartTable();
        loadProductsFromDB();
        loadCartFromDB();
        updateTotal();
        updateCartBadge();
        makeCircle(navLogo);

        // ✅ live search
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldV, newV) -> filterProducts(newV));
        }
    }

    // ✅ circle logo
    private void makeCircle(ImageView imageView) {
        if (imageView == null) return;

        // run later to ensure bounds exist
        imageView.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            double w = imageView.getFitWidth();
            double h = imageView.getFitHeight();

            if (w <= 0) w = newB.getWidth();
            if (h <= 0) h = newB.getHeight();

            double r = Math.min(w, h) / 2.0;
            Circle clip = new Circle(r, r, r);
            imageView.setClip(clip);
        });
    }

    // =========================================================
    // CART TABLE SETUP
    // =========================================================
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
        card.setPrefWidth(190);
        card.setPadding(new Insets(10));
        card.setStyle(
            "-fx-background-color: rgba(255,255,255,0.95);" +
            "-fx-background-radius: 14;" +
            "-fx-border-radius: 14;" +
            "-fx-border-color: rgba(0,0,0,0.06);" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 2);"
        );

        ImageView img = new ImageView();
        img.setFitWidth(250);
        img.setFitHeight(120);
        img.setPreserveRatio(false); // ✅ fill the box
        img.setSmooth(true);
        img.setImage(loadImageSafe(p.getImage()));

        Rectangle clip = new Rectangle(250, 120);
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        img.setClip(clip);

        Label name = new Label(p.getName());
        name.setStyle("-fx-font-weight:bold; -fx-font-size:14;");

        Label price = new Label("₱" + String.format("%.2f", p.getPrice()));
        price.setStyle("-fx-font-size:13; -fx-font-weight:bold;");

        Label stock = new Label("Stock: " + p.getStock());
        stock.setStyle("-fx-text-fill:#555;");

        Button add = new Button("Add to Cart");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setStyle("-fx-background-color:#ff1493; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:10;");
        add.setDisable(p.getStock() <= 0);

        Button details = new Button("View Details");
        details.setMaxWidth(Double.MAX_VALUE);
        details.setStyle("-fx-background-color:#ff1493; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:10;");

        add.setOnAction(e -> addToCart(p.getId(), 1));
        details.setOnAction(e -> openDetailsModal(p));

        card.setOnMouseClicked(e -> openDetailsModal(p));

        HBox btnRow = new HBox(8, add, details);
        btnRow.setAlignment(Pos.CENTER);
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
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.35);");

        BorderPane glass = new BorderPane();
        glass.setMaxWidth(900);
        glass.setMaxHeight(650);
        glass.setPadding(new Insets(20));
        glass.setStyle(
            "-fx-background-color: rgba(255,255,255,0.18);" +
            "-fx-background-radius: 22;" +
            "-fx-border-radius: 22;" +
            "-fx-border-color: rgba(255,255,255,0.35);" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 25, 0, 0, 10);"
        );
        glass.setEffect(new GaussianBlur(0));

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
        title.setStyle("-fx-font-size:24; -fx-font-weight:bold; -fx-text-fill:white;");

        Label type = new Label("Type: " + p.getType());
        type.setStyle("-fx-text-fill: rgba(255,255,255,0.90); -fx-font-size:14;");

        Label price = new Label("₱" + String.format("%.2f", p.getPrice()));
        price.setStyle("-fx-text-fill:white; -fx-font-size:20; -fx-font-weight:bold;");

        Label stock = new Label("Stock: " + p.getStock());
        stock.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size:13;");

        Label descTitle = new Label("Description");
        descTitle.setStyle("-fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14;");

        Label desc = new Label(p.getDesc() == null ? "" : p.getDesc());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: rgba(255,255,255,0.88);");

        ScrollPane descScroll = new ScrollPane(desc);
        descScroll.setFitToWidth(true);
        descScroll.setPrefHeight(180);
        descScroll.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: rgba(255,255,255,0.10);" +
            "-fx-background-radius: 14;" +
            "-fx-border-radius: 14;" +
            "-fx-border-color: rgba(255,255,255,0.20);"
        );

        Label qtyLabel = new Label("Quantity:");
        qtyLabel.setStyle("-fx-text-fill:white; -fx-font-weight:bold;");

        Label qtyValue = new Label("1");
        qtyValue.setStyle("-fx-text-fill:white; -fx-font-size:16; -fx-font-weight:bold;");

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
        add.setStyle("-fx-background-color:#ff1493; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;");
        add.setDisable(p.getStock() <= 0);

        Button buyNow = new Button("Buy Now");
        buyNow.setStyle("-fx-background-color:#22c55e; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;");
        buyNow.setDisable(p.getStock() <= 0);

        Button close = new Button("Close");
        close.setStyle("-fx-background-color: rgba(255,255,255,0.18); -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:12;");

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

        String createOrder = "INSERT INTO tbl_orders(u_id,total,status) VALUES(?,?,?)";
        String addItem = "INSERT INTO tbl_order_items(o_id,p_id,qty,price) VALUES(?,?,?,?)";
        String deductStock = "UPDATE tbl_products SET p_stock = p_stock - ? WHERE p_id=? AND p_stock >= ?";

        try (Connection conn = config.connectDB()) {
            conn.setAutoCommit(false);

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

            double total = p.getPrice() * qty;

            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(createOrder, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setDouble(2, total);
                ps.setString(3, "Pending");
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

            cartMsg.setText("✅ Buy Now order placed! (Pending)");
            loadProductsFromDB();
            loadCartFromDB();
            updateTotal();
            updateCartBadge();
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
        double total = 0;
        for (CartItem i : cartItems) total += i.getSubtotal();
        totalLabel.setText("₱" + String.format("%.2f", total));
    }

    private void updateCartBadge() {
        if (cartBadge != null) {
            cartBadge.setVisible(false);
        }
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

        String createOrder = "INSERT INTO tbl_orders(u_id,total,status) VALUES(?,?,?)";
        String addItem = "INSERT INTO tbl_order_items(o_id,p_id,qty,price) VALUES(?,?,?,?)";
        String deductStock = "UPDATE tbl_products SET p_stock = p_stock - ? WHERE p_id=? AND p_stock >= ?";
        String clearCart = "DELETE FROM tbl_cart_items WHERE c_id = (SELECT c_id FROM tbl_cart WHERE u_id=?)";

        try (Connection conn = config.connectDB()) {
            conn.setAutoCommit(false);

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

            double total = 0;
            for (CartItem it : cartItems) total += it.getSubtotal();

            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(createOrder, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setDouble(2, total);
                ps.setString(3, "Pending");
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

            cartMsg.setText("Order placed! (Pending)");
            loadProductsFromDB();
            loadCartFromDB();
            updateTotal();
            updateCartBadge();

        } catch (Exception e) {
            e.printStackTrace();
            cartMsg.setText("Checkout failed.");
        }
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

    @FXML private void aboutHandleBtn(MouseEvent event) {}

    @FXML private void profileHandlebtn(MouseEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/UserFXML/UserProfile.fxml"));
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

    private void openCart() {
        cartMsg.setText("Cart is on the right side.");
    }
}
