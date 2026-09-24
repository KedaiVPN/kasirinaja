-- +goose Up
-- +goose StatementBegin
ALTER TABLE stores ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN NOT NULL DEFAULT FALSE;
-- +goose StatementEnd

-- +goose Down
-- +goose StatementBegin
ALTER TABLE stores DROP COLUMN IF EXISTS is_blocked;
-- +goose StatementEnd
