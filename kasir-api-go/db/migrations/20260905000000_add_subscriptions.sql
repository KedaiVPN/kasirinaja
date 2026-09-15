-- +goose Up
-- +goose StatementBegin
CREATE TABLE IF NOT EXISTS subscription_plans (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    duration_days INT NOT NULL,
    price BIGINT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS subscription_transactions (
    id SERIAL PRIMARY KEY,
    reference VARCHAR(255) UNIQUE NOT NULL,
    merchant_ref VARCHAR(255) UNIQUE NOT NULL,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    plan_id INT NOT NULL REFERENCES subscription_plans(id) ON DELETE RESTRICT,
    amount BIGINT NOT NULL,
    payment_method VARCHAR(100) NOT NULL,
    payment_name VARCHAR(255) NOT NULL DEFAULT '',
    status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
    pay_code VARCHAR(255) NOT NULL DEFAULT '',
    qr_url TEXT NOT NULL DEFAULT '',
    checkout_url TEXT NOT NULL DEFAULT '',
    instructions_json TEXT NOT NULL DEFAULT '[]',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

ALTER TABLE stores ADD COLUMN IF NOT EXISTS pro_expires_at TIMESTAMP WITH TIME ZONE;
-- +goose StatementEnd

-- +goose Down
-- +goose StatementBegin
ALTER TABLE stores DROP COLUMN IF EXISTS pro_expires_at;
DROP TABLE IF EXISTS subscription_transactions;
DROP TABLE IF EXISTS subscription_plans;
-- +goose StatementEnd
