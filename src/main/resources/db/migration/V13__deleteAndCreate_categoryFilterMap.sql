-- 기존 테이블 삭제 (필요한 경우에만)
DROP TABLE IF EXISTS category_filter_map;

-- 새 구조로 테이블 생성
CREATE TABLE if not exists category_filter_map (
                                     id INT AUTO_INCREMENT PRIMARY KEY,
                                     category_id INT NOT NULL,
                                     filter_id INT NOT NULL,
    -- 기타 필요한 컬럼들
                                     UNIQUE KEY uk_category_filter (category_id, filter_id),
                                     FOREIGN KEY (category_id) REFERENCES category (id),
                                     FOREIGN KEY (filter_id) REFERENCES search_filter (id)
);

-- 기존 데이터가 있고 보존해야 한다면 데이터 마이그레이션
-- INSERT INTO category_filter_map (category_id, filter_id)
-- SELECT category_id, compare_filter_id FROM old_category_filter_map;