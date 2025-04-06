package com.matsinger.barofishserver.domain.product.filter.application;

import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterProducts;
import com.matsinger.barofishserver.domain.product.filter.repository.CategoryFilterProductsRepository;
import com.matsinger.barofishserver.domain.product.filter.utils.FilterConverter;
import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilter;
import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilterField;
import com.matsinger.barofishserver.domain.searchFilter.repository.SearchFilterRepository;
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
public class FilterProductCacheInitService {

    private final ProductService productService;
    private final CategoryFilterProductsRepository categoryFilterProductsRepository;
    private final SearchFilterRepository searchFilterRepository;

    /**
     * 모든 카테고리에 대한 필터 상품 캐시 초기화
     */
    public void initializeFilterProductCaches() {
        try {
            List<SearchFilter> filters = searchFilterRepository.findAll();

            for (SearchFilter filter : filters) {
                processSingleFilter(filter);
            }
        } catch (Exception e) {
            log.error("필터 상품 캐시 초기화 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 특정 카테고리와 필터에 대한 필터 상품 캐시 초기화
     */
    @Transactional
    public void processSingleFilter(SearchFilter filter) {

        try {
            List<Integer> fieldIds = getFieldIdsFromFilter(filter);
            List<List<Integer>> allFieldCombinations = generateAllFieldCombinations(fieldIds);

            for (List<Integer> fieldCombination : allFieldCombinations) {
                if (fieldCombination.isEmpty()) {
                    continue;
                }

                Collections.sort(fieldCombination);
                boolean exists = checkIfCombinationExists(filter.getId(), FilterConverter.convert(fieldCombination));

                if (!exists) {
                    List<Integer> productIds = productService.findIdsByFieldIdsIn(fieldCombination);
                    Collections.sort(productIds);

                    // 저장
                    saveFilterProductCache(filter, fieldCombination, productIds);
                }
            }
        } catch (Exception e) {
            log.error("필터 상품 캐시 초기화 중 오류 발생 - 필터: {}",
                    filter.getName(), e);
            throw e;
        }
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
    private boolean checkIfCombinationExists(Integer filterId, String fieldString) {
        return categoryFilterProductsRepository
                .existsByFilterIdAndFieldIds(filterId, fieldString);
    }
    
    /**
     * 필터 상품 캐시 저장
     */
    @Transactional
    public void saveFilterProductCache(
            SearchFilter filter,
            List<Integer> fieldIds,
            List<Integer> productIds
    ) {

        CategoryFilterProducts categoryFilterProducts = CategoryFilterProducts.from(
                filter.getId(),
                FilterConverter.convert(fieldIds),
                FilterConverter.convert(productIds)
        );
        categoryFilterProductsRepository.save(categoryFilterProducts);
    }
} 