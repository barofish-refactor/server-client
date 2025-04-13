CREATE TABLE if not exists product_recommend_cache (
     category_id BIGINT NOT NULL,
     sub_category_id BIGINT NOT NULL,
     product_ids MEDIUMTEXT NOT NULL, -- 또는 MEDIUMTEXT
     updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY (category_id, sub_category_id)
);