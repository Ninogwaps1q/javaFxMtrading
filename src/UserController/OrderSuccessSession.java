package UserController;

public final class OrderSuccessSession {

    private static int lastOrderId;

    private OrderSuccessSession() {
    }

    public static void setLastOrderId(int orderId) {
        lastOrderId = orderId;
    }

    public static int getLastOrderId() {
        return lastOrderId;
    }

    public static void clear() {
        lastOrderId = 0;
    }
}
