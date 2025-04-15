-- 테이블 이름 변경
RENAME TABLE filter_product_cache TO category_filter_products;

-- 컬럼 추가 (id 컬럼 뒤에 추가)
ALTER TABLE category_filter_products
ADD COLUMN category_id INT NOT NULL AFTER id,
ADD COLUMN sub_category_id INT NOT NULL AFTER category_id;