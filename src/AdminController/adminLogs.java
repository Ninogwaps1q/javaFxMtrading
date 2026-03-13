package AdminController;

import Table.LoginLogRow;
import config.AuditLogService;
import config.SessionAuditUtil;
import config.config;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class adminLogs implements Initializable {

    private static final DateTimeFormatter DB_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter UI_DATE_TIME =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a", Locale.ENGLISH);

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
    private Label totalLogsMetaLabel;
    @FXML
    private TableView<LoginLogRow> logsTable;
    @FXML
    private TableColumn<LoginLogRow, String> nameCol;
    @FXML
    private TableColumn<LoginLogRow, String> emailCol;
    @FXML
    private TableColumn<LoginLogRow, String> roleCol;
    @FXML
    private TableColumn<LoginLogRow, String> loginAtCol;
    @FXML
    private TableColumn<LoginLogRow, String> logoutAtCol;
    @FXML
    private TableColumn<LoginLogRow, String> statusCol;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        makeCircle(logo);
        setupTable();
        loadLogs();
    }

    private void setupTable() {
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        emailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        roleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));
        loginAtCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLoginAt()));
        logoutAtCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLogoutAt()));
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));

        statusCol.setCellFactory(col -> new TableCell<LoginLogRow, String>() {
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
                badge.setStyle(statusBadgeStyle(item));
                setText(null);
                setGraphic(badge);
            }
        });

        logsTable.setPlaceholder(new Label("No login records yet."));
    }

    private void loadLogs() {
        ObservableList<LoginLogRow> rows = FXCollections.observableArrayList();
        int activeCount = 0;
        String sql = "SELECT log_id, COALESCE(u_name, 'Unknown') AS u_name, "
                + "COALESCE(u_email, '-') AS u_email, COALESCE(u_role, '-') AS u_role, "
                + "login_at, COALESCE(logout_at, '') AS logout_at "
                + "FROM tbl_login_logs "
                + "ORDER BY datetime(login_at) DESC, log_id DESC";

        AuditLogService.ensureAuditLogTable();

        try (Connection conn = config.connectDB();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String logoutAt = rs.getString("logout_at");
                boolean active = logoutAt == null || logoutAt.trim().isEmpty();
                if (active) activeCount++;

                rows.add(new LoginLogRow(
                        rs.getInt("log_id"),
                        rs.getString("u_name"),
                        rs.getString("u_email"),
                        rs.getString("u_role"),
                        formatDateTime(rs.getString("login_at")),
                        active ? "-" : formatDateTime(logoutAt),
                        active ? "Active" : "Logged Out"
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        logsTable.setItems(rows);
        totalLogsMetaLabel.setText(
                "Total Sessions: " + String.format("%,d", rows.size()) + " | Active: " + String.format("%,d", activeCount));
    }

    private String formatDateTime(String dbDate) {
        if (dbDate == null || dbDate.trim().isEmpty()) return "-";

        try {
            return LocalDateTime.parse(dbDate.trim(), DB_DATE_TIME).format(UI_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dbDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).format(UI_DATE_TIME);
            } catch (Exception ignored) {
                return dbDate;
            }
        }
    }

    private String statusBadgeStyle(String status) {
        if ("Active".equalsIgnoreCase(status)) {
            return "-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-font-weight: 900;";
        }
        return "-fx-background-color: rgba(15, 23, 42, 0.12); -fx-text-fill: #0f172a; -fx-font-weight: 900;";
    }

    private void makeCircle(ImageView imageView) {
        if (imageView == null) return;

        double w = imageView.getFitWidth();
        double h = imageView.getFitHeight();
        double radius = Math.min(w, h) / 2.0;
        imageView.setClip(new Circle(w / 2.0, h / 2.0, radius));
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
    private void inventoryButtonAction(ActionEvent event) throws IOException {
        openScene(event, "/AdminFXML/adminInventory.fxml");
    }

    @FXML
    private void salesButtonAction(ActionEvent event) throws IOException {
        openScene(event, "/AdminFXML/adminSale.fxml");
    }

    @FXML
    private void userButtonAction(ActionEvent event) throws IOException {
        openScene(event, "/AdminFXML/adminUser.fxml");
    }

    @FXML
    private void logsButtonAction(ActionEvent event) throws IOException {
        openScene(event, "/AdminFXML/adminLogs.fxml");
    }

    @FXML
    private void refreshLogsAction(ActionEvent event) {
        loadLogs();
    }

    @FXML
    private void logoutButtonAction(ActionEvent event) throws IOException {
        SessionAuditUtil.logoutAdminSession();
        openScene(event, "/Main/Login.fxml");
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
