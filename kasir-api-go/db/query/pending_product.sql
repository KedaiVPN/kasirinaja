
-- name: CreatePendingProduct :one
INSERT INTO pending_products (
  name, buy_price, sell_price, stock, category, description, barcode, image_url
) VALUES (
  $1, $2, $3, $4, $5, $6, $7, $8
) RETURNING *;

-- name: ListPendingProducts :many
SELECT * FROM pending_products ORDER BY created_at DESC;

-- name: DeletePendingProduct :exec
DELETE FROM pending_products WHERE id = $1;
