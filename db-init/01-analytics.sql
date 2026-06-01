-- Create a separate analytics database
CREATE DATABASE analytics;

-- Connect to it
\connect analytics

-- Customers
CREATE TABLE customers (
                           id          SERIAL PRIMARY KEY,
                           name        VARCHAR(100) NOT NULL,
                           city        VARCHAR(100) NOT NULL,
                           created_at  DATE NOT NULL DEFAULT CURRENT_DATE
);

-- Products
CREATE TABLE products (
                          id          SERIAL PRIMARY KEY,
                          name        VARCHAR(100) NOT NULL,
                          category    VARCHAR(50)  NOT NULL,
                          price       NUMERIC(10,2) NOT NULL
);

-- Orders
CREATE TABLE orders (
                        id           SERIAL PRIMARY KEY,
                        customer_id  INTEGER NOT NULL REFERENCES customers(id),
                        product_id   INTEGER NOT NULL REFERENCES products(id),
                        quantity     INTEGER NOT NULL,
                        amount       NUMERIC(10,2) NOT NULL,
                        order_date   DATE NOT NULL
);

-- Sample customers
INSERT INTO customers (name, city) VALUES
                                       ('Rahul Sharma', 'Delhi'),
                                       ('Priya Verma', 'Delhi'),
                                       ('Amit Patel', 'Mumbai'),
                                       ('Sneha Iyer', 'Bangalore'),
                                       ('Vikram Singh', 'Delhi'),
                                       ('Anjali Rao', 'Mumbai');

-- Sample products
INSERT INTO products (name, category, price) VALUES
                                                 ('Wireless Mouse', 'Electronics', 799.00),
                                                 ('Mechanical Keyboard', 'Electronics', 3499.00),
                                                 ('Office Chair', 'Furniture', 8999.00),
                                                 ('Standing Desk', 'Furniture', 21999.00),
                                                 ('Notebook Pack', 'Stationery', 299.00);

-- Sample orders
INSERT INTO orders (customer_id, product_id, quantity, amount, order_date) VALUES
                                                                               (1, 2, 1, 3499.00, '2026-01-15'),
                                                                               (1, 1, 2, 1598.00, '2026-02-10'),
                                                                               (2, 4, 1, 21999.00, '2026-01-20'),
                                                                               (3, 3, 2, 17998.00, '2026-03-05'),
                                                                               (4, 5, 10, 2990.00, '2026-02-28'),
                                                                               (5, 2, 1, 3499.00, '2026-03-12'),
                                                                               (5, 1, 1, 799.00, '2026-03-12'),
                                                                               (6, 4, 1, 21999.00, '2026-01-30'),
                                                                               (2, 1, 3, 2397.00, '2026-03-01'),
                                                                               (1, 3, 1, 8999.00, '2026-02-22');