-- name: GetGlobalSetting :one
SELECT value FROM global_settings
WHERE key = $1 LIMIT 1;

-- name: UpdateGlobalSetting :exec
UPDATE global_settings
SET value = $2,
    updated_at = CURRENT_TIMESTAMP
WHERE key = $1;
