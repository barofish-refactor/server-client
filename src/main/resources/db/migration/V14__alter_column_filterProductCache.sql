ALTER TABLE filter_product_cache
    MODIFY field_ids VARCHAR(255) NOT NULL,
    MODIFY product_ids MEDIUMTEXT NOT NULL;