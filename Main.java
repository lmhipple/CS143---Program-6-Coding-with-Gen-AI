import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Main.java - Client / Test Driver for the Store Inventory Tracker
 *
 * Program flow:
 *   1. Prompt the user to choose which inventory data file(s) to load
 *   2. Load selected file(s) into the InventoryManager
 *   3. Run automated demo tests (quantity updates, restock, undo, etc.)
 *   4. Launch an interactive lookup menu where the user can search
 *      by Product ID (e.g. "P003") or by partial product name (e.g. "chair")
 *      The menu loops until the user chooses to exit.
 *
 * Scanner usage:
 *   - promptFileSelection()  : asks which .txt file(s) to load (1, 2, or both)
 *   - runInteractiveLookup() : repeatedly prompts for ID or name searches
 *
 * Data structures exercised:
 *   - HashMap  (Inventory.productMap)  — O(1) product lookup by ID
 *   - HashSet  (Inventory.categories)  — unique category names
 *   - Stack    (TransactionLog.stack)  — undo history (LIFO)
 *   - Queue    (TransactionLog.queue)  — restock orders (FIFO)
 *
 * Test data files:
 *   - inventory_data_1.txt  (15 products: Electronics, Furniture, Stationery)
 *   - inventory_data_2.txt  (15 products: Apparel, Health, Kitchen)
 */
public class Main {

    // Shared Scanner — opened once in main(), passed to interactive methods,
    // and closed at program exit to avoid resource leaks.
    private static Scanner scanner;

    /**
     * Entry point. Opens the shared Scanner, orchestrates file loading,
     * runs the automated demo, then hands control to the interactive menu.
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {

        scanner = new Scanner(System.in);
        InventoryManager manager = new InventoryManager();

        // -------------------------------------------------------
        // Section 1: File Selection (Scanner prompt)
        // -------------------------------------------------------
        printSectionHeader("1. SELECT INVENTORY DATA FILE(S) TO LOAD");
        promptFileSelection(manager);

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
        // Section 4: Update Quantities (trigger low-stock restock)
        // -------------------------------------------------------
        printSectionHeader("4. QUANTITY UPDATES (Automated Demo)");

        updateIfPresent(manager, "P005", 3,  "27-inch Monitor   → triggers restock");
        updateIfPresent(manager, "P025", 1,  "First Aid Kit     → triggers restock");
        updateIfPresent(manager, "P028", 2,  "French Press      → triggers restock");
        updateIfPresent(manager, "P001", 18, "Laptop Pro 15     → normal update");

        // -------------------------------------------------------
        // Section 5: Low-Stock Report
        // -------------------------------------------------------
        printSectionHeader("5. LOW-STOCK REPORT (threshold <= 5)");
        List<Product> lowStock = manager.getLowStockProducts(5);
        if (lowStock.isEmpty()) {
            System.out.println("No low-stock items.");
        } else {
            System.out.println("Items needing attention:");
            lowStock.forEach(System.out::println);
        }

        // -------------------------------------------------------
        // Section 6: Restock Queue (FIFO)
        // -------------------------------------------------------
        printSectionHeader("6. RESTOCK QUEUE (FIFO)");
        System.out.println("Pending restock orders: " + manager.getPendingRestockCount());
        manager.displayRestockQueue();

        System.out.println("\nProcessing all pending restock orders:");
        while (manager.getPendingRestockCount() > 0) {
            manager.processNextRestock();
        }
        manager.processNextRestock(); // Extra call to confirm empty queue message

        // -------------------------------------------------------
        // Section 7: Transaction History (Stack)
        // -------------------------------------------------------
        printSectionHeader("7. TRANSACTION HISTORY (Stack — most recent first)");
        manager.displayTransactionHistory();

        // -------------------------------------------------------
        // Section 8: Undo Last Action (Stack pop)
        // -------------------------------------------------------
        printSectionHeader("8. UNDO LAST ACTION");
        String undoTarget = "P028";
        Product beforeUndo = manager.getProduct(undoTarget);
        if (beforeUndo != null) {
            System.out.println("Before undo: " + beforeUndo);
            manager.undoLastAction();
            System.out.println("After undo:  " + manager.getProduct(undoTarget));
        } else {
            System.out.println("(P028 not in loaded inventory — skipping undo demo)");
            manager.undoLastAction();
        }

        // -------------------------------------------------------
        // Section 9: Remove Products
        // -------------------------------------------------------
        printSectionHeader("9. REMOVE PRODUCTS (Automated Demo)");
        removeIfPresent(manager, "P011"); // Ballpoint Pens (file 1)
        removeIfPresent(manager, "P030"); // Dish Drying Rack (file 2)
        System.out.println("\nCategories after removals: " + manager.getCategories());

        // -------------------------------------------------------
        // Section 10: Final Inventory State
        // -------------------------------------------------------
        printSectionHeader("10. FINAL INVENTORY STATE");
        manager.displayInventory();
        System.out.println("Total products remaining: " + manager.getTotalProductCount());

        // -------------------------------------------------------
        // Section 11: Interactive Product Lookup (Scanner menu)
        // -------------------------------------------------------
        printSectionHeader("11. INTERACTIVE PRODUCT LOOKUP");
        runInteractiveLookup(manager);

        scanner.close();
        System.out.println("\nProgram exited. Goodbye!");
    }

    // ================================================================
    //  SCANNER METHODS
    // ================================================================

    /**
     * Prompts the user to choose which inventory data file(s) to load.
     * Presents three numbered options:
     *   1 — inventory_data_1.txt only
     *   2 — inventory_data_2.txt only
     *   3 — Both files
     * Re-prompts on invalid input until a valid choice (1–3) is entered.
     *
     * @param manager The InventoryManager to load products into
     */
    private static void promptFileSelection(InventoryManager manager) {
        System.out.println("\nAvailable inventory data files:");
        System.out.println("  [1] inventory_data_1.txt  (Electronics, Furniture, Stationery — 15 items)");
        System.out.println("  [2] inventory_data_2.txt  (Apparel, Health, Kitchen           — 15 items)");
        System.out.println("  [3] Both files");

        int choice = -1;
        while (choice < 1 || choice > 3) {
            System.out.print("\nEnter your choice (1, 2, or 3): ");
            String input = scanner.nextLine().trim();
            try {
                choice = Integer.parseInt(input);
                if (choice < 1 || choice > 3) {
                    System.out.println("  Invalid choice. Please enter 1, 2, or 3.");
                }
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Please enter a number (1, 2, or 3).");
            }
        }

        System.out.println();
        if (choice == 1 || choice == 3) {
            System.out.println("-- Loading inventory_data_1.txt --");
            loadFromFile(manager, "inventory_data_1.txt");
        }
        if (choice == 2 || choice == 3) {
            System.out.println("-- Loading inventory_data_2.txt --");
            loadFromFile(manager, "inventory_data_2.txt");
        }
    }

