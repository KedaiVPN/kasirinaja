-- +goose Up
-- +goose StatementBegin
CREATE TABLE IF NOT EXISTS global_settings (
    id SERIAL PRIMARY KEY,
    key VARCHAR(255) UNIQUE NOT NULL,
    value VARCHAR(255) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO global_settings (key, value) VALUES ('is_master_product_enabled', 'true') ON CONFLICT DO NOTHING;
-- +goose StatementEnd

-- +goose Down
-- +goose StatementBegin
DROP TABLE IF EXISTS global_settings;
-- +goose StatementEnd
