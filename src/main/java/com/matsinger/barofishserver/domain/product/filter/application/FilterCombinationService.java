package com.matsinger.barofishserver.domain.product.filter.application;

import com.matsinger.barofishserver.domain.product.filter.CombinationGeneratorCoordinator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FilterCombinationService {
    private final CombinationGeneratorCoordinator coordinator;

    /**
     * 카테고리에 대한 모든 필터 조합을 생성합니다.
     * 세 단계로 진행됩니다:
     * 1. 모든 필터에 대한 필터 필드 조합을 생성
     * 2. 카테고리에 대한 필터 조합을 생성
     * 3. 카테고리에 대한 최종 필터-필드 조합 생성
     */
    @Transactional
    public void generateAllCombinations(Integer categoryId) {
        coordinator.generateAllCombinations(categoryId);
    }

    /**
     * 첫 번째 단계: 모든 필터에 대한 필터 필드 조합을 생성합니다.
     * 이 메서드는 지정된 필터 ID에 해당하는 모든 필드 조합을 생성하고 저장합니다.
     */
    @Transactional
    public void generateFilterFieldCombinationsById(Integer filterId) {
        coordinator.generateFilterFieldCombinationsById(filterId);
    }

    /**
     * 두 번째 단계: 카테고리에 대한 필터 조합을 생성합니다.
     * 이 메서드는 특정 카테고리에 대한 모든 필터 조합을 생성하고 저장합니다.
     */
    @Transactional
    public void generateCategoryFilterCombinations(Integer categoryId) {
        coordinator.generateCategoryFilterCombinations(categoryId);
    }

    /**
     * 세 번째 단계: 카테고리에 대한 최종 필터-필드 조합을 생성합니다.
     * 이 메서드는 앞선 두 단계에서 생성된 데이터를 사용하여
     * 특정 카테고리에 대한 모든 필터-필드 조합을 생성합니다.
     */
    @Transactional
    public void generateCategoryFilterFieldCombinations(Integer categoryId) {
        coordinator.generateCategoryFilterFieldCombinations(categoryId);
    }
}
