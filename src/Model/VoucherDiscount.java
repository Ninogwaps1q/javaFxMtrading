package Model;

public class VoucherDiscount {

    private final boolean valid;
    private final String message;
    private final String code;
    private final String discountType;
    private final double discountValue;
    private final double discountAmount;
    private final double minimumOrder;
    private final double grossTotal;
    private final double payableTotal;

    private VoucherDiscount(boolean valid, String message, String code, String discountType,
            double discountValue, double discountAmount, double minimumOrder,
            double grossTotal, double payableTotal) {
        this.valid = valid;
        this.message = message;
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.discountAmount = discountAmount;
        this.minimumOrder = minimumOrder;
        this.grossTotal = grossTotal;
        this.payableTotal = payableTotal;
    }

    public static VoucherDiscount valid(String code, String discountType, double discountValue,
            double discountAmount, double minimumOrder, double grossTotal, double payableTotal) {
        return new VoucherDiscount(true, null, code, discountType, discountValue, discountAmount,
                minimumOrder, grossTotal, payableTotal);
    }

    public static VoucherDiscount invalid(String message, double grossTotal) {
        return new VoucherDiscount(false, message, "", "", 0.0, 0.0, 0.0, grossTotal, grossTotal);
    }

    public static VoucherDiscount none(double grossTotal) {
        return new VoucherDiscount(true, "", "", "", 0.0, 0.0, 0.0, grossTotal, grossTotal);
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public String getDiscountType() {
        return discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getMinimumOrder() {
        return minimumOrder;
    }

    public double getGrossTotal() {
        return grossTotal;
    }

    public double getPayableTotal() {
        return payableTotal;
    }

    public boolean hasVoucher() {
        return code != null && !code.trim().isEmpty();
    }
}
