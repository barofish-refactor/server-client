package com.matsinger.barofishserver.domain.product.filter.application;

import com.matsinger.barofishserver.domain.product.filter.domain.FilterProductCache;
import com.matsinger.barofishserver.domain.product.filter.repository.FilterProductCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 필터 상품 캐시 서비스 - MongoDB를 사용한 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilterProductCacheService {

    private final FilterProductCacheRepository filterProductCacheRepository;

    /**
     * 필터 ID와 필드 ID 조합에 해당하는 상품 ID 목록 조회
     */
    public List<Integer> getProductIdsByCategoryAndFilter(
            Integer categoryId, Integer subCategoryId, Integer filterId, List<Integer> fieldIds) {
        
        if (fieldIds == null || fieldIds.isEmpty()) {
            log.warn("필드 ID 목록이 비어 있습니다. 빈 목록을 반환합니다.");
            return Collections.emptyList();
        }
        
        // 필드 ID 정렬 (일관된 캐시 조회를 위해)
        Collections.sort(fieldIds);
        
        Optional<FilterProductCache> cacheOptional = filterProductCacheRepository
                .findByCategoryIdAndSubCategoryIdAndFilterIdAndFieldIds(
                        categoryId, subCategoryId, filterId, fieldIds);
        
        if (cacheOptional.isPresent()) {
            FilterProductCache cache = cacheOptional.get();
            // 접근 시간 업데이트
            cache.updateLastAccessed();
            filterProductCacheRepository.save(cache);
            
            log.debug("캐시 조회 성공 - 카테고리: {}, 서브카테고리: {}, 필터: {}, 필드: {}, 상품 수: {}",
                    categoryId, subCategoryId, filterId, fieldIds, cache.getProductCount());
            
            return cache.getProductIds();
        } else {
            log.warn("캐시 조회 실패 - 카테고리: {}, 서브카테고리: {}, 필터: {}, 필드: {}",
                    categoryId, subCategoryId, filterId, fieldIds);
            return Collections.emptyList();
        }
    }
} 