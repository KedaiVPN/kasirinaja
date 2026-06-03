-- name: CreateStoreProduct :one
INSERT INTO store_products (
  store_id, master_product_id, buy_price, sell_price, stock, min_stock, local_name
) VALUES (
  $1, $2, $3, $4, $5, $6, $7
) RETURNING *;

-- name: GetStoreProduct :one
SELECT * FROM store_products WHERE id = $1;

-- name: ListStoreProductsByStore :many
SELECT * FROM store_products WHERE store_id = $1 ORDER BY created_at DESC;

-- name: UpdateStoreProductStock :exec
UPDATE store_products SET stock = stock + $2 WHERE id = $1;
