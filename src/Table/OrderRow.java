package Table;

public class OrderRow {

    private final int orderId;
    private final String customer;
    private final double amount;
    private final String status;
    private final String date;

    public OrderRow(int orderId, String customer, double amount, String status, String date) {
        this.orderId = orderId;
        this.customer = customer;
        this.amount = amount;
        this.status = status;
        this.date = date;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getCustomer() {
        return customer;
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
