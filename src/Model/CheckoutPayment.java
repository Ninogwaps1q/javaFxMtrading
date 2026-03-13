package Model;

public class CheckoutPayment {

    private final String method;
    private final String reference;
    private final String voucherCode;
    private final double grossTotal;
    private final double discountAmount;
    private final double payableTotal;

    public CheckoutPayment(String method, String reference, String voucherCode,
            double grossTotal, double discountAmount, double payableTotal) {
        this.method = method;
        this.reference = reference;
        this.voucherCode = voucherCode;
        this.grossTotal = grossTotal;
        this.discountAmount = discountAmount;
        this.payableTotal = payableTotal;
    }

    public String getMethod() {
        return method;
    }

    public String getReference() {
        return reference;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public double getGrossTotal() {
        return grossTotal;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getPayableTotal() {
        return payableTotal;
    }
}
