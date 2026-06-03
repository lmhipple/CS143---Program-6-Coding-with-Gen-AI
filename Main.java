import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Main.java - Client / Test Driver for the Store Inventory Tracker
 *
 * Demonstrates how to use the InventoryManager to:
 *   1. Load products from external .txt files (CSV format)
 *   2. Add products to the inventory
 *   3. Look up and display products
 *   4. Update stock quantities (and trigger auto-restock on low stock)
 *   5. Filter by category and low-stock threshold
 *   6. Undo a recent action via the transaction stack
 *   7. Process restock orders via the queue
 *   8. Remove products and view transaction history
 *
 * Data structures exercised:
 *   - HashMap  (Inventory.productMap)  - O(1) product lookups
 *   - HashSet  (Inventory.categories)  - unique category tracking
 *   - Stack    (TransactionLog.stack)  - undo history (LIFO)
 *   - Queue    (TransactionLog.queue)  - restock orders (FIFO)
 *
 * Test data files:
 *   - inventory_data_1.txt  (15 products: Electronics, Furniture, Stationery)
 *   - inventory_data_2.txt  (15 products: Apparel, Health, Kitchen)
 */
public class Main {

    /**
     * Entry point. Loads products from both .txt files, then runs a full
     * series of inventory operations to demonstrate all major features.
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {

        InventoryManager manager = new InventoryManager();

        // -------------------------------------------------------
        // Section 1: Load Products from .txt Files
        // -------------------------------------------------------
        printSectionHeader("1. LOADING PRODUCTS FROM FILES");

        System.out.println("\n-- Loading inventory_data_1.txt --");
        loadFromFile(manager, "inventory_data_1.txt");

        System.out.println("\n-- Loading inventory_data_2.txt --");
        loadFromFile(manager, "inventory_data_2.txt");

        // -------------------------------------------------------
        // Section 2: Display Full Inventory
        // -------------------------------------------------------
        printSectionHeader("2. FULL INVENTORY DISPLAY");
        manager.displayInventory();
        System.out.println("Total products loaded: " + manager.getTotalProductCount());

        // -------------------------------------------------------
        // Section 3: Unique Categories (HashSet)
        // -------------------------------------------------------
        printSectionHeader("3. UNIQUE CATEGORIES (HashSet)");
        Set<String> cats = manager.getCategories();
        System.out.println("Categories found: " + cats);
        System.out.println("Total unique categories: " + cats.size());

        // -------------------------------------------------------
        // Section 4: Look Up a Single Product
        // -------------------------------------------------------
        printSectionHeader("4. PRODUCT LOOKUP BY ID");
        Product found = manager.getProduct("P006");
        System.out.println("Lookup P006: " + (found != null ? found : "Not found"));

        Product notFound = manager.getProduct("P999");
        System.out.println("Lookup P999: " + (notFound != null ? notFound : "Not found"));

        // -------------------------------------------------------
        // Section 5: Filter by Category
        // -------------------------------------------------------
        printSectionHeader("5. FILTER BY CATEGORY: Health");
        List<Product> healthItems = manager.getProductsByCategory("Health");
        healthItems.forEach(System.out::println);

        printSectionHeader("5b. FILTER BY CATEGORY: Furniture");
        List<Product> furniture = manager.getProductsByCategory("Furniture");
        furniture.forEach(System.out::println);

        // -------------------------------------------------------
        // Section 6: Update Quantities (trigger low-stock restock)
        // -------------------------------------------------------
        printSectionHeader("6. QUANTITY UPDATES");

        System.out.println("\nUpdate P005 (27-inch Monitor) to 3 — should trigger restock:");
        manager.updateQuantity("P005", 3);

        System.out.println("\nUpdate P025 (First Aid Kit) — already qty 3, reduce to 1:");
        manager.updateQuantity("P025", 1);

        System.out.println("\nUpdate P028 (French Press) — already qty 4, reduce to 2:");
        manager.updateQuantity("P028", 2);

        System.out.println("\nUpdate P001 (Laptop Pro 15) to 18 (normal update, no restock):");
        manager.updateQuantity("P001", 18);

        // -------------------------------------------------------
        // Section 7: Low-Stock Report
        // -------------------------------------------------------
        printSectionHeader("7. LOW-STOCK REPORT (threshold <= 5)");
        List<Product> lowStock = manager.getLowStockProducts(5);
        if (lowStock.isEmpty()) {
            System.out.println("No low-stock items.");
        } else {
            System.out.println("Items needing attention:");
            lowStock.forEach(System.out::println);
        }

        // -------------------------------------------------------
        // Section 8: Restock Queue (FIFO)
        // -------------------------------------------------------
        printSectionHeader("8. RESTOCK QUEUE (FIFO)");
        System.out.println("Pending restock orders: " + manager.getPendingRestockCount());
        manager.displayRestockQueue();

        System.out.println("\nProcessing restock orders one by one:");
        while (manager.getPendingRestockCount() > 0) {
            manager.processNextRestock();
        }
        manager.processNextRestock(); // Extra call — should report empty queue

        // -------------------------------------------------------
        // Section 9: Transaction History (Stack)
        // -------------------------------------------------------
        printSectionHeader("9. TRANSACTION HISTORY (Stack - most recent first)");
        manager.displayTransactionHistory();

        // -------------------------------------------------------
        // Section 10: Undo Last Action (Stack pop)
        // -------------------------------------------------------
        printSectionHeader("10. UNDO LAST ACTION");
        System.out.println("P028 before undo: " + manager.getProduct("P028"));
        manager.undoLastAction();
        System.out.println("P028 after undo:  " + manager.getProduct("P028"));

        // -------------------------------------------------------
        // Section 11: Remove a Product
        // -------------------------------------------------------
        printSectionHeader("11. REMOVE PRODUCT");
        manager.removeProduct("P011"); // Ballpoint Pens
        manager.removeProduct("P030"); // Dish Drying Rack

        System.out.println("\nCategories after removals: " + manager.getCategories());

        // -------------------------------------------------------
        // Section 12: Final State
        // -------------------------------------------------------
        printSectionHeader("12. FINAL INVENTORY STATE");
        manager.displayInventory();
        System.out.println("Total products remaining: " + manager.getTotalProductCount());
    }

    /**
     * Reads a .txt file of products in CSV format and loads each one into the
     * provided InventoryManager. Lines beginning with '#' are treated as comments
     * and skipped. Blank lines are also ignored.
     *
     * Expected format per line:
     *   productId,name,category,price,quantity
     *
     * Example:
     *   P001,Laptop Pro 15,Electronics,1299.99,20
     *
     * @param manager  The InventoryManager to load products into
     * @param filename Path to the .txt file (relative to working directory)
     */
    private static void loadFromFile(InventoryManager manager, String filename) {
        int loaded = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip blank lines and comment lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Split on comma; expect exactly 5 fields
                String[] parts = line.split(",", 5);
                if (parts.length != 5) {
                    System.out.printf("  [Line %d] Skipping malformed line: %s%n", lineNumber, line);
                    skipped++;
                    continue;
                }

                try {
                    String productId = parts[0].trim();
                    String name      = parts[1].trim();
                    String category  = parts[2].trim();
                    double price     = Double.parseDouble(parts[3].trim());
                    int quantity     = Integer.parseInt(parts[4].trim());

                    Product product = new Product(productId, name, category, price, quantity);
                    manager.addProduct(product);
                    loaded++;

                } catch (NumberFormatException e) {
                    System.out.printf("  [Line %d] Skipping line with invalid number: %s%n", lineNumber, line);
                    skipped++;
                }
            }

            System.out.printf("File load complete — %d loaded, %d skipped.%n", loaded, skipped);

        } catch (IOException e) {
            System.out.println("ERROR: Could not read file '" + filename + "' — " + e.getMessage());
            System.out.println("Make sure the file is in the same directory as Main.java.");
        }
    }

    /**
     * Prints a formatted section header to visually separate test output blocks.
     *
     * @param title The section label to display
     */
    private static void printSectionHeader(String title) {
        System.out.println("\n" + "=".repeat(55));
        System.out.println("  " + title);
        System.out.println("=".repeat(55));
    }
}
