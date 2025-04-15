CREATE TABLE if not exists category_search_filter_map (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    category_id INT NOT NULL,          -- 카테고리 ID
    search_filter_id INT NOT NULL,     -- 검색 필터 ID
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_category_id (category_id),
    INDEX idx_search_filter_id (search_filter_id),
    UNIQUE INDEX idx_category_filter_unique (category_id, search_filter_id)
);

-- 설명: 이 테이블은 카테고리와 검색 필터 간의 관계를 저장합니다.
-- 특정 카테고리에 어떤 검색 필터가 적용되는지를 결정합니다.
-- unique 인덱스를 통해 동일한 카테고리-필터 조합이 중복으로 저장되는 것을 방지합니다. 