    /**
     * Runs an interactive loop that lets the user search loaded inventory
     * by Product ID or by product name (partial, case-insensitive match).
     *
     * Menu options shown each iteration:
     *   [1] Lookup by Product ID   — exact match (e.g. "P003")
     *   [2] Lookup by Product Name — partial match (e.g. "chair", "pro")
     *   [3] Show all products      — prints full inventory
     *   [4] Exit                   — breaks the loop
     *
     * Loops until the user selects option 4 or types "exit" / "quit".
     *
     * @param manager The InventoryManager to search against
     */
    private static void runInteractiveLookup(InventoryManager manager) {
        System.out.println("\nYou can now search the loaded inventory.");
        System.out.println("Options: lookup by Product ID, by product name, or view all.\n");

        boolean running = true;

        while (running) {
            printLookupMenu();
            System.out.print("Your choice: ");
            String input = scanner.nextLine().trim();

            switch (input) {

                case "1":
                    // ---- Lookup by exact Product ID ----
                    System.out.print("  Enter Product ID (e.g. P003): ");
                    String idInput = scanner.nextLine().trim().toUpperCase();

                    if (idInput.isEmpty()) {
                        System.out.println("  No input entered.");
                        break;
                    }

                    Product byId = manager.getProduct(idInput);
                    if (byId != null) {
                        System.out.println("\n  Found:");
                        System.out.println("  " + byId);
                    } else {
                        System.out.println("  No product found with ID: " + idInput);
                    }
                    break;

                case "2":
                    // ---- Lookup by partial product name ----
                    System.out.print("  Enter product name (or partial name): ");
                    String nameInput = scanner.nextLine().trim().toLowerCase();

                    if (nameInput.isEmpty()) {
                        System.out.println("  No input entered.");
                        break;
                    }

                    List<Product> nameMatches = searchByName(manager, nameInput);
                    if (nameMatches.isEmpty()) {
                        System.out.println("  No products found matching: \"" + nameInput + "\"");
                    } else {
                        System.out.println("\n  Found " + nameMatches.size()
                                + " match(es) for \"" + nameInput + "\":");
                        nameMatches.forEach(p -> System.out.println("  " + p));
                    }
                    break;

                case "3":
                    // ---- Show full inventory ----
                    manager.displayInventory();
                    System.out.println("Total products: " + manager.getTotalProductCount());
                    break;

                case "4":
                case "exit":
                case "quit":
                    // ---- Exit the lookup loop ----
                    running = false;
                    break;

                default:
                    System.out.println("  Unrecognized option. Please enter 1, 2, 3, or 4.");
            }

            if (running) System.out.println(); // blank line between iterations
        }
    }

