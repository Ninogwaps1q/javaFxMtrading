package Table;

public class InventoryRow {

    private final int inventoryId;
    private final int productId;
    private final String productName;
    private final String adjustmentType;
    private final int quantity;
    private final int stockBefore;
    private final int stockAfter;
    private final String note;
    private final String adjustedBy;
    private final String createdAt;

    public InventoryRow(int inventoryId, int productId, String productName, String adjustmentType,
            int quantity, int stockBefore, int stockAfter, String note, String adjustedBy, String createdAt) {
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.productName = productName;
        this.adjustmentType = adjustmentType;
        this.quantity = quantity;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.note = note;
        this.adjustedBy = adjustedBy;
        this.createdAt = createdAt;
    }

    public int getInventoryId() {
        return inventoryId;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getAdjustmentType() {
        return adjustmentType;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getStockBefore() {
        return stockBefore;
    }

    public int getStockAfter() {
        return stockAfter;
    }

    public String getNote() {
        return note;
    }

    public String getAdjustedBy() {
        return adjustedBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
