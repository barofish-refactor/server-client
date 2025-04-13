CREATE TABLE product_recommend (
    product_id int NOT NULL,
    category_id int NOT NULL,
    weight FLOAT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id, category_id),
    CONSTRAINT fk_product_recommend_product
       FOREIGN KEY (product_id) REFERENCES product(id)
           ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_product_recommend_category
       FOREIGN KEY (category_id) REFERENCES category(id)
           ON DELETE CASCADE ON UPDATE CASCADE
);
