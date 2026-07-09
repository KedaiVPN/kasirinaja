-- +goose Up
ALTER TABLE stores ADD COLUMN logo_url VARCHAR(255);

-- +goose Down
ALTER TABLE stores DROP COLUMN logo_url;
