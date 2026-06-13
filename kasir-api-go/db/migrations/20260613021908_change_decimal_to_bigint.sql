-- +goose Up
ALTER TABLE store_products ALTER COLUMN buy_price TYPE BIGINT USING buy_price::BIGINT;
ALTER TABLE store_products ALTER COLUMN sell_price TYPE BIGINT USING sell_price::BIGINT;

ALTER TABLE pending_products ALTER COLUMN buy_price TYPE BIGINT USING buy_price::BIGINT;
ALTER TABLE pending_products ALTER COLUMN sell_price TYPE BIGINT USING sell_price::BIGINT;

ALTER TABLE transactions ALTER COLUMN total_amount TYPE BIGINT USING total_amount::BIGINT;
ALTER TABLE transactions ALTER COLUMN paid_amount TYPE BIGINT USING paid_amount::BIGINT;
ALTER TABLE transactions ALTER COLUMN change_amount TYPE BIGINT USING change_amount::BIGINT;

ALTER TABLE transaction_items ALTER COLUMN buy_price TYPE BIGINT USING buy_price::BIGINT;
ALTER TABLE transaction_items ALTER COLUMN sell_price TYPE BIGINT USING sell_price::BIGINT;
ALTER TABLE transaction_items ALTER COLUMN subtotal TYPE BIGINT USING subtotal::BIGINT;

-- +goose Down
ALTER TABLE store_products ALTER COLUMN buy_price TYPE DECIMAL(15, 2) USING buy_price::DECIMAL;
ALTER TABLE store_products ALTER COLUMN sell_price TYPE DECIMAL(15, 2) USING sell_price::DECIMAL;

ALTER TABLE pending_products ALTER COLUMN buy_price TYPE DECIMAL(15, 2) USING buy_price::DECIMAL;
ALTER TABLE pending_products ALTER COLUMN sell_price TYPE DECIMAL(15, 2) USING sell_price::DECIMAL;

ALTER TABLE transactions ALTER COLUMN total_amount TYPE DECIMAL(15, 2) USING total_amount::DECIMAL;
ALTER TABLE transactions ALTER COLUMN paid_amount TYPE DECIMAL(15, 2) USING paid_amount::DECIMAL;
ALTER TABLE transactions ALTER COLUMN change_amount TYPE DECIMAL(15, 2) USING change_amount::DECIMAL;

ALTER TABLE transaction_items ALTER COLUMN buy_price TYPE DECIMAL(15, 2) USING buy_price::DECIMAL;
ALTER TABLE transaction_items ALTER COLUMN sell_price TYPE DECIMAL(15, 2) USING sell_price::DECIMAL;
ALTER TABLE transaction_items ALTER COLUMN subtotal TYPE DECIMAL(15, 2) USING subtotal::DECIMAL;
