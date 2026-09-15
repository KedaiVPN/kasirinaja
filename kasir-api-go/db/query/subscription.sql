-- name: CreateSubscriptionPlan :one
INSERT INTO subscription_plans (name, duration_days, price, description, is_active)
VALUES ($1, $2, $3, $4, $5)
RETURNING *;

-- name: GetSubscriptionPlanByID :one
SELECT * FROM subscription_plans WHERE id = $1;

-- name: ListActiveSubscriptionPlans :many
SELECT * FROM subscription_plans WHERE is_active = TRUE ORDER BY price ASC;

-- name: ListAllSubscriptionPlans :many
SELECT * FROM subscription_plans ORDER BY id DESC;

-- name: UpdateSubscriptionPlan :one
UPDATE subscription_plans
SET name = $1, duration_days = $2, price = $3, description = $4, is_active = $5, updated_at = NOW()
WHERE id = $6
RETURNING *;

-- name: DeleteSubscriptionPlan :exec
DELETE FROM subscription_plans WHERE id = $1;

-- name: CreateSubscriptionTransaction :one
INSERT INTO subscription_transactions (
    reference, merchant_ref, store_id, plan_id, amount, payment_method, payment_name, status, pay_code, qr_url, checkout_url, instructions_json, expires_at
)
VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
RETURNING *;

-- name: GetSubscriptionTransactionByReference :one
SELECT st.*, sp.name as plan_name, sp.duration_days, s.store_name
FROM subscription_transactions st
JOIN subscription_plans sp ON st.plan_id = sp.id
JOIN stores s ON st.store_id = s.id
WHERE st.reference = $1;

-- name: GetSubscriptionTransactionByMerchantRef :one
SELECT st.*, sp.name as plan_name, sp.duration_days, s.store_name
FROM subscription_transactions st
JOIN subscription_plans sp ON st.plan_id = sp.id
JOIN stores s ON st.store_id = s.id
WHERE st.merchant_ref = $1;

-- name: UpdateSubscriptionTransactionStatus :one
UPDATE subscription_transactions
SET status = $1, paid_at = $2, updated_at = NOW()
WHERE reference = $3
RETURNING *;

-- name: ListSubscriptionTransactions :many
SELECT st.*, sp.name as plan_name, s.store_name
FROM subscription_transactions st
JOIN subscription_plans sp ON st.plan_id = sp.id
JOIN stores s ON st.store_id = s.id
ORDER BY st.id DESC;

-- name: UpdateStoreProExpiry :one
UPDATE stores
SET pro_expires_at = $1, updated_at = NOW()
WHERE id = $2
RETURNING *;

-- name: GetStoreProStatus :one
SELECT id, store_name, pro_expires_at FROM stores WHERE id = $1;
