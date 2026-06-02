-- name: CreateStoreProduct :one
INSERT INTO store_products (
  store_id, master_product_id, buy_price, sell_price, stock, min_stock
) VALUES (
  $1, $2, $3, $4, $5, $6
)
RETURNING *;

-- name: GetStoreProduct :one
SELECT * FROM store_products
WHERE id = $1 LIMIT 1;

-- name: ListStoreProductsByStore :many
SELECT * FROM store_products
WHERE store_id = $1
ORDER BY id;
