/**
 * Represents a product in the store inventory.
 * Stores essential product information such as ID, name, category, price, and quantity.
 */
public class Product {

    private String productId;
    private String name;
    private String category;
    private double price;
    private int quantity;

    /**
     * Constructs a new Product with all required fields.
     *
     * @param productId Unique identifier for the product
     * @param name      Display name of the product
     * @param category  Category the product belongs to
     * @param price     Unit price of the product
     * @param quantity  Current stock quantity
     */
    public Product(String productId, String name, String category, double price, int quantity) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
    }

    // -------------------------
    // Getters
    // -------------------------

    /** Returns the product's unique ID. */
    public String getProductId() { return productId; }

    /** Returns the product's display name. */
    public String getName() { return name; }

    /** Returns the category this product belongs to. */
    public String getCategory() { return category; }

    /** Returns the unit price of the product. */
    public double getPrice() { return price; }

    /** Returns the current stock quantity. */
    public int getQuantity() { return quantity; }

    // -------------------------
    // Setters
    // -------------------------

    /** Sets the unit price of the product. */
    public void setPrice(double price) { this.price = price; }

    /** Sets the current stock quantity. */
    public void setQuantity(int quantity) { this.quantity = quantity; }

    /**
     * Returns a formatted string representation of this product.
     *
     * @return Human-readable product summary
     */
    @Override
    public String toString() {
        return String.format("[%s] %s | Category: %s | Price: $%.2f | Qty: %d",
                productId, name, category, price, quantity);
    }
}
