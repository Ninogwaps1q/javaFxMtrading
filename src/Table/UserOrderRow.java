package Table;

public class UserOrderRow {

    private final int orderId;
    private final double amount;
    private final String status;
    private final String date;

    public UserOrderRow(int orderId, double amount, String status, String date) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.date = date;
    }

    public int getOrderId() {
        return orderId;
    }

    public double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getDate() {
        return date;
    }
}
