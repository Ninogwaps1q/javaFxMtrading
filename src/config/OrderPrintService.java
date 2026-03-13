package config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

public final class OrderPrintService {

    private static final DateTimeFormatter DB_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter PRINT_DATE_TIME =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a", Locale.ENGLISH);

    private OrderPrintService() {
    }

    public static String autoPrintOrderNote(int orderId) {
        PrintStatus status = printOrder(orderId);
        switch (status) {
            case SUCCESS:
                return String.format(
                        Locale.ENGLISH,
                        "Order #%06d was sent to the printer automatically.",
                        orderId
                );
            case NO_PRINTER:
                return String.format(
                        Locale.ENGLISH,
                        "Order #%06d was saved, but auto-print was skipped because no default printer is connected.",
                        orderId
                );
            case LOAD_FAILED:
                return String.format(
                        Locale.ENGLISH,
                        "Order #%06d was saved, but the order slip could not be prepared for auto-print.",
                        orderId
                );
            default:
                return String.format(
                        Locale.ENGLISH,
                        "Order #%06d was saved, but auto-print failed.",
                        orderId
                );
        }
    }

    public static PrintStatus printOrder(int orderId) {
        OrderPrintData data = loadOrderPrintData(orderId);
        if (data == null) {
            return PrintStatus.LOAD_FAILED;
        }
        return printSlip(orderId, buildOrderSlipText(data));
    }

