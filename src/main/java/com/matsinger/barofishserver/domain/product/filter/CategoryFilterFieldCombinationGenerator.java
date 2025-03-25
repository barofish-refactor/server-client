package com.matsinger.barofishserver.domain.product.filter;

import com.matsinger.barofishserver.domain.category.application.CategoryQueryService;
import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterCombination;
import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterFieldCombination;
import com.matsinger.barofishserver.domain.product.filter.domain.FilterFieldCombination;
import com.matsinger.barofishserver.domain.product.filter.repository.CategoryFilterCombinationRepository;
import com.matsinger.barofishserver.domain.product.filter.repository.CategoryFilterFieldCombinationRepository;
import com.matsinger.barofishserver.domain.product.filter.repository.FilterFieldCombinationRepository;
import com.matsinger.barofishserver.domain.product.filter.util.FilterCombinationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 카테고리별 최종 필터-필드 조합을 생성하는 클래스
 */
@Component
@RequiredArgsConstructor
public class CategoryFilterFieldCombinationGenerator {
    
    private final CategoryFilterCombinationRepository categoryFilterCombinationRepository;
    private final FilterFieldCombinationRepository filterFieldCombinationRepository;
    private final CategoryFilterFieldCombinationRepository categoryFilterFieldCombinationRepository;
    private final CategoryQueryService categoryQueryService;
    
    /**
     * 특정 카테고리에 대한 모든 필터-필드 조합을 생성합니다.
     * 기존 데이터를 모두 삭제하고 새로 생성하는 방식으로 구현합니다.
     */
    @Transactional
    public void generateCombinationsForCategory(Integer categoryId) {
        // 카테고리 정보 조회
        Category category = categoryQueryService.findById(categoryId);
        
        // 기존 카테고리 조합 모두 삭제
        categoryFilterFieldCombinationRepository.deleteByCategoryId(categoryId);
        
        // 카테고리에 해당하는 모든 필터 조합 가져오기
        List<CategoryFilterCombination> filterCombinations = 
            categoryFilterCombinationRepository.findByCategoryId(categoryId);
        
        // 새로운 조합을 담을 리스트
        List<CategoryFilterFieldCombination> combinationsToAdd = new ArrayList<>();
        
        for (CategoryFilterCombination filterCombination : filterCombinations) {
            processCombination(category, filterCombination, combinationsToAdd);
        }
        
        // 모든 조합 한 번에 저장
        if (!combinationsToAdd.isEmpty()) {
            categoryFilterFieldCombinationRepository.saveAll(combinationsToAdd);
        }
    }
    
    /**
     * 각 필터 조합을 처리합니다.
     */
    private void processCombination(
            Category category, 
            CategoryFilterCombination filterCombination,
            List<CategoryFilterFieldCombination> combinationsToAdd) {
        
        // 필터 ID 목록 (쉼표로 구분된 문자열에서 파싱)
        List<Integer> filterIds = Arrays.stream(filterCombination.getFilterIds().split(","))
            .map(Integer::valueOf)
            .collect(Collectors.toList());
        
        // 각 필터에 대한 필드 조합 가져오기
        List<List<FilterFieldCombination>> fieldCombinationsByFilter = new ArrayList<>();
        
        for (Integer filterId : filterIds) {
            List<FilterFieldCombination> fieldCombinations = 
                filterFieldCombinationRepository.findByFilterId(filterId);
            
            if (!fieldCombinations.isEmpty()) {
                fieldCombinationsByFilter.add(fieldCombinations);
            }
        }
        
        // 필드 조합이 없는 경우 처리 중단
        if (fieldCombinationsByFilter.isEmpty()) {
            return;
        }
        
        // 데카르트 곱 계산
        List<List<FilterFieldCombination>> allCombinations = 
            FilterCombinationUtils.cartesianProduct(fieldCombinationsByFilter);
        
        // 각 조합에 대해 처리
        for (List<FilterFieldCombination> combination : allCombinations) {
            // 필터-필드 조합 문자열 생성
            String filterIdsString = FilterCombinationUtils.createFilterFieldIdsString(combination);
            String filterKeysString = FilterCombinationUtils.createFilterFieldKeysString(combination);
            
            // 새 조합 생성 및 추가
            CategoryFilterFieldCombination fieldCombination = CategoryFilterFieldCombination.builder()
                .categoryId(category.getId())
                .filterIds(filterIdsString)
                .filterKeys(filterKeysString)
                .category(category)
                .build();
            
            combinationsToAdd.add(fieldCombination);
        }
    }
} 