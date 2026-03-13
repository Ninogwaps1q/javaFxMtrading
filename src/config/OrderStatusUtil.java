package config;

import java.util.Locale;

public final class OrderStatusUtil {

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_SHIPPED = "Shipped";
    public static final String STATUS_READY_TO_DELIVER = "Ready to Deliver";
    public static final String STATUS_DELIVERED = "Delivered";
    public static final String STATUS_CANCELLED = "Cancelled";

    private OrderStatusUtil() {
    }

    public static String normalizeDisplayStatus(String status) {
        String normalized = normalizeKey(status);
        if ("ready_to_deliver".equals(normalized)) return STATUS_READY_TO_DELIVER;
        if ("delivered".equals(normalized)) return STATUS_DELIVERED;
        if ("shipped".equals(normalized)) return STATUS_SHIPPED;
        if ("cancelled".equals(normalized)) return STATUS_CANCELLED;
        return STATUS_PENDING;
    }

    public static String normalizeKey(String status) {
        if (status == null) return "pending";

        String value = status.trim().toLowerCase(Locale.ENGLISH);
        if (value.contains("ready") && value.contains("deliver")) return "ready_to_deliver";
        if (value.contains("deliver")) return "delivered";
        if (value.contains("ship")) return "shipped";
        if (value.contains("cancel")) return "cancelled";
        return "pending";
    }

    public static String statusCssClass(String status) {
        String normalized = normalizeKey(status);
        if ("ready_to_deliver".equals(normalized)) return "status-ready";
        if ("delivered".equals(normalized)) return "status-delivered";
        if ("shipped".equals(normalized)) return "status-shipped";
        if ("cancelled".equals(normalized)) return "status-cancelled";
        return "status-pending";
    }

    public static boolean isReadyToDeliver(String status) {
        return "ready_to_deliver".equals(normalizeKey(status));
    }

    public static boolean isDelivered(String status) {
        return "delivered".equals(normalizeKey(status));
    }

    public static boolean isCancelled(String status) {
        return "cancelled".equals(normalizeKey(status));
    }

    public static boolean canStaffUpdate(String status) {
        return !isDelivered(status) && !isCancelled(status);
    }

    public static boolean isStaffSelectableStatus(String status) {
        String normalized = normalizeKey(status);
        return "shipped".equals(normalized)
                || "ready_to_deliver".equals(normalized)
                || "cancelled".equals(normalized);
    }
}
