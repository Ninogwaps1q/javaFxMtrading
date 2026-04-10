package Table;

public class VoucherRow {

    private final int voucherId;
    private final String code;
    private final String discountType;
    private final double discountValue;
    private final double minimumOrder;
    private final String expiresAt;
    private final String active;
    private final String createdBy;

    public VoucherRow(int voucherId, String code, String discountType, double discountValue,
            double minimumOrder, String expiresAt, String active, String createdBy) {
        this.voucherId = voucherId;
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrder = minimumOrder;
        this.expiresAt = expiresAt;
        this.active = active;
        this.createdBy = createdBy;
    }

    public int getVoucherId() {
        return voucherId;
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

    public double getMinimumOrder() {
        return minimumOrder;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public String getActive() {
        return active;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
