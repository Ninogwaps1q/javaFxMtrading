package Model;

public class CartItem {
    private int productId;
    private String name;
    private double price;
    private int stock;
    private int qty;
    private String image;

    public CartItem(int productId, String name, double price, int stock, int qty, String image) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.qty = qty;
        this.image = image;
    }

    public int getProductId() { return productId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public int getQty() { return qty; }
    public String getImage() { return image; }

    public void setQty(int qty) { this.qty = qty; }

    public double getSubtotal() {
        return price * qty;
    }
}
