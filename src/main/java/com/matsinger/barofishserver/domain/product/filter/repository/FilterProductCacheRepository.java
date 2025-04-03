package com.matsinger.barofishserver.domain.product.filter.repository;

import com.matsinger.barofishserver.domain.product.filter.domain.FilterProductCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilterProductCacheRepository extends JpaRepository<FilterProductCache, String> {
    
    // 필드 조합에 맞는 캐시 조회
    Optional<FilterProductCache> findByCategoryIdAndSubCategoryIdAndFilterIdAndFieldIds(
            Integer categoryId, Integer subCategoryId, Integer filterId, List<Integer> fieldIds);
    
    // 필요한 경우 삭제 메서드
    void deleteByCategoryIdAndSubCategoryIdAndFilterId(
            Integer categoryId, Integer subCategoryId, Integer filterId);
} 