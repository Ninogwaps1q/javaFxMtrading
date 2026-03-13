package Table;

public class AdminReviewRow {

    private final int reviewId;
    private final String customerName;
    private final String productName;
    private final int rating;
    private final String reviewText;
    private final String reviewImage;
    private final String reviewedAt;

    public AdminReviewRow(int reviewId, String customerName, String productName, int rating,
            String reviewText, String reviewImage, String reviewedAt) {
        this.reviewId = reviewId;
        this.customerName = customerName;
        this.productName = productName;
        this.rating = rating;
        this.reviewText = reviewText;
        this.reviewImage = reviewImage;
        this.reviewedAt = reviewedAt;
    }

    public int getReviewId() {
        return reviewId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getProductName() {
        return productName;
    }

    public int getRating() {
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
}
