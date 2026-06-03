import java.util.List;
import java.util.Set;

/**
 * InventoryManager serves as the central controller for the store inventory system.
 * It composes an Inventory (for product storage and lookup) and a TransactionLog
 * (for change history and restock orders), coordinating operations between them.
 *
 * This is the primary class that client code should interact with.
 */
public class InventoryManager {

    private final Inventory inventory;
    private final TransactionLog transactionLog;

    /**
     * Constructs an InventoryManager with fresh, empty Inventory and TransactionLog instances.
     */
    public InventoryManager() {
        this.inventory = new Inventory();
        this.transactionLog = new TransactionLog();
    }

    /**
     * Adds a new product to the inventory and logs the transaction.
     *
     * @param product The product to add
     * @return true if added successfully, false if the product ID already exists
     */
    public boolean addProduct(Product product) {
        boolean success = inventory.addProduct(product);
        if (success) {
            transactionLog.logTransaction("ADD", product.getProductId(),
                    "Added product: " + product.getName(), 0);
        }
        return success;
    }

    /**
     * Removes a product from the inventory by ID and logs the transaction.
     * If the product does not exist, no transaction is logged.
     *
     * @param productId The unique ID of the product to remove
     * @return true if removed, false if not found
     */
    public boolean removeProduct(String productId) {
        Product existing = inventory.getProduct(productId);
        if (existing == null) return false;

        int prevQty = existing.getQuantity();
        boolean success = inventory.removeProduct(productId);
        if (success) {
            transactionLog.logTransaction("REMOVE", productId,
                    "Removed product: " + existing.getName(), prevQty);
        }
        return success;
    }

    /**
     * Updates the stock quantity of an existing product and logs the change.
     * If the new quantity is low (5 or below), a restock order is automatically queued.
     *
     * @param productId   The ID of the product to update
     * @param newQuantity The new stock quantity (must be >= 0)
     * @return true if updated, false if the product was not found or quantity is invalid
     */
    public boolean updateQuantity(String productId, int newQuantity) {
        Product existing = inventory.getProduct(productId);
        if (existing == null) return false;

        int prevQty = existing.getQuantity();
        boolean success = inventory.updateQuantity(productId, newQuantity);

        if (success) {
            transactionLog.logTransaction("UPDATE_QTY", productId,
                    String.format("Qty changed from %d to %d", prevQty, newQuantity), prevQty);

            // Automatically queue restock if stock is critically low
            if (newQuantity <= 5) {
                System.out.println("  ⚠ Low stock detected for " + productId + ". Queuing restock order.");
                transactionLog.enqueueRestockOrder(productId, 50);
            }
        }
        return success;
    }

    /**
     * Undoes the last logged inventory transaction by restoring the previous quantity.
     * Only quantity changes (UPDATE_QTY) and removals (REMOVE) are restorable.
     * ADD transactions are noted but require manual correction.
     *
     * @return true if the undo was applied, false if there was nothing to undo
     */
    public boolean undoLastAction() {
        TransactionLog.Transaction last = transactionLog.undoLastTransaction();
        if (last == null) return false;

        String id = last.getProductId();
        String type = last.getType();

        if ("UPDATE_QTY".equals(type)) {
            inventory.updateQuantity(id, last.getPreviousQuantity());
            System.out.println("Undo applied: restored qty to " + last.getPreviousQuantity());
        } else if ("REMOVE".equals(type)) {
            System.out.println("Undo note: Product was removed. Re-add manually if needed: " + id);
        } else if ("ADD".equals(type)) {
            System.out.println("Undo note: Reversing an ADD would require re-removing product: " + id);
        }

        return true;
    }

    /**
     * Processes the next pending restock order from the queue and applies
     * the quantity increase to the matching product in inventory.
     *
     * @return true if an order was processed, false if no orders are pending
     */
    public boolean processNextRestock() {
        TransactionLog.RestockOrder order = transactionLog.processNextRestockOrder();
        if (order == null) return false;

        Product product = inventory.getProduct(order.getProductId());
        if (product != null) {
            int newQty = product.getQuantity() + order.getQuantity();
            inventory.updateQuantity(order.getProductId(), newQty);
            transactionLog.logTransaction("RESTOCK", order.getProductId(),
                    "Restocked +" + order.getQuantity() + " units", product.getQuantity());
            System.out.println("Restocked " + order.getProductId() + " to qty: " + newQty);
        } else {
            System.out.println("Restock skipped: product no longer exists -> " + order.getProductId());
        }
        return true;
    }

    /**
     * Retrieves a product from inventory by its ID.
     *
     * @param productId The product ID to look up
     * @return The matching Product, or null if not found
     */
    public Product getProduct(String productId) {
        return inventory.getProduct(productId);
    }

    /**
     * Returns a list of every product currently in the inventory.
     * Used by name-search routines that need to iterate all products.
     *
     * @return List of all Product objects
     */
    public List<Product> getAllProducts() {
        return inventory.getAllProducts();
    }

    /**
     * Returns all products belonging to a specific category.
     *
     * @param category The category to filter by
     * @return List of matching products
     */
    public List<Product> getProductsByCategory(String category) {
        return inventory.getProductsByCategory(category);
    }

    /**
     * Returns all products whose quantity is at or below the specified threshold.
     *
     * @param threshold The low-stock threshold (inclusive)
     * @return List of low-stock products
     */
    public List<Product> getLowStockProducts(int threshold) {
        return inventory.getLowStockProducts(threshold);
    }

    /**
     * Returns the set of unique product categories currently in the inventory.
     *
     * @return Set of category name strings
     */
    public Set<String> getCategories() {
        return inventory.getCategories();
    }

    /**
     * Prints the full inventory list to standard output.
     */
    public void displayInventory() {
        inventory.displayInventory();
    }

    /**
     * Prints the transaction history stack to standard output.
     */
    public void displayTransactionHistory() {
        transactionLog.displayTransactionHistory();
    }

    /**
     * Prints all pending restock orders to standard output.
     */
    public void displayRestockQueue() {
        transactionLog.displayRestockQueue();
    }

    /**
     * Returns the number of pending restock orders in the queue.
     *
     * @return Pending restock order count
     */
    public int getPendingRestockCount() {
        return transactionLog.getPendingRestockCount();
    }

    /**
     * Returns the total number of products tracked in the inventory.
     *
     * @return Product count
     */
    public int getTotalProductCount() {
        return inventory.getTotalProductCount();
    }
}