    private static OrderPrintData loadOrderPrintData(int orderId) {
        String orderSql = "SELECT o.o_id, o.total, o.created_at, "
                + "COALESCE(o.gross_total, o.total) AS gross_total, "
                + "COALESCE(o.discount_amount, 0) AS discount_amount, "
                + "COALESCE(o.voucher_code, '') AS voucher_code, "
                + "COALESCE(a.u_name, 'Unknown') AS customer, "
                + "COALESCE(a.u_phone, '-') AS phone, "
                + "COALESCE(a.u_address, '-') AS address "
                + "FROM tbl_orders o "
                + "LEFT JOIN tbl_acc a ON a.u_id = o.u_id "
                + "WHERE o.o_id = ?";

        String itemsSql = "SELECT COALESCE(p.p_name, 'Unknown Item') AS product_name, oi.qty, oi.price "
                + "FROM tbl_order_items oi "
                + "LEFT JOIN tbl_products p ON p.p_id = oi.p_id "
                + "WHERE oi.o_id = ? "
                + "ORDER BY oi.oi_id ASC";

        try (Connection conn = config.connectDB()) {
            if (conn == null) {
                return null;
            }

            addColumnIfMissing(conn, "tbl_orders", "gross_total", "REAL");
            addColumnIfMissing(conn, "tbl_orders", "discount_amount", "REAL");
            addColumnIfMissing(conn, "tbl_orders", "voucher_code", "TEXT");

            OrderPrintData data = null;
            try (PreparedStatement orderPs = conn.prepareStatement(orderSql)) {
                orderPs.setInt(1, orderId);
                try (ResultSet rs = orderPs.executeQuery()) {
                    if (rs.next()) {
                        data = new OrderPrintData();
                        data.orderId = rs.getInt("o_id");
                        data.customerName = safeText(rs.getString("customer"));
                        data.customerPhone = safeText(rs.getString("phone"));
                        data.customerAddress = safeText(rs.getString("address"));
                        data.createdAt = safeText(formatDateTime(rs.getString("created_at")));
                        data.total = rs.getDouble("total");
                        data.grossTotal = rs.getDouble("gross_total");
                        data.discountAmount = rs.getDouble("discount_amount");
                        data.voucherCode = safeText(rs.getString("voucher_code"));
                    }
                }
            }

            if (data == null) {
                return null;
            }

            try (PreparedStatement itemsPs = conn.prepareStatement(itemsSql)) {
                itemsPs.setInt(1, orderId);
                try (ResultSet rs = itemsPs.executeQuery()) {
                    while (rs.next()) {
                        data.items.add(new OrderPrintItem(
                                safeText(rs.getString("product_name")),
                                rs.getInt("qty"),
                                rs.getDouble("price")
                        ));
                    }
                }
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String type) throws Exception {
        if (columnExists(conn, table, column)) {
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type)) {
            ps.executeUpdate();
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) {
        String sql = "PRAGMA table_info(" + table + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String buildOrderSlipText(OrderPrintData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("MELYNAL TRADING").append('\n');
        sb.append("ORDER DETAILS").append('\n');
        sb.append(line('=', 42)).append('\n');
        sb.append(String.format(Locale.ENGLISH, "Order ID : #%06d%n", data.orderId));
        sb.append("Date     : ").append(data.createdAt).append('\n');
        sb.append(line('-', 42)).append('\n');
        sb.append("Customer").append('\n');
        sb.append("Name     : ").append(data.customerName).append('\n');
        sb.append("Phone    : ").append(data.customerPhone).append('\n');
        sb.append("Address  : ").append(data.customerAddress).append('\n');
        sb.append(line('-', 42)).append('\n');
        sb.append(String.format(Locale.ENGLISH, "%-20s %3s %14s%n", "Item", "Qty", "Subtotal"));
        sb.append(line('-', 42)).append('\n');

        if (data.items.isEmpty()) {
            sb.append("(No order items)").append('\n');
        } else {
            for (OrderPrintItem item : data.items) {
                sb.append(fitText(item.name, 20))
                        .append(' ')
                        .append(String.format(
                                Locale.ENGLISH,
                                "%3d %14s%n",
                                item.qty,
                                formatCurrency(item.lineTotal())
                        ));
            }
        }

        sb.append(line('-', 42)).append('\n');
        sb.append(String.format(Locale.ENGLISH, "Gross: %s%n", formatCurrency(data.grossTotal)));
        sb.append(String.format(Locale.ENGLISH, "Discount: %s%n", formatCurrency(data.discountAmount)));
        sb.append("Voucher: ").append(data.voucherCode).append('\n');
        sb.append(String.format(Locale.ENGLISH, "Total: %s%n", formatCurrency(data.total)));
        sb.append("Printed: ").append(LocalDateTime.now().format(PRINT_DATE_TIME)).append('\n');
        sb.append(line('=', 42)).append('\n');
        return sb.toString();
    }

    private static PrintStatus printSlip(int orderId, String slipText) {
        Printer defaultPrinter = Printer.getDefaultPrinter();
        if (defaultPrinter == null) {
            return PrintStatus.NO_PRINTER;
        }

        PrinterJob job = PrinterJob.createPrinterJob(defaultPrinter);
        if (job == null) {
            return PrintStatus.FAILED;
        }

        job.getJobSettings().setJobName("Order-" + orderId);

        Text printText = new Text(slipText);
        printText.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-fill: #111111;");

        VBox paper = new VBox(10);
        paper.setPrefWidth(380);
        paper.setStyle("-fx-background-color: white; -fx-padding: 18;");
        paper.setAlignment(Pos.TOP_LEFT);

        ImageView logoView = createReceiptLogo();
        if (logoView != null) {
            HBox logoBox = new HBox(logoView);
            logoBox.setAlignment(Pos.CENTER);
            paper.getChildren().add(logoBox);
        }
        paper.getChildren().add(printText);

        Group root = new Group(paper);
        new Scene(root);
        root.applyCss();
        root.layout();

        PageLayout page = job.getJobSettings().getPageLayout();
        double printableWidth = page.getPrintableWidth() - 8;
        double nodeWidth = paper.getBoundsInParent().getWidth();
        if (printableWidth > 0 && nodeWidth > printableWidth) {
            double scale = printableWidth / nodeWidth;
            paper.setScaleX(scale);
            paper.setScaleY(scale);
            root.applyCss();
            root.layout();
        }

        boolean printed = job.printPage(root);
        if (!printed) {
            return PrintStatus.FAILED;
        }

        boolean ended = job.endJob();
        return ended ? PrintStatus.SUCCESS : PrintStatus.FAILED;
    }

    private static ImageView createReceiptLogo() {
        try {
            java.net.URL logoUrl = OrderPrintService.class.getResource("/image/image6.jpg");
            if (logoUrl == null) {
                return null;
            }

            Image logo = new Image(logoUrl.toExternalForm(), false);
            if (logo.isError()) {
                return null;
            }

            double diameter = 84.0;
            ImageView logoView = new ImageView(logo);
            logoView.setPreserveRatio(false);
            logoView.setFitWidth(diameter);
            logoView.setFitHeight(diameter);
            logoView.setSmooth(true);
            logoView.setClip(new Circle(diameter / 2.0, diameter / 2.0, diameter / 2.0));
            return logoView;
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeText(String value) {
        if (value == null) {
            return "-";
        }
        String cleaned = value.replace('\n', ' ').replace('\r', ' ').trim();
        return cleaned.isEmpty() ? "-" : cleaned;
    }

    private static String fitText(String value, int width) {
        String text = safeText(value);
        if (text.length() > width) {
            text = text.substring(0, Math.max(0, width - 3)) + "...";
        }
        return String.format("%-" + width + "s", text);
    }

    private static String line(char c, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String formatCurrency(double value) {
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

    private static String formatDateTime(String dbDate) {
        LocalDateTime date = parseDbDateTime(dbDate);
        if (date == null) {
            return dbDate == null ? "-" : dbDate;
        }
        return date.format(PRINT_DATE_TIME);
    }

    private static LocalDateTime parseDbDateTime(String dbDate) {
        if (dbDate == null || dbDate.trim().isEmpty()) {
            return null;
        }
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

    public enum PrintStatus {
        SUCCESS,
        NO_PRINTER,
        LOAD_FAILED,
        FAILED
    }

    private static final class OrderPrintData {
        private int orderId;
        private String customerName;
        private String customerPhone;
        private String customerAddress;
        private String createdAt;
        private String voucherCode = "-";
        private double grossTotal;
        private double discountAmount;
        private double total;
        private final List<OrderPrintItem> items = new ArrayList<>();
    }

    private static final class OrderPrintItem {
        private final String name;
        private final int qty;
        private final double unitPrice;

        private OrderPrintItem(String name, int qty, double unitPrice) {
            this.name = name;
            this.qty = qty;
            this.unitPrice = unitPrice;
        }

        private double lineTotal() {
            return unitPrice * qty;
        }
    }
}
