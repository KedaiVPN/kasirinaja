-- +goose Up
-- +goose StatementBegin
ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_user_store;
ALTER TABLE stores DROP CONSTRAINT IF EXISTS stores_owner_id_fkey;
-- +goose StatementEnd

-- +goose Down
-- +goose StatementBegin
ALTER TABLE users ADD CONSTRAINT fk_user_store FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE stores ADD CONSTRAINT stores_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES users(id);
-- +goose StatementEnd
