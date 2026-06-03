-- +goose Up
CREATE TABLE pending_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    buy_price DECIMAL(15, 2) NOT NULL,
    sell_price DECIMAL(15, 2) NOT NULL,
    stock INT NOT NULL DEFAULT -1, -- -1 means unlimited
    category VARCHAR(255) NOT NULL,
    description TEXT,
    barcode VARCHAR(255),
    image_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- +goose Down
DROP TABLE pending_products;
