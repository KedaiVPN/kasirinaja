-- name: CreatePendingProduct :one
INSERT INTO pending_products (
  name, buy_price, sell_price, stock, category, description, barcode, image_url, store_id
) VALUES (
  $1, $2, $3, $4, $5, $6, $7, $8, $9
) RETURNING *;

-- name: ListPendingProducts :many
SELECT * FROM pending_products ORDER BY created_at DESC;

-- name: DeletePendingProduct :exec
DELETE FROM pending_products WHERE id = $1;

-- name: GetPendingProduct :one
SELECT * FROM pending_products WHERE id = $1;

-- name: UpdatePendingProduct :exec
UPDATE pending_products
SET name = $2, buy_price = $3, sell_price = $4, stock = $5, category = $6, description = $7, barcode = $8, image_url = $9
WHERE id = $1;

-- name: CountPendingProducts :one
SELECT COUNT(*) FROM pending_products;
