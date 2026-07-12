-- +goose Up
ALTER TABLE users ADD COLUMN photo_url VARCHAR(500);

-- +goose Down
ALTER TABLE users DROP COLUMN photo_url;
