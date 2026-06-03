import java.util.*;

/**
 * Manages inventory transactions using two complementary data structures:
 *
 *   - Stack (Deque as stack): Stores a history of inventory changes for undo support.
 *     LIFO (Last-In-First-Out) — the most recent transaction can be reversed first.
 *
 *   - Queue (Deque as queue): Holds pending restock orders to be processed in order.
 *     FIFO (First-In-First-Out) — oldest orders are fulfilled before newer ones.
 *
 * Each transaction records which product was affected, what changed, and
 * a snapshot of the old value to support undo.
 */
public class TransactionLog {

    /**
     * Represents a single inventory transaction, capturing the before-state
     * so changes can be reversed if needed.
     */
    public static class Transaction {
        private final String type;        // e.g., "ADD", "REMOVE", "UPDATE_QTY"
        private final String productId;
        private final String description;
        private final int previousQuantity;

        /**
         * Constructs a Transaction record.
         *
         * @param type             The action type (ADD, REMOVE, UPDATE_QTY)
         * @param productId        The ID of the affected product
         * @param description      Human-readable description of what changed
         * @param previousQuantity The quantity before the change (used for undo)
         */
        public Transaction(String type, String productId, String description, int previousQuantity) {
            this.type = type;
            this.productId = productId;
            this.description = description;
            this.previousQuantity = previousQuantity;
        }

        /** Returns the transaction type label. */
        public String getType() { return type; }

        /** Returns the ID of the product involved in this transaction. */
        public String getProductId() { return productId; }

        /** Returns the pre-change quantity for undo operations. */
        public int getPreviousQuantity() { return previousQuantity; }

        /**
         * Returns a human-readable string summary of this transaction.
         *
         * @return Formatted transaction string
         */
        @Override
        public String toString() {
            return String.format("[%s] Product: %s | %s (prev qty: %d)",
                    type, productId, description, previousQuantity);
        }
    }

    /**
     * Represents a pending restock order waiting to be processed.
     */
    public static class RestockOrder {
        private final String productId;
        private final int quantity;

        /**
         * Constructs a restock order for a given product.
         *
         * @param productId The ID of the product to restock
         * @param quantity  The number of units to reorder
         */
        public RestockOrder(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        /** Returns the product ID for this restock order. */
        public String getProductId() { return productId; }

        /** Returns the restock quantity. */
        public int getQuantity() { return quantity; }

        /**
         * Returns a summary of the restock order.
         *
         * @return Formatted order string
         */
        @Override
        public String toString() {
            return String.format("RestockOrder -> Product: %s | Qty: %d", productId, quantity);
        }
    }

    // Stack: LIFO history of past transactions (for undo support)
    private final Deque<Transaction> transactionStack;

    // Queue: FIFO queue of pending restock orders (processed in order received)
    private final Queue<RestockOrder> restockQueue;

    /**
     * Constructs an empty TransactionLog with an empty history stack
     * and an empty restock order queue.
     */
    public TransactionLog() {
        this.transactionStack = new ArrayDeque<>();
        this.restockQueue = new ArrayDeque<>();
    }

    /**
     * Records a new transaction and pushes it onto the history stack.
     * This enables undo of the most recent change.
     *
     * @param type             The action type string (e.g., "ADD", "REMOVE")
     * @param productId        The ID of the affected product
     * @param description      A description of the change
     * @param previousQuantity The quantity before the change occurred
     */
    public void logTransaction(String type, String productId, String description, int previousQuantity) {
        Transaction t = new Transaction(type, productId, description, previousQuantity);
        transactionStack.push(t);
        System.out.println("Logged transaction: " + t);
    }

    /**
     * Pops and returns the most recent transaction from the history stack.
     * Returns null and prints a message if the stack is empty.
     *
     * @return The most recent Transaction, or null if history is empty
     */
    public Transaction undoLastTransaction() {
        if (transactionStack.isEmpty()) {
            System.out.println("No transactions to undo.");
            return null;
        }
        Transaction last = transactionStack.pop();
        System.out.println("Undoing transaction: " + last);
        return last;
    }

    /**
     * Peeks at the most recent transaction without removing it from the stack.
     *
     * @return The top Transaction, or null if the stack is empty
     */
    public Transaction peekLastTransaction() {
        return transactionStack.peek();
    }

    /**
     * Adds a new restock order to the end of the processing queue.
     *
     * @param productId The product ID that needs restocking
     * @param quantity  The number of units to reorder
     */
    public void enqueueRestockOrder(String productId, int quantity) {
        RestockOrder order = new RestockOrder(productId, quantity);
        restockQueue.offer(order);
        System.out.println("Queued restock order: " + order);
    }

    /**
     * Processes and removes the next restock order from the front of the queue.
     * Returns null if no orders are pending.
     *
     * @return The next RestockOrder, or null if the queue is empty
     */
    public RestockOrder processNextRestockOrder() {
        if (restockQueue.isEmpty()) {
            System.out.println("No pending restock orders.");
            return null;
        }
        RestockOrder order = restockQueue.poll();
        System.out.println("Processing restock order: " + order);
        return order;
    }

    /**
     * Returns the number of restock orders currently waiting to be processed.
     *
     * @return Pending restock order count
     */
    public int getPendingRestockCount() {
        return restockQueue.size();
    }

    /**
     * Returns the number of transactions currently stored in the history stack.
     *
     * @return Transaction history count
     */
    public int getTransactionHistoryCount() {
        return transactionStack.size();
    }

    /**
     * Prints all transactions currently in the history stack (most recent first).
     */
    public void displayTransactionHistory() {
        if (transactionStack.isEmpty()) {
            System.out.println("No transaction history.");
            return;
        }
        System.out.println("\n===== Transaction History (most recent first) =====");
        for (Transaction t : transactionStack) {
            System.out.println(t);
        }
        System.out.println("===================================================");
    }

    /**
     * Prints all pending restock orders in the queue (oldest first).
     */
    public void displayRestockQueue() {
        if (restockQueue.isEmpty()) {
            System.out.println("No pending restock orders.");
            return;
        }
        System.out.println("\n===== Pending Restock Orders (oldest first) =====");
        for (RestockOrder o : restockQueue) {
            System.out.println(o);
        }
        System.out.println("=================================================");
    }
}
