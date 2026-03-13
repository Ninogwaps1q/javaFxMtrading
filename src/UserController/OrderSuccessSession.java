package UserController;

public final class OrderSuccessSession {

    private static int lastOrderId;
    private static String entryMessage;

    private OrderSuccessSession() {
    }

    public static void setLastOrderId(int orderId) {
        lastOrderId = orderId;
        entryMessage = null;
    }

    public static int getLastOrderId() {
        return lastOrderId;
    }

    public static void setEntryMessage(String message) {
        entryMessage = message;
    }

    public static String getEntryMessage() {
        return entryMessage == null ? "" : entryMessage;
    }

    public static void clear() {
        lastOrderId = 0;
        entryMessage = null;
    }
}
