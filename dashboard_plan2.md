1. **Room Queries**
   - In `TransactionDao`, add:
     - `@Query("SELECT SUM(totalAmount) FROM transactions")` -> `suspend fun getTotalRevenue(): Double?`
     - `@Query("SELECT COUNT(id) FROM transactions")` -> `suspend fun getTotalTransactions(): Int?`
     - `@Query("SELECT SUM(subtotal - (buyPrice * quantity)) FROM transaction_items")` -> `suspend fun getNetProfit(): Double?`
     - `@Query("SELECT * FROM transactions ORDER BY transactionTime DESC LIMIT :limit")` -> `fun getRecentTransactionsFlow(limit: Int): Flow<List<LocalTransactionEntity>>`
   - In `ProductDao`, add:
     - `@Query("SELECT COUNT(id) FROM local_products")` -> `suspend fun getTotalProducts(): Int?`
2. **Repository**
   - Update `TransactionRepository` to expose these stats.
3. **ViewModel**
   - Create `DashboardViewModel` with a `DashboardState` data class.
   - Inject `TransactionRepository` and `ProductRepository` or DAOs directly.
   - Combine the stats into `StateFlow<DashboardState>`.
4. **UI**
   - In `MainScreen`, create `DashboardViewModel`.
   - Update `DashboardScreen` to accept `DashboardViewModel`, observe the state, and render the real values instead of dummy data.
