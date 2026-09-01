-- name: CreateCashierReport :one
INSERT INTO cashier_reports (
  store_id, cashier_id, cashier_name, start_time, end_time, total_transactions, total_revenue, total_profit
) VALUES (
  $1, $2, $3, $4, $5, $6, $7, $8
)
RETURNING *;

-- name: GetCashierReportsByStore :many
SELECT * FROM cashier_reports
WHERE store_id = $1
ORDER BY created_at DESC;

-- name: GetCashierReportsByCashier :many
SELECT * FROM cashier_reports
WHERE store_id = $1 AND cashier_id = $2
ORDER BY created_at DESC;

-- name: DeleteCashierReport :exec
DELETE FROM cashier_reports
WHERE id = $1 AND store_id = $2;

-- name: GetCashierReportById :one
SELECT * FROM cashier_reports
WHERE id = $1 AND store_id = $2;

-- name: UnmarkTransactionsAsReported :exec
UPDATE transactions
SET is_reported = false
WHERE store_id = $1 AND cashier_id = $2 AND transaction_time >= $3 AND transaction_time <= $4;
