package Table;

public class ProductReviewRow {

    private final String reviewerName;
    private final int rating;
    private final String reviewText;
    private final String reviewImage;
    private final String reviewedAt;

    public ProductReviewRow(String reviewerName, int rating, String reviewText, String reviewImage, String reviewedAt) {
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.reviewText = reviewText;
        this.reviewImage = reviewImage;
        this.reviewedAt = reviewedAt;
    }

    public String getReviewerName() {
        return reviewerName;
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

    public boolean hasReviewText() {
        return reviewText != null && !reviewText.trim().isEmpty();
    }

    public boolean hasReviewImage() {
        return reviewImage != null && !reviewImage.trim().isEmpty();
    }
}
