-- name: CreateStore :one
INSERT INTO stores (
  owner_id, store_code, store_name, address, phone
) VALUES (
  $1, $2, $3, $4, $5
)
RETURNING *;

-- name: GetStore :one
SELECT * FROM stores
WHERE id = $1 LIMIT 1;
