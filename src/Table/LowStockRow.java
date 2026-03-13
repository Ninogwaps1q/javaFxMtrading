package Table;

public class LowStockRow {

    private final int productId;
    private final String productName;
    private final String productType;
    private final int stock;

    public LowStockRow(int productId, String productName, String productType, int stock) {
        this.productId = productId;
        this.productName = productName;
        this.productType = productType;
        this.stock = stock;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductType() {
        return productType;
    }

    public int getStock() {
        return stock;
    }
}
