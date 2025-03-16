CREATE TABLE store_summary (
       id INT AUTO_INCREMENT PRIMARY KEY,
       store_id INT NOT NULL,
       review_cnt INT DEFAULT 0,
       taste_cnt INT DEFAULT 0,
       fresh_cnt INT DEFAULT 0,
       packaging_cnt INT DEFAULT 0,
       size_cnt INT DEFAULT 0,
       price_cnt INT DEFAULT 0,
       product_cnt INT DEFAULT 0,
       deleted BOOLEAN DEFAULT FALSE,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       CONSTRAINT fk_store_summary_store FOREIGN KEY (store_id) REFERENCES store(id) ON DELETE CASCADE
);
