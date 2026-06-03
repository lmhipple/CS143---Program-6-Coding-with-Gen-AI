import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the store's product inventory using a HashMap for fast lookups
 * and a HashSet to track unique product categories.
 *
 * Data Structures Used:
 *   - HashMap<String, Product>: Maps productId -> Product for O(1) access
 *   - HashSet<String>:          Tracks all unique category names in the inventory
 */
public class Inventory {

    // Maps productId -> Product for O(1) add, remove, and lookup
    private Map<String, Product> productMap;

    // Tracks unique category names present in the inventory
    private Set<String> categories;

    /**
     * Constructs an empty Inventory with an initialized map and category set.
     */
    public Inventory() {
        this.productMap = new HashMap<>();
        this.categories = new HashSet<>();
    }

    /**
     * Adds a new product to the inventory.
     * If a product with the same ID already exists, it will NOT be overwritten.
     *
     * @param product The product to add
     * @return true if the product was added successfully, false if the ID already exists
     */
    public boolean addProduct(Product product) {
        if (productMap.containsKey(product.getProductId())) {
            System.out.println("Product ID already exists: " + product.getProductId());
            return false;
        }
        productMap.put(product.getProductId(), product);
        categories.add(product.getCategory());
        System.out.println("Added: " + product);
        return true;
    }

    /**
     * Removes a product from the inventory by its ID.
     * Also updates the category set if no other products share the same category.
     *
     * @param productId The unique ID of the product to remove
     * @return true if the product was found and removed, false otherwise
     */
    public boolean removeProduct(String productId) {
        Product removed = productMap.remove(productId);
        if (removed == null) {
            System.out.println("Product not found: " + productId);
            return false;
        }
        // Rebuild category set from remaining products
        rebuildCategories();
        System.out.println("Removed: " + removed);
        return true;
    }

    /**
     * Retrieves a product by its ID.
     *
     * @param productId The unique ID of the product
     * @return The matching Product, or null if not found
     */
    public Product getProduct(String productId) {
        return productMap.get(productId);
    }

    /**
     * Updates the quantity of an existing product.
     *
     * @param productId   The ID of the product to update
     * @param newQuantity The new quantity value (must be >= 0)
     * @return true if updated successfully, false if the product was not found
     */
    public boolean updateQuantity(String productId, int newQuantity) {
        if (newQuantity < 0) {
            System.out.println("Quantity cannot be negative.");
            return false;
        }
        Product product = productMap.get(productId);
        if (product == null) {
            System.out.println("Product not found: " + productId);
            return false;
        }
        product.setQuantity(newQuantity);
        System.out.println("Updated quantity for " + productId + " -> " + newQuantity);
        return true;
    }

    /**
     * Returns a list of all products in the inventory.
     *
     * @return List of all Product objects
     */
    public List<Product> getAllProducts() {
        return new ArrayList<>(productMap.values());
    }

    /**
     * Returns all products that belong to a specific category.
     *
     * @param category The category name to filter by
     * @return List of products matching the given category
     */
    public List<Product> getProductsByCategory(String category) {
        List<Product> result = new ArrayList<>();
        for (Product p : productMap.values()) {
            if (p.getCategory().equalsIgnoreCase(category)) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Returns all products where stock quantity falls at or below a given threshold.
     * Useful for identifying items that need restocking.
     *
     * @param threshold The quantity threshold (inclusive)
     * @return List of low-stock products
     */
    public List<Product> getLowStockProducts(int threshold) {
        List<Product> result = new ArrayList<>();
        for (Product p : productMap.values()) {
            if (p.getQuantity() <= threshold) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Returns the set of unique categories currently in the inventory.
     *
     * @return A Set of category name strings
     */
    public Set<String> getCategories() {
        return new HashSet<>(categories);
    }

    /**
     * Returns the total number of products in the inventory.
     *
     * @return Product count
     */
    public int getTotalProductCount() {
        return productMap.size();
    }

    /**
     * Prints all products currently in the inventory to standard output.
     */
    public void displayInventory() {
        if (productMap.isEmpty()) {
            System.out.println("Inventory is empty.");
            return;
        }
        System.out.println("\n===== Current Inventory =====");
        for (Product p : productMap.values()) {
            System.out.println(p);
        }
        System.out.println("=============================");
    }

    /**
     * Rebuilds the category set by scanning all remaining products.
     * Called internally after a product removal.
     */
    private void rebuildCategories() {
        categories.clear();
        for (Product p : productMap.values()) {
            categories.add(p.getCategory());
        }
    }
}
