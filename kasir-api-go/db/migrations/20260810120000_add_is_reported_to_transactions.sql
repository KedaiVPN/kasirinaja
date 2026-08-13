-- +goose Up
ALTER TABLE transactions ADD COLUMN is_reported BOOLEAN NOT NULL DEFAULT FALSE;

-- +goose Down
ALTER TABLE transactions DROP COLUMN is_reported;
