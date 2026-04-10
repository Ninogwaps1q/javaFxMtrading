package AdminController;

import Table.AdminReviewRow;
import Table.LowStockRow;
import Table.OrderRow;
import Table.TopProductRow;
import Table.VoucherRow;
import config.OrderStatusUtil;
import config.ReviewDataUtil;
import config.SessionAuditUtil;
import config.VoucherDataUtil;
import config.config;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDate;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class AdminDashboard implements Initializable {

    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final DateTimeFormatter DB_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter UI_DATE =
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter UI_DATE_TIME =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a", Locale.ENGLISH);

    @FXML private AnchorPane AnchorPane;
    @FXML private VBox panel;
    @FXML private ImageView logo;
    @FXML private Label textPanel;
    @FXML private Button dashboard;
    @FXML private Button productBtn;
    @FXML private Button userBtn;
    @FXML private Button logoutBtn;
    @FXML private Button salesBtn;

    @FXML private Label totalProductsLabel;
    @FXML private Label totalOrdersLabel;
    @FXML private Label totalCustomersLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label todayRevenueLabel;
    @FXML private Label weekRevenueLabel;
    @FXML private Label monthRevenueLabel;
    @FXML private Label lowStockAlertLabel;
    @FXML private Label dashboardMessageLabel;

    @FXML private TableView<OrderRow> recentOrdersTable;
    @FXML private TableColumn<OrderRow, String> recentOrderIdCol;
    @FXML private TableColumn<OrderRow, String> recentCustomerCol;
    @FXML private TableColumn<OrderRow, String> recentAmountCol;
    @FXML private TableColumn<OrderRow, String> recentStatusCol;
    @FXML private TableColumn<OrderRow, String> recentDateCol;

    @FXML private TableView<TopProductRow> topProductsTable;
    @FXML private TableColumn<TopProductRow, String> topProductNameCol;
    @FXML private TableColumn<TopProductRow, String> topUnitsSoldCol;
    @FXML private TableColumn<TopProductRow, String> topRevenueCol;

    @FXML private TableView<LowStockRow> lowStockTable;
    @FXML private TableColumn<LowStockRow, String> lowStockProductCol;
    @FXML private TableColumn<LowStockRow, String> lowStockTypeCol;
    @FXML private TableColumn<LowStockRow, String> lowStockQtyCol;

    @FXML private TableView<AdminReviewRow> reviewsTable;
    @FXML private TableColumn<AdminReviewRow, String> reviewCustomerCol;
    @FXML private TableColumn<AdminReviewRow, String> reviewProductCol;
    @FXML private TableColumn<AdminReviewRow, String> reviewRatingCol;
    @FXML private TableColumn<AdminReviewRow, String> reviewCommentCol;
    @FXML private TableColumn<AdminReviewRow, String> reviewDateCol;

    @FXML private TextField voucherCodeField;
    @FXML private ComboBox<String> voucherTypeCombo;
    @FXML private TextField voucherValueField;
    @FXML private TextField voucherMinOrderField;
    @FXML private DatePicker voucherExpiryPicker;
    @FXML private ComboBox<String> voucherActiveCombo;

    @FXML private TableView<VoucherRow> vouchersTable;
    @FXML private TableColumn<VoucherRow, String> voucherCodeCol;
    @FXML private TableColumn<VoucherRow, String> voucherTypeCol;
    @FXML private TableColumn<VoucherRow, String> voucherCreatedByCol;
    @FXML private TableColumn<VoucherRow, String> voucherValueCol;
    @FXML private TableColumn<VoucherRow, String> voucherMinOrderCol;
    @FXML private TableColumn<VoucherRow, String> voucherExpiryCol;
    @FXML private TableColumn<VoucherRow, String> voucherStatusCol;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        makeCircle(logo);
        configureVoucherControls();
        setupRecentOrdersTable();
        setupTopProductsTable();
        setupLowStockTable();
        setupReviewTable();
        setupVoucherTable();
        ReviewDataUtil.ensureReviewTable();
        VoucherDataUtil.ensureVoucherTable();
        refreshAllData();
        setDashboardMessage("Dashboard insights loaded.", false);
    }

    private void configureVoucherControls() {
        if (voucherTypeCombo != null) {
            voucherTypeCombo.setItems(FXCollections.observableArrayList(
                    VoucherDataUtil.TYPE_PERCENT,
                    VoucherDataUtil.TYPE_FIXED
            ));
            voucherTypeCombo.getSelectionModel().selectFirst();
        }

        if (voucherActiveCombo != null) {
            voucherActiveCombo.setItems(FXCollections.observableArrayList("Active", "Inactive"));
            voucherActiveCombo.getSelectionModel().selectFirst();
        }
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

        recentStatusCol.setCellFactory(col -> buildStatusCell());
        recentOrdersTable.setPlaceholder(new Label("No orders yet."));
    }

    private void setupTopProductsTable() {
        topProductNameCol.setCellValueFactory(d ->
                new SimpleStringProperty(safeText(d.getValue().getProductName())));
        topUnitsSoldCol.setCellValueFactory(d ->
                new SimpleStringProperty(String.format("%,d", d.getValue().getUnitsSold())));
        topRevenueCol.setCellValueFactory(d ->
                new SimpleStringProperty(formatCurrency(d.getValue().getRevenue())));
        topProductsTable.setPlaceholder(new Label("No delivered sales yet."));
    }

    private void setupLowStockTable() {
        lowStockProductCol.setCellValueFactory(d ->
                new SimpleStringProperty(safeText(d.getValue().getProductName())));
        lowStockTypeCol.setCellValueFactory(d ->
                new SimpleStringProperty(safeText(d.getValue().getProductType())));
        lowStockQtyCol.setCellValueFactory(d ->
                new SimpleStringProperty(String.format("%,d", d.getValue().getStock())));
        lowStockTable.setPlaceholder(new Label("No low-stock products."));
    }

    private void setupReviewTable() {
        reviewCustomerCol.setCellValueFactory(d ->
                new SimpleStringProperty(safeText(d.getValue().getCustomerName())));
        reviewProductCol.setCellValueFactory(d ->
                new SimpleStringProperty(safeText(d.getValue().getProductName())));
        reviewRatingCol.setCellValueFactory(d ->
                new SimpleStringProperty(formatStars(d.getValue().getRating()) + " " + d.getValue().getRating() + "/5"));
        reviewCommentCol.setCellValueFactory(d ->
                new SimpleStringProperty(safeText(d.getValue().getReviewText())));
        reviewDateCol.setCellValueFactory(d ->
                new SimpleStringProperty(formatDateTime(d.getValue().getReviewedAt())));
        reviewsTable.setPlaceholder(new Label("No customer reviews yet."));
    }

    private void setupVoucherTable() {
        voucherCodeCol.setCellValueFactory(d ->
                new SimpleStringProperty(safeText(d.getValue().getCode())));
        voucherTypeCol.setCellValueFactory(d ->
                new SimpleStringProperty(safeText(d.getValue().getDiscountType())));
        voucherCreatedByCol.setCellValueFactory(d ->
                new SimpleStringProperty(safeText(d.getValue().getCreatedBy())));
        voucherValueCol.setCellValueFactory(d ->
                new SimpleStringProperty(formatVoucherValue(d.getValue().getDiscountType(), d.getValue().getDiscountValue())));
        voucherMinOrderCol.setCellValueFactory(d ->
                new SimpleStringProperty(formatCurrency(d.getValue().getMinimumOrder())));
        voucherExpiryCol.setCellValueFactory(d ->
                new SimpleStringProperty(safeText(d.getValue().getExpiresAt())));
        voucherStatusCol.setCellValueFactory(d ->
                new SimpleStringProperty(safeText(d.getValue().getActive())));
        vouchersTable.setPlaceholder(new Label("No vouchers added yet."));
    }

    private TableCell<OrderRow, String> buildStatusCell() {
        return new TableCell<OrderRow, String>() {
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
                badge.getStyleClass().add(OrderStatusUtil.statusCssClass(item));
                setGraphic(badge);
                setText(null);
            }
        };
    }

    private void refreshAllData() {
        loadSummaryCards();
        loadRecentOrders();
        loadTopProducts();
        loadLowStockProducts();
        loadReviews();
        loadVouchers();
    }

    private void loadSummaryCards() {
        int totalProducts = queryInt("SELECT COUNT(*) FROM tbl_products");
        int totalOrders = queryInt("SELECT COUNT(*) FROM tbl_orders");
        int totalCustomers = queryInt("SELECT COUNT(*) FROM tbl_acc WHERE LOWER(u_role) = 'user'");
        int lowStockItems = queryInt("SELECT COUNT(*) FROM tbl_products WHERE COALESCE(p_stock, 0) <= ?", LOW_STOCK_THRESHOLD);

        double totalRevenue = queryDouble(
                "SELECT COALESCE(SUM(total), 0) FROM tbl_orders WHERE LOWER(COALESCE(status,'')) = LOWER(?)",
                OrderStatusUtil.STATUS_DELIVERED
        );
        double todayRevenue = queryDouble(
                "SELECT COALESCE(SUM(total), 0) FROM tbl_orders "
                + "WHERE LOWER(COALESCE(status,'')) = LOWER(?) "
                + "AND date(COALESCE(created_at, datetime('now'))) = date('now')",
                OrderStatusUtil.STATUS_DELIVERED
        );
        double weekRevenue = queryDouble(
                "SELECT COALESCE(SUM(total), 0) FROM tbl_orders "
                + "WHERE LOWER(COALESCE(status,'')) = LOWER(?) "
                + "AND date(COALESCE(created_at, datetime('now'))) >= date('now', '-6 days')",
                OrderStatusUtil.STATUS_DELIVERED
        );
        double monthRevenue = queryDouble(
                "SELECT COALESCE(SUM(total), 0) FROM tbl_orders "
                + "WHERE LOWER(COALESCE(status,'')) = LOWER(?) "
                + "AND strftime('%Y-%m', COALESCE(created_at, datetime('now'))) = strftime('%Y-%m', 'now')",
                OrderStatusUtil.STATUS_DELIVERED
        );

        totalProductsLabel.setText(String.format("%,d", totalProducts));
        totalOrdersLabel.setText(String.format("%,d", totalOrders));
        totalCustomersLabel.setText(String.format("%,d", totalCustomers));
        totalRevenueLabel.setText(formatCurrency(totalRevenue));
        todayRevenueLabel.setText(formatCurrency(todayRevenue));
        weekRevenueLabel.setText(formatCurrency(weekRevenue));
        monthRevenueLabel.setText(formatCurrency(monthRevenue));
        lowStockAlertLabel.setText(String.format("%,d", lowStockItems));
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
                        OrderStatusUtil.normalizeDisplayStatus(rs.getString("status")),
                        formatDate(rs.getString("created_at"))
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        recentOrdersTable.setItems(rows);
    }

    private void loadTopProducts() {
        ObservableList<TopProductRow> rows = FXCollections.observableArrayList();
        String sql = "SELECT COALESCE(p.p_name, 'Unknown Product') AS product_name, "
                + "COALESCE(SUM(oi.qty), 0) AS units_sold, "
                + "COALESCE(SUM(oi.qty * oi.price), 0) AS revenue "
                + "FROM tbl_order_items oi "
                + "JOIN tbl_orders o ON o.o_id = oi.o_id "
                + "LEFT JOIN tbl_products p ON p.p_id = oi.p_id "
                + "WHERE LOWER(COALESCE(o.status,'')) = LOWER(?) "
                + "GROUP BY oi.p_id, COALESCE(p.p_name, 'Unknown Product') "
                + "ORDER BY units_sold DESC, revenue DESC "
                + "LIMIT 5";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, OrderStatusUtil.STATUS_DELIVERED);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new TopProductRow(
                            rs.getString("product_name"),
                            rs.getInt("units_sold"),
                            rs.getDouble("revenue")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        topProductsTable.setItems(rows);
    }

    private void loadLowStockProducts() {
        ObservableList<LowStockRow> rows = FXCollections.observableArrayList();
        String sql = "SELECT p_id, COALESCE(p_name, 'Unknown Product') AS p_name, "
                + "COALESCE(p_type, '-') AS p_type, COALESCE(p_stock, 0) AS p_stock "
                + "FROM tbl_products "
                + "WHERE COALESCE(p_stock, 0) <= ? "
                + "ORDER BY COALESCE(p_stock, 0) ASC, LOWER(COALESCE(p_name, '')) ASC";

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, LOW_STOCK_THRESHOLD);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new LowStockRow(
                            rs.getInt("p_id"),
                            rs.getString("p_name"),
                            rs.getString("p_type"),
                            rs.getInt("p_stock")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        lowStockTable.setItems(rows);
    }

    private void loadReviews() {
        ObservableList<AdminReviewRow> rows = FXCollections.observableArrayList();
        String sql = "SELECT r.review_id, COALESCE(a.u_name, 'Customer') AS customer_name, "
                + "COALESCE(p.p_name, 'Unknown Product') AS product_name, "
                + "COALESCE(r.rating, 0) AS rating, COALESCE(r.review_text, '') AS review_text, "
                + "COALESCE(r.review_image, '') AS review_image, COALESCE(r.created_at, '') AS created_at "
                + "FROM tbl_review r "
                + "LEFT JOIN tbl_acc a ON a.u_id = r.u_id "
                + "LEFT JOIN tbl_products p ON p.p_id = r.p_id "
                + "ORDER BY datetime(COALESCE(r.created_at, '1970-01-01 00:00:00')) DESC, r.review_id DESC "
                + "LIMIT 50";

        try (Connection conn = config.connectDB()) {
            if (conn == null) return;
            ReviewDataUtil.ensureReviewTable(conn);

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    rows.add(new AdminReviewRow(
                            rs.getInt("review_id"),
                            rs.getString("customer_name"),
                            rs.getString("product_name"),
                            rs.getInt("rating"),
                            rs.getString("review_text"),
                            rs.getString("review_image"),
                            rs.getString("created_at")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        reviewsTable.setItems(rows);
    }

    private void loadVouchers() {
        ObservableList<VoucherRow> rows = FXCollections.observableArrayList();
        String sql = "SELECT voucher_id, code, discount_type, discount_value, minimum_order, "
                + "COALESCE(expires_at, '') AS expires_at, COALESCE(created_by, '') AS created_by, "
                + "COALESCE(is_active, 1) AS is_active "
                + "FROM tbl_vouchers "
                + "ORDER BY COALESCE(is_active, 1) DESC, datetime(COALESCE(created_at, datetime('now'))) DESC, voucher_id DESC";

        try (Connection conn = config.connectDB()) {
            if (conn == null) return;
            VoucherDataUtil.ensureVoucherTable(conn);

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    rows.add(new VoucherRow(
                            rs.getInt("voucher_id"),
                            rs.getString("code"),
                            rs.getString("discount_type"),
                            rs.getDouble("discount_value"),
                            rs.getDouble("minimum_order"),
                            rs.getString("expires_at"),
                            rs.getInt("is_active") == 1 ? "Active" : "Inactive",
                            rs.getString("created_by")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        vouchersTable.setItems(rows);
    }

    private int queryInt(String sql, Object... params) {
        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double queryDouble(String sql, Object... params) {
        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private void bindParameters(PreparedStatement ps, Object... params) throws Exception {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private String formatCurrency(double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        String currency = format.format(value);
        if (currency.startsWith("PHP")) return currency.replaceFirst("PHP", "\u20B1");
        if (currency.startsWith("Php")) return currency.replaceFirst("Php", "\u20B1");
        return currency;
    }

    private String formatVoucherValue(String type, double value) {
        if (type != null && type.toLowerCase(Locale.ENGLISH).contains("percent")) {
            return String.format(Locale.ENGLISH, "%.0f%%", value);
        }
        return formatCurrency(value);
    }

    private String formatDate(String dbDate) {
        LocalDateTime date = parseDbDateTime(dbDate);
        if (date == null) return dbDate == null ? "-" : dbDate;
        return date.format(UI_DATE);
    }

    private String formatDateTime(String dbDate) {
        LocalDateTime date = parseDbDateTime(dbDate);
        if (date == null) return dbDate == null ? "-" : dbDate;
        return date.format(UI_DATE_TIME);
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

    private String formatStars(int rating) {
        int stars = Math.max(0, Math.min(5, rating));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stars; i++) sb.append('\u2605');
        return sb.toString();
    }

    private String safeText(String value) {
        if (value == null || value.trim().isEmpty()) return "-";
        return value.trim();
    }

    private void setDashboardMessage(String message, boolean isError) {
        if (dashboardMessageLabel == null) return;
        dashboardMessageLabel.setText(message == null ? "" : message);
        if (isError) {
            dashboardMessageLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 800;");
        } else {
            dashboardMessageLabel.setStyle("-fx-text-fill: rgba(15, 23, 42, 0.62); -fx-font-weight: 800;");
        }
    }

    @FXML
    private void refreshDashboardAction(ActionEvent event) {
        refreshAllData();
        setDashboardMessage("Dashboard data refreshed.", false);
    }

    @FXML
    private void refreshVouchersAction(ActionEvent event) {
        loadVouchers();
        setDashboardMessage("Voucher list refreshed.", false);
    }

    @FXML
    private void openInventoryFromLowStockAction(ActionEvent event) throws IOException {
        inventoryButtonAction(event);
    }

    @FXML
    private void deleteSelectedReviewAction(ActionEvent event) {
        AdminReviewRow selected = reviewsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setDashboardMessage("Select a review first.", true);
            return;
        }

        try {
            int deleted = config.deleteRecord("DELETE FROM tbl_review WHERE review_id = ?", selected.getReviewId());
            if (deleted <= 0) {
                setDashboardMessage("No review was deleted.", true);
                return;
            }
            loadReviews();
            setDashboardMessage("Selected review deleted.", false);
        } catch (Exception e) {
            e.printStackTrace();
            setDashboardMessage("Failed to delete the selected review.", true);
        }
    }

    @FXML
    private void addVoucherAction(ActionEvent event) {
        String code = VoucherDataUtil.normalizeCode(voucherCodeField == null ? "" : voucherCodeField.getText());
        String type = voucherTypeCombo == null ? "" : safeText(voucherTypeCombo.getValue());
        String active = voucherActiveCombo == null ? "Active" : safeText(voucherActiveCombo.getValue());
        String expiry = voucherExpiryPicker != null && voucherExpiryPicker.getValue() != null
                ? voucherExpiryPicker.getValue().toString()
                : "";

        if (code.length() < 3 || !code.matches("^[A-Z0-9-]{3,20}$")) {
            setDashboardMessage("Voucher code must be 3-20 characters using letters, numbers, or dash.", true);
            return;
        }

        Double discountValue = parseDouble(voucherValueField == null ? "" : voucherValueField.getText(), "Discount value");
        if (discountValue == null) return;
        if (discountValue <= 0) {
            setDashboardMessage("Discount value must be greater than zero.", true);
            return;
        }

        if (VoucherDataUtil.TYPE_PERCENT.equals(type) && discountValue > 100) {
            setDashboardMessage("Percent discount cannot be more than 100.", true);
            return;
        }

        Double minimumOrder = parseDouble(voucherMinOrderField == null ? "" : voucherMinOrderField.getText(), "Minimum order");
        if (minimumOrder == null) return;
        if (minimumOrder < 0) {
            setDashboardMessage("Minimum order cannot be negative.", true);
            return;
        }

        String createdBy = AdminSession.getName();
        if (createdBy == null || createdBy.trim().isEmpty()) {
            createdBy = "Unknown";
        }

        String sql = "INSERT INTO tbl_vouchers(code, discount_type, discount_value, minimum_order, expires_at, is_active, created_by) "
                + "VALUES(?,?,?,?,?,?,?)";

        try (Connection conn = config.connectDB()) {
            if (conn == null) {
                setDashboardMessage("Unable to connect to the database.", true);
                return;
            }

            VoucherDataUtil.ensureVoucherTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, code);
                ps.setString(2, type);
                ps.setDouble(3, discountValue);
                ps.setDouble(4, minimumOrder);
                ps.setString(5, expiry);
                ps.setInt(6, "Active".equalsIgnoreCase(active) ? 1 : 0);
                ps.setString(7, createdBy);
                ps.executeUpdate();
            }

            clearVoucherForm();
            loadVouchers();
            setDashboardMessage("Voucher added successfully.", false);
        } catch (Exception e) {
            e.printStackTrace();
            setDashboardMessage("Failed to add voucher. Make sure the code is unique.", true);
        }
    }

    @FXML
    private void deleteVoucherAction(ActionEvent event) {
        VoucherRow selected = vouchersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setDashboardMessage("Select a voucher first.", true);
            return;
        }

        try {
            int deleted = config.deleteRecord("DELETE FROM tbl_vouchers WHERE voucher_id = ?", selected.getVoucherId());
            if (deleted <= 0) {
                setDashboardMessage("No voucher was deleted.", true);
                return;
            }
            loadVouchers();
            setDashboardMessage("Selected voucher deleted.", false);
        } catch (Exception e) {
            e.printStackTrace();
            setDashboardMessage("Failed to delete the selected voucher.", true);
        }
    }

    private void clearVoucherForm() {
        if (voucherCodeField != null) voucherCodeField.clear();
        if (voucherValueField != null) voucherValueField.clear();
        if (voucherMinOrderField != null) voucherMinOrderField.setText("0");
        if (voucherTypeCombo != null) voucherTypeCombo.getSelectionModel().selectFirst();
        if (voucherActiveCombo != null) voucherActiveCombo.getSelectionModel().selectFirst();
        if (voucherExpiryPicker != null) voucherExpiryPicker.setValue((LocalDate) null);
    }

    private Double parseDouble(String raw, String label) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            setDashboardMessage(label + " is required.", true);
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            setDashboardMessage(label + " must be a valid number.", true);
            return null;
        }
    }

    private void makeCircle(ImageView imageView) {
        double w = imageView.getFitWidth();
        double h = imageView.getFitHeight();
        double radius = Math.min(w, h) / 2.0;
        imageView.setClip(new Circle(w / 2.0, h / 2.0, radius));
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
    private void inventoryButtonAction(ActionEvent event) throws IOException {
        openScene(event, "/AdminFXML/adminInventory.fxml");
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
