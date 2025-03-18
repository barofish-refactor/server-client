CREATE TABLE product_summary (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    review_cnt INT DEFAULT 0,
    taste_cnt INT DEFAULT 0,
    fresh_cnt INT DEFAULT 0,
    packaging_cnt INT DEFAULT 0,
    size_cnt INT DEFAULT 0,
    price_cnt INT DEFAULT 0,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_summary_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
); 