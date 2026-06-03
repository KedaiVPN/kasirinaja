-- +goose Up
ALTER TABLE store_products ADD COLUMN local_name VARCHAR(255);
ALTER TABLE pending_products ADD COLUMN store_id UUID;

-- +goose Down
ALTER TABLE store_products DROP COLUMN local_name;
ALTER TABLE pending_products DROP COLUMN store_id;
