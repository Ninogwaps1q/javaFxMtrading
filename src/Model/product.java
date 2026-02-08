
package Model;


public class product {
    
    private int id;
    private String name;
    private String type;
    private String desc;
    private int stock;
    private double price;
    private String image;

    public product(int id, String name, String type, String desc, int stock, double price, String image) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.desc = desc;
        this.stock = stock;
        this.price = price;
        this.image = image;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDesc() { return desc; }
    public int getStock() { return stock; }
    public double getPrice() { return price; }
    public String getImage() { return image; }
}
