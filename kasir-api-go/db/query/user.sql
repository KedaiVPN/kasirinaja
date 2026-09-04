-- name: GetUser :one
SELECT * FROM users
WHERE id = $1 LIMIT 1;

-- name: GetUserByEmail :one
SELECT * FROM users
WHERE email = $1 LIMIT 1;

-- name: CreateUser :one
INSERT INTO users (
  full_name, email, phone, password_hash, role, store_id
) VALUES (
  $1, $2, $3, $4, $5, $6
)
RETURNING *;

-- name: ListUsers :many
SELECT * FROM users
ORDER BY id;
-- name: UpdateUserStoreID :exec
UPDATE users SET store_id = $2 WHERE id = $1;

-- name: ListUsersByStore :many
SELECT * FROM users
WHERE store_id = $1
ORDER BY created_at ASC;

-- name: UpdateUserProfile :one
UPDATE users
SET
  full_name = COALESCE(sqlc.narg('full_name'), full_name),
  photo_url = COALESCE(sqlc.narg('photo_url'), photo_url),
  updated_at = CURRENT_TIMESTAMP
WHERE id = $1
RETURNING *;

-- name: DeleteUser :exec
DELETE FROM users
WHERE id = $1;

-- name: UpdateUserFCMToken :exec
UPDATE users SET fcm_token = $2 WHERE id = $1;
