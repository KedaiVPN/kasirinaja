-- name: CreateMasterProduct :one
INSERT INTO master_products (
  barcode, name, photo_url, photo_path, category_id, brand_id, unit, source, is_generated_barcode, created_by
) VALUES (
  $1, $2, $3, $4, $5, $6, $7, $8, $9, $10
)
RETURNING *;

-- name: GetMasterProduct :one
SELECT * FROM master_products
WHERE id = $1 LIMIT 1;

-- name: ListMasterProducts :many
SELECT * FROM master_products
ORDER BY id;
