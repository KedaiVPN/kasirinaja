-- name: CreateTransaction :one
INSERT INTO transactions (
  store_id, cashier_id, invoice_number, total_amount, paid_amount, change_amount, payment_method, transaction_time, sync_status, device_id
) VALUES (
  $1, $2, $3, $4, $5, $6, $7, $8, $9, $10
)
RETURNING *;

-- name: CreateTransactionItem :one
INSERT INTO transaction_items (
  transaction_id, store_product_id, master_product_id, product_name, barcode, quantity, buy_price, sell_price, subtotal
) VALUES (
  $1, $2, $3, $4, $5, $6, $7, $8, $9
)
RETURNING *;

-- name: CreateStockMovement :one
INSERT INTO stock_movements (
  store_id, store_product_id, movement_type, quantity, reference_type, reference_id
) VALUES (
  $1, $2, $3, $4, $5, $6
)
RETURNING *;

-- name: GetStoreDashboardStats :one
SELECT
    COALESCE(SUM(CASE WHEN DATE(transaction_time) = CURRENT_DATE THEN total_amount ELSE 0 END), 0)::BIGINT AS total_revenue,
    COUNT(CASE WHEN DATE(transaction_time) = CURRENT_DATE THEN id ELSE NULL END)::INT AS total_transactions,
    (SELECT COUNT(store_products.id)::INT FROM store_products WHERE store_products.store_id = $1 AND store_products.is_active = true) AS total_products,
    COALESCE(
        (SELECT SUM(ti.subtotal - (ti.buy_price * ti.quantity))
         FROM transaction_items ti
         JOIN transactions t ON ti.transaction_id = t.id
         WHERE t.store_id = $1 AND DATE(t.transaction_time) = CURRENT_DATE), 0
    )::BIGINT AS net_profit
FROM transactions
WHERE transactions.store_id = $1;

-- name: GetRecentStoreTransactions :many
SELECT * FROM transactions
WHERE store_id = $1
ORDER BY transaction_time DESC
LIMIT 5;

-- name: GetAllStoreTransactions :many
SELECT * FROM transactions
WHERE store_id = $1
ORDER BY transaction_time DESC;

-- name: GetTransactionItemsByTransactionId :many
SELECT * FROM transaction_items
WHERE transaction_id = $1;
