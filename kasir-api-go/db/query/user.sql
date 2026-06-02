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
