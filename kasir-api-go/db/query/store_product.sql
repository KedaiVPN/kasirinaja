-- name: CreateStoreProduct :one
INSERT INTO store_products (
  store_id, master_product_id, buy_price, sell_price, stock, min_stock, local_name, local_category
) VALUES (
  $1, $2, $3, $4, $5, $6, $7, $8
) RETURNING *;

-- name: GetStoreProduct :one
SELECT * FROM store_products WHERE id = $1;

-- name: ListStoreProductsByStore :many
SELECT sp.*, mp.barcode, mp.photo_url as image_url, c.name as category_name
FROM store_products sp
LEFT JOIN master_products mp ON sp.master_product_id = mp.id
LEFT JOIN categories c ON mp.category_id = c.id
WHERE sp.store_id = $1 ORDER BY sp.created_at DESC;

-- name: UpdateStoreProductStock :exec
UPDATE store_products SET stock = stock + $2 WHERE id = $1;

-- name: DeleteStoreProduct :exec
DELETE FROM store_products WHERE id = $1;

-- name: UpdateStoreProduct :exec
UPDATE store_products
SET buy_price = $2, sell_price = $3, stock = $4, local_name = $5, local_category = $6
WHERE id = $1;

-- name: DeleteStoreProductsByMasterID :exec
DELETE FROM store_products WHERE master_product_id = $1;

-- name: DeleteStockMovementsByMasterProduct :exec
DELETE FROM stock_movements WHERE store_product_id IN (SELECT id FROM store_products WHERE master_product_id = $1);
