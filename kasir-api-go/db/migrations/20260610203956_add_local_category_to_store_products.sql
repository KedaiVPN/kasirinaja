-- +goose Up
ALTER TABLE store_products ADD COLUMN local_category VARCHAR(255);

-- +goose Down
ALTER TABLE store_products DROP COLUMN local_category;
