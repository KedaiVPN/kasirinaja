-- +goose Up
-- +goose StatementBegin
ALTER TABLE store_products DROP CONSTRAINT IF EXISTS store_products_master_product_id_fkey;
ALTER TABLE transaction_items DROP CONSTRAINT IF EXISTS transaction_items_master_product_id_fkey;
-- +goose StatementEnd

-- +goose Down
-- +goose StatementBegin
ALTER TABLE store_products ADD CONSTRAINT store_products_master_product_id_fkey FOREIGN KEY (master_product_id) REFERENCES master_products(id);
ALTER TABLE transaction_items ADD CONSTRAINT transaction_items_master_product_id_fkey FOREIGN KEY (master_product_id) REFERENCES master_products(id);
-- +goose StatementEnd
