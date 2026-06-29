CREATE TABLE IF NOT EXISTS kampo_products (
    id BIGINT PRIMARY KEY,
    identification_code VARCHAR(50) NOT NULL,
    sales_name VARCHAR(100) NOT NULL,
    reading VARCHAR(100),
    efficacy_condition_text TEXT NOT NULL,
    efficacy_indication_text TEXT NOT NULL,
    dosage_daily_amount NUMERIC(10,3) NOT NULL,
    dosage_instructions_text TEXT NOT NULL,
    source_file_name VARCHAR(255),
    source_document_no VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_kampo_products_sales_name UNIQUE (sales_name)
);

CREATE TABLE IF NOT EXISTS kampo_ingredients (
    id BIGINT PRIMARY KEY,
    ingredient_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_kampo_ingredients_ingredient_name UNIQUE (ingredient_name)
);

CREATE TABLE IF NOT EXISTS kampo_product_ingredients (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    amount_value NUMERIC(10,3) NOT NULL,
    amount_unit VARCHAR(20) NOT NULL DEFAULT 'g',
    sort_order INT NOT NULL,
    raw_amount_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_kampo_product_ingredients_product
        FOREIGN KEY (product_id) REFERENCES kampo_products(id),
    CONSTRAINT fk_kampo_product_ingredients_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES kampo_ingredients(id),
    CONSTRAINT uq_kampo_product_ingredients_product_ingredient
        UNIQUE (product_id, ingredient_id)
);

CREATE INDEX IF NOT EXISTS idx_kampo_product_ingredients_product_id
    ON kampo_product_ingredients(product_id);

CREATE INDEX IF NOT EXISTS idx_kampo_product_ingredients_ingredient_id
    ON kampo_product_ingredients(ingredient_id);

CREATE INDEX IF NOT EXISTS idx_kampo_product_ingredients_product_sort_id
    ON kampo_product_ingredients(product_id, sort_order, id);

CREATE INDEX IF NOT EXISTS idx_kampo_products_identification_code
    ON kampo_products(identification_code);
