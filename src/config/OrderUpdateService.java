package config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class OrderUpdateService {

    private OrderUpdateService() {
    }

    public static UpdateResult updateOrderStatus(
            int orderId,
            String newStatus,
            String handledByEmail,
            String handledByName,
            String handledByRole) {

        String nextStatus = OrderStatusUtil.normalizeDisplayStatus(newStatus);
        if (!OrderStatusUtil.isStaffSelectableStatus(nextStatus)) {
            return UpdateResult.error("Invalid order status selected.");
        }

        Connection conn = null;
        try {
            conn = config.connectDB();
            if (conn == null) {
                return UpdateResult.error("Unable to connect to the database.");
            }

            conn.setAutoCommit(false);

            String currentStatus = findCurrentStatus(conn, orderId);
            if (currentStatus == null) {
                conn.rollback();
                return UpdateResult.error("No matching order was found.");
            }

            if (!OrderStatusUtil.canStaffUpdate(currentStatus)) {
                conn.rollback();
                if (OrderStatusUtil.isDelivered(currentStatus)) {
                    return UpdateResult.error("This order is already delivered and can no longer be updated.");
                }
                return UpdateResult.error("This order is already cancelled and can no longer be updated.");
            }

            boolean shouldNotifyReadyToDeliver =
                    OrderStatusUtil.isReadyToDeliver(nextStatus)
                    && !OrderStatusUtil.isReadyToDeliver(currentStatus);

            boolean restoredStock = false;
            if (OrderStatusUtil.isCancelled(nextStatus) && !OrderStatusUtil.isCancelled(currentStatus)) {
                restoreOrderStock(conn, orderId);
                restoredStock = true;
            }

            int updated = saveOrderStatus(conn, orderId, nextStatus, handledByEmail, handledByName, handledByRole);
            if (updated <= 0) {
                conn.rollback();
                return UpdateResult.error("No matching order was updated.");
            }

            CustomerContact customerContact = shouldNotifyReadyToDeliver
                    ? findCustomerContact(conn, orderId)
                    : null;

            conn.commit();
            String notificationNote = shouldNotifyReadyToDeliver
                    ? sendReadyToDeliverEmail(orderId, customerContact)
                    : null;
            return UpdateResult.success(nextStatus, restoredStock, notificationNote);
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    rollbackError.printStackTrace();
                }
            }
            e.printStackTrace();
            return UpdateResult.error("Failed to update order status.");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeError) {
                    closeError.printStackTrace();
                }
            }
        }
    }

    private static String findCurrentStatus(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT status FROM tbl_orders WHERE o_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        }
        return null;
    }

    private static CustomerContact findCustomerContact(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT COALESCE(a.u_name, '') AS customer_name, COALESCE(a.u_email, '') AS customer_email "
                + "FROM tbl_orders o "
                + "LEFT JOIN tbl_acc a ON a.u_id = o.u_id "
                + "WHERE o.o_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CustomerContact(
                            rs.getString("customer_name"),
                            rs.getString("customer_email")
                    );
                }
            }
        }
        return null;
    }

    private static void restoreOrderStock(Connection conn, int orderId) throws SQLException {
        String itemSql = "SELECT p_id, qty FROM tbl_order_items WHERE o_id=?";
        String stockSql = "UPDATE tbl_products SET p_stock = COALESCE(p_stock, 0) + ? WHERE p_id=?";

        try (PreparedStatement itemPs = conn.prepareStatement(itemSql);
             PreparedStatement stockPs = conn.prepareStatement(stockSql)) {

            itemPs.setInt(1, orderId);
            try (ResultSet rs = itemPs.executeQuery()) {
                while (rs.next()) {
                    stockPs.setInt(1, rs.getInt("qty"));
                    stockPs.setInt(2, rs.getInt("p_id"));
                    stockPs.addBatch();
                }
            }

            stockPs.executeBatch();
        }
    }

    private static int saveOrderStatus(
            Connection conn,
            int orderId,
            String status,
            String handledByEmail,
            String handledByName,
            String handledByRole) throws SQLException {

        String sql = "UPDATE tbl_orders SET status=?, handled_by_email=?, handled_by_name=?, "
                + "handled_by_role=?, handled_at=datetime('now') WHERE o_id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, handledByEmail == null ? "" : handledByEmail.trim());
            ps.setString(3, handledByName == null ? "" : handledByName.trim());
            ps.setString(4, handledByRole == null ? "" : handledByRole.trim());
            ps.setInt(5, orderId);
            return ps.executeUpdate();
        }
    }

    private static String sendReadyToDeliverEmail(int orderId, CustomerContact customerContact) {
        if (customerContact == null || customerContact.email.trim().isEmpty()) {
            return "Customer email was not found, so no Ready to Deliver email was sent.";
        }

        String customerName = customerContact.name == null || customerContact.name.trim().isEmpty()
                ? "Customer"
                : customerContact.name.trim();

        String subject = "Order Ready to Deliver - Melynal Trading";
        String body = "Hello " + customerName + ",\n\n"
                + "Your order #" + String.format("%06d", orderId) + " is now Ready to Deliver.\n"
                + "Please check your Melynal Trading orders page for the latest update.\n\n"
                + "Thank you,\n"
                + "Melynal Trading";

        boolean sent = new config().sendEmail(customerContact.email.trim(), subject, body);
        if (sent) {
            return "Ready to Deliver email sent to the customer.";
        }
        return "Order updated, but the Ready to Deliver email could not be sent.";
    }

    public static final class UpdateResult {

        private final boolean success;
        private final String status;
        private final boolean restoredStock;
        private final String message;
        private final String notificationNote;

        private UpdateResult(boolean success, String status, boolean restoredStock, String message, String notificationNote) {
            this.success = success;
            this.status = status;
            this.restoredStock = restoredStock;
            this.message = message;
            this.notificationNote = notificationNote;
        }

        public static UpdateResult success(String status, boolean restoredStock, String notificationNote) {
            return new UpdateResult(true, status, restoredStock, null, notificationNote);
        }

        public static UpdateResult error(String message) {
            return new UpdateResult(false, null, false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getStatus() {
            return status;
        }

        public boolean isRestoredStock() {
            return restoredStock;
        }

        public String getMessage() {
            return message;
        }

        public String getNotificationNote() {
            return notificationNote;
        }
    }

    private static final class CustomerContact {

        private final String name;
        private final String email;

        private CustomerContact(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
