# CS143---Program-6-Coding-with-Gen-AI
Use ClaudeAI (or alternative Gen-AI tool) to write a Java program that uses OOP design and one of the new data structures you learned this quarter in its solution. 

-----Claude Generated Readme Below------

# Store Inventory Tracker

A Java-based store inventory management system built with an **Object-Oriented Programming (OOP)** approach. Demonstrates the practical use of core data structures — `HashMap`, `HashSet`, `Stack`, and `Queue` — to manage products, track changes, and process restock orders.

---

## Table of Contents

- [Overview](#overview)
- [Data Structures Used](#data-structures-used)
- [Project Structure](#project-structure)
- [Class Descriptions](#class-descriptions)
- [Getting Started](#getting-started)
- [Sample Output](#sample-output)
- [Features](#features)
- [Future Improvements](#future-improvements)

---

## Overview

This project simulates a real-world store inventory system where products can be added, updated, removed, and queried. Every change is logged to a transaction history (supporting undo), and low-stock conditions automatically trigger queued restock orders.

---

## Data Structures Used

| Structure | Class Used | Purpose |
|-----------|------------|---------|
| `HashMap<String, Product>` | `Inventory` | O(1) product storage and lookup by ID |
| `HashSet<String>` | `Inventory` | Tracks unique category names without duplicates |
| `Stack` (via `ArrayDeque`) | `TransactionLog` | LIFO history of changes — enables undo |
| `Queue` (via `ArrayDeque`) | `TransactionLog` | FIFO restock order queue — oldest orders processed first |

> **Why these three?** HashMap + HashSet handle the read-heavy lookup and filtering needs of an inventory. Stack + Queue address the write-side concerns: reversibility (undo) and ordered fulfillment (restock).

---

## Project Structure

```
StoreInventory/
├── Product.java           # Data model — stores product attributes
├── Inventory.java         # Core storage — HashMap + HashSet operations
├── TransactionLog.java    # Change history (Stack) + restock orders (Queue)
├── InventoryManager.java  # Facade — coordinates Inventory and TransactionLog
├── Main.java              # Client / test driver — runs all demo scenarios
└── README.md
```

---

## Class Descriptions

### `Product`
Plain data class representing a single inventory item.

**Fields:** `productId`, `name`, `category`, `price`, `quantity`

---

### `Inventory`
Manages the product catalog using a `HashMap` for O(1) access and a `HashSet` for unique category tracking.

**Key Methods:**
- `addProduct(Product)` — adds a product; rejects duplicates
- `removeProduct(String)` — removes by ID; rebuilds category set
- `getProduct(String)` — retrieves a single product by ID
- `updateQuantity(String, int)` — updates stock level
- `getProductsByCategory(String)` — filters by category
- `getLowStockProducts(int)` — finds products at or below a threshold
- `getCategories()` — returns the `HashSet` of current categories

---

### `TransactionLog`
Records all changes in a `Stack` for undo support and maintains a `Queue` of pending restock orders.

**Key Methods:**
- `logTransaction(...)` — pushes a new transaction onto the stack
- `undoLastTransaction()` — pops and returns the most recent transaction
- `enqueueRestockOrder(String, int)` — adds a restock order to the queue
- `processNextRestockOrder()` — polls the next order from the queue

---

### `InventoryManager`
The main controller. Client code should interact with this class only.

**Key Methods:**
- `addProduct(Product)` — add + log
- `removeProduct(String)` — remove + log
- `updateQuantity(String, int)` — update + log + auto-queue restock if qty ≤ 5
- `undoLastAction()` — delegate to TransactionLog and apply to Inventory
- `processNextRestock()` — process one order from the queue

---

### `Main`
Client/test driver. Exercises all features in sequence with clear labeled sections.

---

## Getting Started

### Prerequisites
- Java 11 or higher
- A terminal or IDE (IntelliJ, Eclipse, VS Code + Java Extension)

### Compile

```bash
javac *.java
```

### Run

```bash
java Main
```

---

## Sample Output

```
=======================================================
  1. ADDING PRODUCTS
=======================================================
Added: [P001] Laptop | Category: Electronics | Price: $999.99 | Qty: 15
Added: [P002] Wireless Mouse | Category: Electronics | Price: $29.99 | Qty: 40
...

=======================================================
  8. RESTOCK QUEUE (FIFO)
=======================================================
Pending restock orders: 2

===== Pending Restock Orders (oldest first) =====
RestockOrder -> Product: P003 | Qty: 50
RestockOrder -> Product: P004 | Qty: 50
=================================================
```

---

## Features

- [x] Add, remove, and update products
- [x] O(1) product lookup via `HashMap`
- [x] Unique category tracking via `HashSet`
- [x] Full transaction history with `Stack` (LIFO)
- [x] Undo last inventory action
- [x] Automatic low-stock detection and restock queueing
- [x] FIFO restock order processing via `Queue`
- [x] Filter products by category or low-stock threshold
- [x] Javadoc comments on every method

---

## Future Improvements

- [ ] Persist inventory to a file or database (e.g., SQLite, JSON)
- [ ] Add a `BST` or `TreeMap` for alphabetical/price-sorted product views
- [ ] Build a simple CLI menu for interactive use
- [ ] Add unit tests with JUnit 5
- [ ] Support multi-store / multi-warehouse inventory

---

## License

This project is open source and available under the [MIT License](LICENSE).
