CREATE TABLE if not exists filter_product_cache (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,

      category_id INT NOT NULL,               -- 대카테고리 ID
      category_name VARCHAR(100) NOT NULL,    -- 대카테고리 이름

      sub_category_id INT NOT NULL,           -- 소카테고리 ID
      sub_category_name VARCHAR(100) NOT NULL,-- 소카테고리 이름

      filter_id INT NOT NULL,                 -- 필터 ID
      field_ids JSON NOT NULL,                -- 선택된 필드 ID 배열
      product_ids JSON NOT NULL               -- 해당 조합에 해당하는 상품 ID 배열
);
