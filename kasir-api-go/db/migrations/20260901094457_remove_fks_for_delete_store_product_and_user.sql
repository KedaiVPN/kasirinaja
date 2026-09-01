-- +goose Up
ALTER TABLE stock_movements DROP CONSTRAINT IF EXISTS stock_movements_store_product_id_fkey;
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_cashier_id_fkey;

-- +goose Down
ALTER TABLE stock_movements ADD CONSTRAINT stock_movements_store_product_id_fkey FOREIGN KEY (store_product_id) REFERENCES store_products(id);
ALTER TABLE transactions ADD CONSTRAINT transactions_cashier_id_fkey FOREIGN KEY (cashier_id) REFERENCES users(id);
