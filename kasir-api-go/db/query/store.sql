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

-- name: UpdateStore :one
UPDATE stores
SET store_name = COALESCE($2, store_name),
    address = COALESCE($3, address),
    phone = COALESCE($4, phone),
    logo_url = COALESCE($5, logo_url),
    updated_at = CURRENT_TIMESTAMP
WHERE id = $1
RETURNING *;
