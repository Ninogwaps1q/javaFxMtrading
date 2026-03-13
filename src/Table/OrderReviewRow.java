package Table;

public class OrderReviewRow {

    private final int productId;
    private final String productName;
    private final String productImage;
    private final int quantity;
    private final double unitPrice;
    private final Integer rating;
    private final String reviewText;
    private final String reviewImage;
    private final String reviewedAt;

    public OrderReviewRow(int productId, String productName, String productImage, int quantity,
            double unitPrice, Integer rating, String reviewText, String reviewImage, String reviewedAt) {
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.rating = rating;
        this.reviewText = reviewText;
        this.reviewImage = reviewImage;
        this.reviewedAt = reviewedAt;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductImage() {
        return productImage;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public Integer getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public String getReviewImage() {
        return reviewImage;
    }

    public String getReviewedAt() {
        return reviewedAt;
    }

    public boolean hasReview() {
        return rating != null && rating > 0;
    }

    public boolean hasReviewText() {
        return reviewText != null && !reviewText.trim().isEmpty();
    }
}
