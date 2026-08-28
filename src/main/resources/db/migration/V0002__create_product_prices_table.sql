CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE product_prices (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    price_amount NUMERIC(19, 2) NOT NULL,
    price_currency VARCHAR(3) NOT NULL,
    init_date DATE NOT NULL,
    end_date DATE,
    CONSTRAINT chk_price_amount_positive CHECK (price_amount > 0),
    CONSTRAINT chk_price_dates CHECK (end_date IS NULL OR init_date < end_date),
    CONSTRAINT ex_product_currency_validity EXCLUDE USING gist (
        product_id WITH =,
        price_currency WITH =,
        daterange(init_date, coalesce(end_date, 'infinity'), '[]') WITH &&
    )
);

CREATE INDEX idx_product_prices_lookup 
ON product_prices (product_id, price_currency, init_date ASC);