    // ================================================================
    //  HELPER / UTILITY METHODS
    // ================================================================

    /**
     * Searches all loaded products for those whose name contains the given
     * query string (case-insensitive, partial match).
     *
     * @param manager   The InventoryManager containing all products
     * @param query     Lowercase search term to match against product names
     * @return          List of products whose name contains the query
     */
    private static List<Product> searchByName(InventoryManager manager, String query) {
        List<Product> results = new java.util.ArrayList<>();
        for (Product p : manager.getAllProducts()) {
            if (p.getName().toLowerCase().contains(query)) {
                results.add(p);
            }
        }
        return results;
    }

    /**
     * Updates the quantity of a product only if it exists in the inventory.
     * Prints a skip notice if the product ID is not found (useful when only
     * one of the two data files was loaded).
     *
     * @param manager     The InventoryManager to update
     * @param productId   The ID of the product to update
     * @param newQty      The new quantity to set
     * @param description A short label printed alongside the update for readability
     */
    private static void updateIfPresent(InventoryManager manager, String productId,
                                         int newQty, String description) {
        if (manager.getProduct(productId) != null) {
            System.out.println("\n" + description);
            manager.updateQuantity(productId, newQty);
        } else {
            System.out.println("  (Skipping " + productId + " — not in loaded file)");
        }
    }

    /**
     * Removes a product only if it exists in the inventory.
     * Prints a skip notice if the product ID is not found.
     *
     * @param manager   The InventoryManager to remove from
     * @param productId The ID of the product to remove
     */
    private static void removeIfPresent(InventoryManager manager, String productId) {
        if (manager.getProduct(productId) != null) {
            manager.removeProduct(productId);
        } else {
            System.out.println("  (Skipping removal of " + productId + " — not in loaded file)");
        }
    }

    /**
     * Reads a .txt file of products in CSV format and loads each one into the
     * provided InventoryManager. Lines beginning with '#' are treated as comments
     * and skipped. Blank lines are also ignored.
     *
     * Expected line format:
     *   productId,name,category,price,quantity
     *
     * Example:
     *   P001,Laptop Pro 15,Electronics,1299.99,20
     *
     * @param manager  The InventoryManager to load products into
     * @param filename Path to the .txt file (relative to working directory)
     */
    private static void loadFromFile(InventoryManager manager, String filename) {
        int loaded  = 0;
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
                    int    quantity  = Integer.parseInt(parts[4].trim());

                    manager.addProduct(new Product(productId, name, category, price, quantity));
                    loaded++;

                } catch (NumberFormatException e) {
                    System.out.printf("  [Line %d] Skipping line with invalid number: %s%n",
                            lineNumber, line);
                    skipped++;
                }
            }

            System.out.printf("  File load complete — %d loaded, %d skipped.%n", loaded, skipped);

        } catch (IOException e) {
            System.out.println("  ERROR: Could not read '" + filename + "' — " + e.getMessage());
            System.out.println("  Ensure the file is in the same directory as Main.java.");
        }
    }

    /**
     * Prints the interactive lookup sub-menu options to standard output.
     */
    private static void printLookupMenu() {
        System.out.println("---------------------------------------------");
        System.out.println("  [1] Lookup by Product ID   (exact, e.g. P003)");
        System.out.println("  [2] Lookup by Product Name (partial match)");
        System.out.println("  [3] Show all products");
        System.out.println("  [4] Exit");
        System.out.println("---------------------------------------------");
    }

    /**
     * Prints a formatted section header to visually separate output blocks.
     *
     * @param title The section label to display
     */
    private static void printSectionHeader(String title) {
        System.out.println("\n" + "=".repeat(55));
        System.out.println("  " + title);
        System.out.println("=".repeat(55));
    }
}
