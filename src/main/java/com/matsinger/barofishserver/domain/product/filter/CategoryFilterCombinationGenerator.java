package com.matsinger.barofishserver.domain.product.filter;

import com.matsinger.barofishserver.domain.category.application.CategoryQueryService;
import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilter;
import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterCombination;
import com.matsinger.barofishserver.domain.product.filter.repository.CategoryFilterCombinationRepository;
import com.matsinger.barofishserver.domain.product.filter.repository.CategoryFilterRepository;
import com.matsinger.barofishserver.domain.product.filter.util.FilterCombinationUtils;
import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 카테고리에 적용 가능한 모든 필터 조합을 생성하는 클래스
 */
@Component
@RequiredArgsConstructor
public class CategoryFilterCombinationGenerator {
    
    private final CategoryFilterRepository categoryFilterRepository;
    private final CategoryFilterCombinationRepository categoryFilterCombinationRepository;
    private final CategoryQueryService categoryQueryService;
    
    /**
     * 특정 카테고리에 대한 모든 필터 조합을 생성하고 저장합니다.
     * 기존 데이터를 모두 삭제하고 새로 생성하는 방식으로 구현합니다.
     */
    @Transactional
    public void generateFilterCombinationsForCategory(Integer categoryId) {
        // 카테고리 정보 조회
        Category category = categoryQueryService.findById(categoryId);
        
        // 기존 카테고리 필터 조합 삭제
        categoryFilterCombinationRepository.deleteByCategoryId(categoryId);
        
        // 카테고리에 연결된 모든 필터 조회
        List<CategoryFilter> categoryFilters = categoryFilterRepository.findByCategoryId(categoryId);
        
        // 필터가 없으면 처리 중단
        if (categoryFilters.isEmpty()) {
            return;
        }
        
        // 필터 정보 추출
        List<SearchFilter> filters = categoryFilters.stream()
            .map(CategoryFilter::getFilter)
            .collect(Collectors.toList());
        
        // 필터의 모든 부분집합 생성 (빈 집합 제외)
        List<List<SearchFilter>> filterCombinations = FilterCombinationUtils.generateNonEmptySubsets(filters);
        
        // 모든 조합을 담을 리스트
        List<CategoryFilterCombination> combinationsToAdd = new ArrayList<>();
        
        // 각 조합에 대해 처리
        for (List<SearchFilter> combination : filterCombinations) {
            CategoryFilterCombination filterCombination = createFilterCombination(category, combination);
            combinationsToAdd.add(filterCombination);
        }
        
        // 모든 조합 한 번에 저장
        if (!combinationsToAdd.isEmpty()) {
            categoryFilterCombinationRepository.saveAll(combinationsToAdd);
        }
    }
    
    /**
     * 필터 조합을 생성합니다.
     */
    private CategoryFilterCombination createFilterCombination(Category category, List<SearchFilter> combination) {
        // 필터 조합 생성
        return CategoryFilterCombination.builder()
            .categoryId(category.getId())
            .filterIds(FilterCombinationUtils.createFilterIdsString(combination))
            .filterNames(FilterCombinationUtils.createFilterNamesString(combination))
            .category(category)
            .build();
    }
} 