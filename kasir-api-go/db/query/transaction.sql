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
