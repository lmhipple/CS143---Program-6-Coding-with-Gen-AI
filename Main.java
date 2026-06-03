import java.io.*;
import java.util.*;

/**
 * Main.java - Client / Test Driver for the Store Inventory Tracker
 *
 * Program flow:
 *   1.  Prompt the user to choose which inventory data file(s) to load
 *   2.  Load selected file(s) into the InventoryManager
 *   3.  Run automated demo tests (quantity updates, restock, undo, etc.)
 *   4.  Launch an interactive menu where the user can:
 *         [1] Lookup by Product ID      — exact match (e.g. "P003")
 *         [2] Lookup by Product Name    — partial, case-insensitive match
 *         [3] Show all products         — full inventory display
 *         [4] Add a new product         — guided prompts; appended to source .txt
 *         [5] Remove a product          — prompted by ID; removed from live inventory
 *                                         and rewritten out of the source .txt file
 *         [6] Exit
 *
 * File I/O:
 *   - loadFromFile()               : BufferedReader reads CSV lines into InventoryManager
 *   - appendProductToFile()        : BufferedWriter (FileWriter append=true) writes one
 *                                    new CSV line to the end of the chosen source file
 *   - rewriteFileWithoutProduct()  : BufferedReader reads all lines into memory, filters
 *                                    out the removed product's CSV line, then
 *                                    BufferedWriter (overwrite mode) writes the remaining
 *                                    lines back, keeping the .txt file in sync.
 *
 * Scanner usage:
 *   - promptFileSelection()   : chooses which .txt file(s) to load (1, 2, or both)
 *   - runInteractiveMenu()    : main interactive loop (lookup + add + remove + exit)
 *   - promptAddProduct()      : field-by-field guided input for a new product
 *   - promptRemoveProduct()   : prompts for a Product ID, confirms, then removes
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

    // ------------------------------------------------------------------
    // Constants — file names used throughout the program
    // ------------------------------------------------------------------
    private static final String FILE_1 = "inventory_data_1.txt";
    private static final String FILE_2 = "inventory_data_2.txt";

    // Shared Scanner — opened once in main(), closed on exit.
    private static Scanner scanner;

    // Tracks which file was last chosen during file selection so that
    // "Add product" knows where to append when one file is loaded.
    // When both files are loaded the user is asked to choose at add-time.
    private static int loadedFileChoice = -1;

    /**
     * Entry point. Opens the shared Scanner, orchestrates file loading,
     * runs the automated demo sections, then hands control to the
     * interactive menu.
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {

        scanner = new Scanner(System.in);
        InventoryManager manager = new InventoryManager();

        // -----------------------------------------------------------
        // Section 1: File Selection (Scanner prompt)
        // -----------------------------------------------------------
        printSectionHeader("1. SELECT INVENTORY DATA FILE(S) TO LOAD");
        promptFileSelection(manager);

        // -----------------------------------------------------------
        // Section 2: Full Inventory Display
        // -----------------------------------------------------------
        printSectionHeader("2. FULL INVENTORY DISPLAY");
        manager.displayInventory();
        System.out.println("Total products loaded: " + manager.getTotalProductCount());

        // -----------------------------------------------------------
        // Section 3: Unique Categories (HashSet)
        // -----------------------------------------------------------
        printSectionHeader("3. UNIQUE CATEGORIES (HashSet)");
        Set<String> cats = manager.getCategories();
        System.out.println("Categories found: " + cats);
        System.out.println("Total unique categories: " + cats.size());

        // -----------------------------------------------------------
        // Section 4: Quantity Updates — automated demo
        // -----------------------------------------------------------
        printSectionHeader("4. QUANTITY UPDATES (Automated Demo)");
        updateIfPresent(manager, "P005", 3,  "27-inch Monitor   → triggers restock");
        updateIfPresent(manager, "P025", 1,  "First Aid Kit     → triggers restock");
        updateIfPresent(manager, "P028", 2,  "French Press      → triggers restock");
        updateIfPresent(manager, "P001", 18, "Laptop Pro 15     → normal update");

        // -----------------------------------------------------------
        // Section 5: Low-Stock Report
        // -----------------------------------------------------------
        printSectionHeader("5. LOW-STOCK REPORT (threshold <= 5)");
        List<Product> lowStock = manager.getLowStockProducts(5);
        if (lowStock.isEmpty()) {
            System.out.println("No low-stock items.");
        } else {
            System.out.println("Items needing attention:");
            lowStock.forEach(System.out::println);
        }

        // -----------------------------------------------------------
        // Section 6: Restock Queue (FIFO)
        // -----------------------------------------------------------
        printSectionHeader("6. RESTOCK QUEUE (FIFO)");
        System.out.println("Pending restock orders: " + manager.getPendingRestockCount());
        manager.displayRestockQueue();
        System.out.println("\nProcessing all pending restock orders:");
        while (manager.getPendingRestockCount() > 0) {
            manager.processNextRestock();
        }
        manager.processNextRestock(); // Extra call — confirms empty-queue message

        // -----------------------------------------------------------
        // Section 7: Transaction History (Stack)
        // -----------------------------------------------------------
        printSectionHeader("7. TRANSACTION HISTORY (Stack — most recent first)");
        manager.displayTransactionHistory();

        // -----------------------------------------------------------
        // Section 8: Undo Last Action (Stack pop)
        // -----------------------------------------------------------
        printSectionHeader("8. UNDO LAST ACTION");
        Product beforeUndo = manager.getProduct("P028");
        if (beforeUndo != null) {
            System.out.println("Before undo: " + beforeUndo);
            manager.undoLastAction();
            System.out.println("After undo:  " + manager.getProduct("P028"));
        } else {
            System.out.println("(P028 not in loaded inventory — skipping undo demo)");
            manager.undoLastAction();
        }

        // -----------------------------------------------------------
        // Section 9: Remove Products — automated demo
        // -----------------------------------------------------------
        printSectionHeader("9. REMOVE PRODUCTS (Automated Demo)");
        removeIfPresent(manager, "P011"); // Ballpoint Pens   (file 1)
        removeIfPresent(manager, "P030"); // Dish Drying Rack (file 2)
        System.out.println("\nCategories after removals: " + manager.getCategories());

        // -----------------------------------------------------------
        // Section 10: Final Inventory State
        // -----------------------------------------------------------
        printSectionHeader("10. FINAL INVENTORY STATE");
        manager.displayInventory();
        System.out.println("Total products remaining: " + manager.getTotalProductCount());

        // -----------------------------------------------------------
        // Section 11: Interactive Menu (lookup + add product)
        // -----------------------------------------------------------
        printSectionHeader("11. INTERACTIVE MENU");
        runInteractiveMenu(manager);

        scanner.close();
        System.out.println("\nProgram exited. Goodbye!");
    }

    // ==================================================================
    //  SCANNER METHODS
    // ==================================================================

    /**
     * Prompts the user to choose which inventory data file(s) to load.
     * Presents three numbered options and re-prompts on invalid input.
     * Stores the user's choice in {@code loadedFileChoice} so that the
     * add-product feature knows the default target file.
     *
     *   [1] inventory_data_1.txt only
     *   [2] inventory_data_2.txt only
     *   [3] Both files
     *
     * @param manager The InventoryManager to load products into
     */
    private static void promptFileSelection(InventoryManager manager) {
        // Count live data lines in each file so the displayed count reflects
        // any adds or removes that have modified the .txt files since last run.
        int count1 = countProductsInFile(FILE_1);
        int count2 = countProductsInFile(FILE_2);

        System.out.println("\nAvailable inventory data files:");
        System.out.println("  [1] " + FILE_1 + "  (Electronics, Furniture, Stationery — " + count1 + " items)");
        System.out.println("  [2] " + FILE_2 + "  (Apparel, Health, Kitchen           — " + count2 + " items)");
        System.out.println("  [3] Both files (" + (count1 + count2) + " items total)");

        int choice = -1;
        while (choice < 1 || choice > 3) {
            System.out.print("\nEnter your choice (1, 2, or 3): ");
            String raw = scanner.nextLine().trim();
            try {
                choice = Integer.parseInt(raw);
                if (choice < 1 || choice > 3) {
                    System.out.println("  Invalid choice. Please enter 1, 2, or 3.");
                }
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Please enter a number (1, 2, or 3).");
            }
        }

        loadedFileChoice = choice;      // remember for add-product targeting
        System.out.println();

        if (choice == 1 || choice == 3) {
            System.out.println("-- Loading " + FILE_1 + " --");
            loadFromFile(manager, FILE_1);
        }
        if (choice == 2 || choice == 3) {
            System.out.println("-- Loading " + FILE_2 + " --");
            loadFromFile(manager, FILE_2);
        }
    }

    /**
     * Runs the main interactive menu loop. Available options:
     *
     *   [1] Lookup by Product ID      — exact match, case-insensitive
     *   [2] Lookup by Product Name    — partial, case-insensitive match
     *   [3] Show all products         — full inventory list
     *   [4] Add a new product         — guided input; persisted to .txt file
     *   [5] Remove a product          — prompted by ID; removed from inventory
     *                                   and rewritten out of the source .txt file
     *   [6] Exit
     *
     * Loops until the user selects 6, "exit", or "quit".
     *
     * @param manager The InventoryManager to query / update
     */
    private static void runInteractiveMenu(InventoryManager manager) {
        System.out.println("\nInteractive menu ready. Use the options below.\n");

        boolean running = true;

        while (running) {
            printMainMenu();
            System.out.print("Your choice: ");
            String input = scanner.nextLine().trim();

            switch (input) {

                // ---- [1] Lookup by exact Product ID ----
                case "1":
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

                // ---- [2] Lookup by partial product name ----
                case "2":
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

                // ---- [3] Show all products ----
                case "3":
                    manager.displayInventory();
                    System.out.println("Total products: " + manager.getTotalProductCount());
                    break;

                // ---- [4] Add a new product ----
                case "4":
                    promptAddProduct(manager);
                    break;

                // ---- [5] Remove a product ----
                case "5":
                    promptRemoveProduct(manager);
                    break;

                // ---- [6] Exit ----
                case "6":
                case "exit":
                case "quit":
                    running = false;
                    break;

                default:
                    System.out.println("  Unrecognized option. Please enter 1–6.");
            }

            if (running) System.out.println();
        }
    }

    /**
     * Guides the user through entering every field needed to create a new
     * Product, adds it to the live InventoryManager, then appends one CSV
     * line to the target .txt file using a BufferedWriter (append mode).
     *
     * Field prompts and validation:
     *   - Product ID   : must not be blank; rejected if ID already exists
     *   - Name         : must not be blank
     *   - Category     : must not be blank
     *   - Price        : must be a non-negative decimal number
     *   - Quantity     : must be a non-negative integer
     *
     * Target file selection:
     *   - If only one file was loaded at startup, that file is used automatically.
     *   - If both files were loaded, the user is asked which file to append to.
     *
     * On success the new product is visible in both the live inventory and
     * the chosen .txt file immediately.
     *
     * @param manager The InventoryManager to add the new product into
     */
    private static void promptAddProduct(InventoryManager manager) {
        System.out.println("\n  --- Add New Product ---");

        // ---- Product ID ----
        String productId = "";
        while (productId.isEmpty()) {
            System.out.print("  Product ID (e.g. P031): ");
            productId = scanner.nextLine().trim().toUpperCase();
            if (productId.isEmpty()) {
                System.out.println("  Product ID cannot be blank. Please try again.");
            } else if (manager.getProduct(productId) != null) {
                System.out.println("  '" + productId + "' already exists in inventory. Use a unique ID.");
                productId = ""; // force re-prompt
            }
        }

        // ---- Name ----
        String name = "";
        while (name.isEmpty()) {
            System.out.print("  Product name: ");
            name = scanner.nextLine().trim();
            if (name.isEmpty()) System.out.println("  Name cannot be blank. Please try again.");
        }

        // ---- Category ----
        String category = "";
        while (category.isEmpty()) {
            System.out.print("  Category: ");
            category = scanner.nextLine().trim();
            if (category.isEmpty()) System.out.println("  Category cannot be blank. Please try again.");
        }

        // ---- Price ----
        double price = -1.0;
        while (price < 0) {
            System.out.print("  Price (e.g. 19.99): ");
            String raw = scanner.nextLine().trim();
            try {
                price = Double.parseDouble(raw);
                if (price < 0) System.out.println("  Price cannot be negative. Please try again.");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid price — enter a numeric value (e.g. 19.99).");
            }
        }

        // ---- Quantity ----
        int quantity = -1;
        while (quantity < 0) {
            System.out.print("  Quantity (whole number): ");
            String raw = scanner.nextLine().trim();
            try {
                quantity = Integer.parseInt(raw);
                if (quantity < 0) System.out.println("  Quantity cannot be negative. Please try again.");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid quantity — enter a whole number (e.g. 25).");
            }
        }

        // ---- Confirm before saving ----
        Product newProduct = new Product(productId, name, category, price, quantity);
        System.out.println("\n  New product to be added:");
        System.out.println("  " + newProduct);
        System.out.print("\n  Confirm? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("y") && !confirm.equals("yes")) {
            System.out.println("  Add product cancelled.");
            return;
        }

        // ---- Determine target .txt file ----
        String targetFile = resolveTargetFile();

        // ---- Add to live inventory ----
        boolean added = manager.addProduct(newProduct);
        if (!added) {
            // Should not happen (we checked above), but guard anyway
            System.out.println("  ERROR: Product could not be added to inventory.");
            return;
        }

        // ---- Persist to .txt file via BufferedWriter (append mode) ----
        appendProductToFile(newProduct, targetFile);
    }

    /**
     * Determines which .txt file a newly added product should be appended to.
     *
     * Rules:
     *   - If only file 1 was loaded  → target is FILE_1 automatically.
     *   - If only file 2 was loaded  → target is FILE_2 automatically.
     *   - If both files were loaded  → prompt the user to choose (1 or 2),
     *     re-prompting on invalid input.
     *
     * @return The filename string of the chosen target file
     */
    private static String resolveTargetFile() {
        if (loadedFileChoice == 1) {
            System.out.println("  Appending to: " + FILE_1);
            return FILE_1;
        }
        if (loadedFileChoice == 2) {
            System.out.println("  Appending to: " + FILE_2);
            return FILE_2;
        }

        // Both files loaded — ask user which to append to
        System.out.println("\n  Both files are loaded. Which file should this product be saved to?");
        System.out.println("    [1] " + FILE_1);
        System.out.println("    [2] " + FILE_2);

        int fileChoice = -1;
        while (fileChoice < 1 || fileChoice > 2) {
            System.out.print("  Your choice (1 or 2): ");
            String raw = scanner.nextLine().trim();
            try {
                fileChoice = Integer.parseInt(raw);
                if (fileChoice < 1 || fileChoice > 2) {
                    System.out.println("  Please enter 1 or 2.");
                }
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Please enter 1 or 2.");
            }
        }

        return fileChoice == 1 ? FILE_1 : FILE_2;
    }

    /**
     * Appends one product as a CSV line to the end of the specified .txt file
     * using a BufferedWriter opened in append mode (FileWriter(filename, true)).
     *
     * The written line follows the same format used by loadFromFile():
     *   productId,name,category,price,quantity
     *
     * A newline is written before the data line only if the file does not
     * already end with one, preventing blank lines or merged lines in the file.
     * A trailing newline is always added after the data line so subsequent
     * appends each land on their own line.
     *
     * @param product  The Product whose data will be written
     * @param filename Path to the target .txt file
     */
    private static void appendProductToFile(Product product, String filename) {
        // Build the CSV line to append
        String csvLine = String.format("%s,%s,%s,%.2f,%d",
                product.getProductId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getQuantity());

        // Open with append=true so existing file contents are preserved
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            writer.newLine();           // blank separator line before new entry
            writer.write(csvLine);      // write the CSV data line
            writer.newLine();           // trailing newline after the entry

            System.out.println("  Saved to " + filename + ": " + csvLine);
            System.out.println("  Product successfully added to inventory and file.");

        } catch (IOException e) {
            System.out.println("  ERROR: Could not write to '" + filename + "' — " + e.getMessage());
            System.out.println("  The product was added to live inventory but NOT saved to disk.");
        }
    }

    /**
     * Guides the user through removing an existing product from the live
     * inventory and its source .txt file.
     *
     * Steps:
     *   1. Prompt for a Product ID; re-prompt if blank or not found.
     *   2. Display the matching product so the user can confirm the right item.
     *   3. Ask for y/n confirmation before committing.
     *   4. On confirmation:
     *        a. Determine which file the product belongs to via
     *           {@link #resolveSourceFile(String)} — scans each loaded .txt
     *           for a line that starts with the given product ID.
     *        b. Remove the product from the live InventoryManager.
     *        c. Call {@link #rewriteFileWithoutProduct(String, String)} to
     *           filter the matching CSV line out of the source file.
     *
     * @param manager The InventoryManager to remove the product from
     */
    private static void promptRemoveProduct(InventoryManager manager) {
        System.out.println("\n  --- Remove Product ---");

        // ---- Product ID ----
        String productId = "";
        Product target = null;
        while (target == null) {
            System.out.print("  Enter Product ID to remove (e.g. P003): ");
            productId = scanner.nextLine().trim().toUpperCase();
            if (productId.isEmpty()) {
                System.out.println("  Product ID cannot be blank. Please try again.");
                continue;
            }
            target = manager.getProduct(productId);
            if (target == null) {
                System.out.println("  '" + productId + "' not found in inventory. Please try again.");
            }
        }

        // ---- Show product and confirm ----
        System.out.println("\n  Product to be removed:");
        System.out.println("  " + target);
        System.out.print("\n  Confirm removal? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("y") && !confirm.equals("yes")) {
            System.out.println("  Remove product cancelled.");
            return;
        }

        // ---- Locate the source file that contains this product ID ----
        String sourceFile = resolveSourceFile(productId);

        // ---- Remove from live inventory ----
        boolean removed = manager.removeProduct(productId);
        if (!removed) {
            System.out.println("  ERROR: Could not remove product from inventory.");
            return;
        }

        // ---- Rewrite the source file without that product's line ----
        if (sourceFile != null) {
            rewriteFileWithoutProduct(productId, sourceFile);
        } else {
            System.out.println("  Note: product removed from live inventory, but its source "
                    + "file could not be determined — .txt file was NOT modified.");
        }
    }

    /**
     * Scans each loaded .txt file to find which one contains a CSV line
     * that starts with the given product ID. Returns the filename of the
     * first file where a match is found, or {@code null} if no match exists
     * (e.g. the product was added only in memory during this session without
     * being persisted, which should not happen through normal program flow).
     *
     * Only files that were actually loaded at startup are searched, based
     * on the value of {@code loadedFileChoice}.
     *
     * @param productId The product ID to search for (e.g. "P003")
     * @return The filename containing the product line, or null if not found
     */
    private static String resolveSourceFile(String productId) {
        List<String> filesToSearch = new ArrayList<>();
        if (loadedFileChoice == 1 || loadedFileChoice == 3) filesToSearch.add(FILE_1);
        if (loadedFileChoice == 2 || loadedFileChoice == 3) filesToSearch.add(FILE_2);

        // The CSV prefix we are looking for: "P003," (ID + comma)
        String prefix = productId + ",";

        for (String filename : filesToSearch) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith(prefix)) {
                        return filename;   // found in this file
                    }
                }
            } catch (IOException e) {
                System.out.println("  WARNING: Could not read '" + filename
                        + "' while searching for product: " + e.getMessage());
            }
        }
        return null; // not found in any loaded file
    }

    /**
     * Rewrites a .txt file in place, omitting any CSV line whose first field
     * matches the given product ID. All comment lines, blank lines, and every
     * other data line are preserved exactly as they appear in the original file.
     *
     * Strategy (read-then-overwrite):
     *   1. Open the file with a BufferedReader and read every line into a List.
     *   2. Close the reader (releasing the file handle).
     *   3. Open the same file with a BufferedWriter in overwrite mode
     *      (FileWriter(filename, false)) and write back every line except
     *      the one that starts with {@code productId + ","}.
     *
     * This approach is safe for the small file sizes used here. For very large
     * files a temp-file swap pattern would be preferable.
     *
     * @param productId The ID of the product whose CSV line should be omitted
     * @param filename  Path to the .txt file to rewrite
     */
    private static void rewriteFileWithoutProduct(String productId, String filename) {
        String prefix       = productId + ",";
        List<String> lines  = new ArrayList<>();
        int removedCount    = 0;

        // ---- Step 1: Read all lines into memory ----
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.out.println("  ERROR: Could not read '" + filename + "' for rewrite — "
                    + e.getMessage());
            System.out.println("  Product removed from live inventory but .txt was NOT modified.");
            return;
        }

        // ---- Step 2: Write lines back, skipping the removed product's line ----
        // FileWriter(filename, false) opens in overwrite (truncate) mode.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, false))) {
            for (String line : lines) {
                // Skip the data line that belongs to the removed product.
                // Trim before checking so leading whitespace does not cause a miss.
                if (line.trim().startsWith(prefix)) {
                    removedCount++;
                    continue;  // omit this line from the rewritten file
                }
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("  ERROR: Could not write back to '" + filename + "' — "
                    + e.getMessage());
            System.out.println("  Product removed from live inventory but .txt may be incomplete.");
            return;
        }

        if (removedCount > 0) {
            System.out.println("  Removed " + removedCount + " line(s) from " + filename + ".");
            System.out.println("  Product successfully removed from inventory and file.");
        } else {
            // Line was not found — file unchanged; warn the user.
            System.out.println("  WARNING: No matching line found in " + filename
                    + " for ID '" + productId + "'. File was not modified.");
        }
    }

    // ==================================================================
    //  HELPER / UTILITY METHODS
    // ==================================================================

    /**
     * Searches all loaded products for those whose name contains the given
     * query string (case-insensitive partial match).
     *
     * @param manager The InventoryManager to search
     * @param query   Lowercase search term to match against product names
     * @return        List of products whose name contains the query
     */
    private static List<Product> searchByName(InventoryManager manager, String query) {
        List<Product> results = new ArrayList<>();
        for (Product p : manager.getAllProducts()) {
            if (p.getName().toLowerCase().contains(query)) {
                results.add(p);
            }
        }
        return results;
    }

    /**
     * Updates the quantity of a product only if it is present in the inventory.
     * Prints a skip notice when the ID is absent (e.g. only one file was loaded).
     *
     * @param manager     The InventoryManager to update
     * @param productId   The ID of the product to update
     * @param newQty      The replacement quantity value
     * @param description Short label printed alongside the update for readability
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
     * Removes a product only if it is present in the inventory.
     * Prints a skip notice when the ID is absent.
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
     * Counts the number of valid product data lines in a .txt file without
     * loading any products into the InventoryManager. Used by
     * {@link #promptFileSelection} to display a live, accurate item count
     * for each file — reflecting any adds or removes from prior sessions.
     *
     * A line is counted only if it is non-blank, does not start with '#',
     * and contains exactly 5 comma-separated fields (the same criteria used
     * by loadFromFile). Comment lines, blank lines, and malformed lines are
     * not counted. Returns 0 if the file cannot be read.
     *
     * @param filename Path to the .txt file to inspect
     * @return Number of valid product lines found in the file
     */
    private static int countProductsInFile(String filename) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.split(",", 5).length == 5) count++;
            }
        } catch (IOException e) {
            // File unreadable — return 0 so the menu still displays cleanly.
            System.out.println("  WARNING: Could not read '" + filename
                    + "' for item count: " + e.getMessage());
        }
        return count;
    }

    /**
     * Reads a .txt file in CSV format and loads each valid product line into
     * the provided InventoryManager. Comment lines (starting with '#') and
     * blank lines are silently skipped. Malformed or unparseable lines print
     * a warning and are counted as skipped.
     *
     * Expected line format:
     *   productId,name,category,price,quantity
     *
     * Example:
     *   P001,Laptop Pro 15,Electronics,1299.99,20
     *
     * @param manager  The InventoryManager to load products into
     * @param filename Path to the .txt file (relative to the working directory)
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
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Expect exactly 5 comma-separated fields
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

    // ==================================================================
    //  DISPLAY HELPERS
    // ==================================================================

    /**
     * Prints the interactive main menu options to standard output.
     */
    private static void printMainMenu() {
        System.out.println("---------------------------------------------");
        System.out.println("  [1] Lookup by Product ID   (exact, e.g. P003)");
        System.out.println("  [2] Lookup by Product Name (partial match)");
        System.out.println("  [3] Show all products");
        System.out.println("  [4] Add a new product");
        System.out.println("  [5] Remove a product");
        System.out.println("  [6] Exit");
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
