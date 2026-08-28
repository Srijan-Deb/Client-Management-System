CREATE TABLE product_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    price DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    billing_cycle VARCHAR(50) NOT NULL,
    category_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_categories(id)
);

-- Seed initial data
INSERT INTO product_categories (name, description) VALUES
('SaaS', 'Software as a Service subscriptions'),
('Support', 'Support and maintenance plans'),
('Add-ons', 'Additional features and modules');

INSERT INTO products (name, description, price, currency, billing_cycle, category_id) VALUES
('Basic Plan', 'Essential features for small teams', 9.00, 'INR', 'MONTHLY', (SELECT id FROM product_categories WHERE name = 'SaaS')),
('Pro Plan', 'Advanced features for growing businesses', 799.00, 'INR', 'MONTHLY', (SELECT id FROM product_categories WHERE name = 'SaaS')),
('Enterprise Plan', 'Full suite with dedicated support', 3999.00, 'INR', 'MONTHLY', (SELECT id FROM product_categories WHERE name = 'SaaS')),
('Annual Pro Plan', 'Pro plan billed annually (save 20%)', 7599.00, 'INR', 'YEARLY', (SELECT id FROM product_categories WHERE name = 'SaaS')),
('Premium Support', '24/7 phone and email support', 1199.00, 'INR', 'MONTHLY', (SELECT id FROM product_categories WHERE name = 'Support'));

