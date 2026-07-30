-- +goose Up
-- +goose StatementBegin
ALTER TABLE transaction_items DROP CONSTRAINT IF EXISTS transaction_items_store_product_id_fkey;
-- +goose StatementEnd

-- +goose Down
-- +goose StatementBegin
ALTER TABLE transaction_items ADD CONSTRAINT transaction_items_store_product_id_fkey FOREIGN KEY (store_product_id) REFERENCES store_products(id);
-- +goose StatementEnd
