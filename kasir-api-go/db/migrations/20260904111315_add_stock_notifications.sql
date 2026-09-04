-- +goose Up
ALTER TABLE store_products ADD COLUMN is_stock_notification_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN fcm_token VARCHAR(255);

-- +goose Down
ALTER TABLE store_products DROP COLUMN is_stock_notification_enabled;
ALTER TABLE users DROP COLUMN fcm_token;
