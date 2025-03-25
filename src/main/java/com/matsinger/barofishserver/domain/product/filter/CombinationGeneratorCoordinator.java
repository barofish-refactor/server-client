package com.matsinger.barofishserver.domain.product.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전체 조합 생성 과정을 조율하는 코디네이터 클래스
 */
@Service
@RequiredArgsConstructor
public class CombinationGeneratorCoordinator {
    
    private final FilterFieldCombinationGenerator filterFieldCombinationGenerator;
    private final CategoryFilterCombinationGenerator categoryFilterCombinationGenerator;
    private final CategoryFilterFieldCombinationGenerator categoryFilterFieldCombinationGenerator;
    
    /**
     * 카테고리에 대한 전체 필터-필드 조합 생성 프로세스를 실행합니다.
     * 1단계: 필터 필드 조합 생성
     * 2단계: 카테고리 필터 조합 생성
     * 3단계: 카테고리 필터-필드 조합 생성
     */
    @Transactional
    public void generateAllCombinations(Integer categoryId) {
        // 1단계: 모든 필터에 대한 필터 필드 조합 생성
        filterFieldCombinationGenerator.generateAllFilterFieldCombinations();
        
        // 2단계: 카테고리에 대한 필터 조합 생성
        categoryFilterCombinationGenerator.generateFilterCombinationsForCategory(categoryId);
        
        // 3단계: 카테고리에 대한 최종 필터-필드 조합 생성
        categoryFilterFieldCombinationGenerator.generateCombinationsForCategory(categoryId);
    }
    
    /**
     * 필터 필드 조합만 생성합니다. (1단계)
     */
    @Transactional
    public void generateFilterFieldCombinations() {
        filterFieldCombinationGenerator.generateAllFilterFieldCombinations();
    }
    
    /**
     * 특정 필터 ID에 대한 필터 필드 조합만 생성합니다.
     */
    @Transactional
    public void generateFilterFieldCombinationsById(Integer filterId) {
        filterFieldCombinationGenerator.generateFilterFieldCombinationsById(filterId);
    }
    
    /**
     * 카테고리 필터 조합만 생성합니다. (2단계)
     */
    @Transactional
    public void generateCategoryFilterCombinations(Integer categoryId) {
        categoryFilterCombinationGenerator.generateFilterCombinationsForCategory(categoryId);
    }
    
    /**
     * 카테고리 필터-필드 조합만 생성합니다. (3단계)
     */
    @Transactional
    public void generateCategoryFilterFieldCombinations(Integer categoryId) {
        categoryFilterFieldCombinationGenerator.generateCombinationsForCategory(categoryId);
    }
} 