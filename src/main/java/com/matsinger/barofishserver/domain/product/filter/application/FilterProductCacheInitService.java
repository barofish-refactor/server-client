package com.matsinger.barofishserver.domain.product.filter.application;

import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.category.domain.CategorySearchFilterMap;
import com.matsinger.barofishserver.domain.category.repository.CategorySearchFilterMapRepository;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterProducts;
import com.matsinger.barofishserver.domain.product.filter.repository.CategoryFilterProductsRepository;
import com.matsinger.barofishserver.domain.product.filter.utils.FilterConverter;
import com.matsinger.barofishserver.domain.product.processor.AbstractCategoryProcessor;
import com.matsinger.barofishserver.domain.product.repository.ProductQueryRepository;
import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilter;
import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilterField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 애플리케이션 시작 시 FilterProductCache를 초기화하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilterProductCacheInitService extends AbstractCategoryProcessor {

    private final ProductService productService;
    private final ProductQueryRepository productQueryRepository;
    private final CategoryFilterProductsRepository categoryFilterProductsRepository;
    private final CategorySearchFilterMapRepository categorySearchFilterMapRepository;

    @Transactional
    @Override
    public void processCategory(Category category) {
        List<CategorySearchFilterMap> categoryFilterMaps = categorySearchFilterMapRepository.findAllByCategoryId(category.getId());
        List<SearchFilter> categoryFilters = categoryFilterMaps.stream()
                .map(CategorySearchFilterMap::getSearchFilter)
                .toList();

        for (SearchFilter filter : categoryFilters) {
            List<Integer> fieldIds = getFieldIdsFromFilter(filter);

            for (Integer fieldId : fieldIds) {
                boolean exists = checkIfCombinationExists(category.getCategoryId(), category.getId(), filter.getId(), fieldId);
                if (exists) {
                    return;
                }

                if (category.isParent()) {
                    List<Integer> productIds = productService.findIdsByCategoryIdsInAndFieldId(
                            category.getCategoryList().stream()
                                    .map(Category::getId)
                                    .collect(Collectors.toList()),
                            fieldId);
                    Collections.sort(productIds);
                    saveFilterProductCache(category.getId(), null, filter, fieldId, productIds);
                } else {
                    List<Integer> productIds = productQueryRepository.findCategoryFieldProduct(category.getId(), fieldId);
                    Collections.sort(productIds);
                    saveFilterProductCache(category.getCategoryId(), category.getId(), filter, fieldId, productIds);
                }
            }
        }
    }

    @Override
    protected void processAll() {
    }

    private List<Integer> getFieldIdsFromFilter(SearchFilter filter) {
        List<SearchFilterField> fields = filter.getSearchFilterFields();
        return fields.stream()
                .map(SearchFilterField::getId)
                .collect(Collectors.toList());
    }

    /**
     * 주어진 필드 ID 목록의 모든 가능한 조합을 생성
     * (비어 있지 않은 모든 부분집합을 생성)
     */
    private List<List<Integer>> generateAllFieldCombinations(List<Integer> fieldIds) {
        List<List<Integer>> result = new ArrayList<>();
        // 재귀 호출을 통해 모든 부분집합 생성 (공집합 제외)
        generateSubsets(fieldIds, 0, new ArrayList<>(), result);
        return result;
    }

    private void generateSubsets(
            List<Integer> fieldIds, int index,
            List<Integer> current, List<List<Integer>> result) {
        // 모든 요소를 고려했을 때
        if (index == fieldIds.size()) {
            // 공집합이 아니면 결과에 추가
            if (!current.isEmpty()) {
                result.add(new ArrayList<>(current));
            }
            return;
        }
        
        // 현재 요소를 포함하지 않는 경우
        generateSubsets(fieldIds, index + 1, current, result);
        
        // 현재 요소를 포함하는 경우
        current.add(fieldIds.get(index));
        generateSubsets(fieldIds, index + 1, current, result);
        
        // 백트래킹을 위해 추가했던 요소 제거
        current.remove(current.size() - 1);
    }
    
    /**
     * 특정 필터 조합이 이미 존재하는지 확인
     */
    private boolean checkIfCombinationExists(Integer pCategoryId, Integer cCategoryId, Integer filterId, Integer field) {
        return categoryFilterProductsRepository
                .existsByCategoryIdAndSubCategoryIdAndFilterIdAndFieldId(pCategoryId, cCategoryId, filterId, field);
    }
    
    /**
     * 필터 상품 캐시 저장
     */
    @Transactional
    public void saveFilterProductCache(
            Integer pCategoryId,
            Integer cCategoryId,
            SearchFilter filter,
            Integer fieldId,
            List<Integer> productIds
    ) {

        CategoryFilterProducts categoryFilterProducts = CategoryFilterProducts.from(
                pCategoryId,
                cCategoryId,
                filter.getId(),
                fieldId,
                FilterConverter.convert(productIds)
        );
        categoryFilterProductsRepository.save(categoryFilterProducts);
    }
} 