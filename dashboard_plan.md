**Backend (Go API):**
1. Update `kasir-api-go/db/query/transaction.sql` to include queries for dashboard stats:
    - `GetStoreDashboardStats`: sum of `total_amount`, total transactions count, sum of (`sell_price` - `buy_price`)*`quantity` (net profit), count of total products.
    - `GetRecentStoreTransactions`: Returns the 5 most recent transactions for the store.
2. Run `sqlc generate`.
3. Add `DashboardHandler` or add methods to `TransactionHandler` to serve `/api/transactions/dashboard` route.
    - Make sure to fetch store ID from JWT middleware.

**Frontend (Android):**
1. Since we want Option C (offline-first, mix of local Room + server), we'll do this:
    - Fetch stats primarily from the local Room database using `TransactionDao` and `ProductDao`.
    - Fetch the recent transactions locally too.
2. Update `AppDatabase` queries:
    - `TransactionDao`: Add `getTotalRevenue()`, `getTotalTransactions()`, `getNetProfit()`, `getRecentTransactions(limit)`.
    - `ProductDao`: Add `getTotalProducts()`.
3. Create `DashboardViewModel` to run these Room queries and expose them via `StateFlow` to `DashboardScreen`.
4. Update `DashboardScreen` to consume this ViewModel.

Since you prefer Option C, the Android app is offline-first. Most logic for calculating totals can be done locally via Room queries, ensuring the dashboard works perfectly offline and instantly updates when a local transaction is made.
