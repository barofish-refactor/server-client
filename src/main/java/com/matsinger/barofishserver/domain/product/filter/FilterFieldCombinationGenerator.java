package com.matsinger.barofishserver.domain.product.filter;

import com.matsinger.barofishserver.domain.product.filter.domain.FilterFieldCombination;
import com.matsinger.barofishserver.domain.product.filter.repository.FilterFieldCombinationRepository;
import com.matsinger.barofishserver.domain.product.filter.util.FilterCombinationUtils;
import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilter;
import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilterField;
import com.matsinger.barofishserver.domain.searchFilter.repository.SearchFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 모든 필터에 대한 필터 필드 조합을 생성하는 클래스
 */
@Component
@RequiredArgsConstructor
public class FilterFieldCombinationGenerator {

    private final SearchFilterRepository searchFilterRepository;
    private final FilterFieldCombinationRepository filterFieldCombinationRepository;

    /**
     * 모든 필터에 대한 필터 필드 조합을 생성하고 저장합니다.
     */
    @Transactional
    public void generateAllFilterFieldCombinations() {
        // 모든 필터 조회
        List<SearchFilter> filters = searchFilterRepository.findAll();

        for (SearchFilter filter : filters) {
            generateFilterFieldCombinations(filter);
        }
    }
    
    /**
     * 특정 필터 ID에 대한 필터 필드 조합을 생성하고 저장합니다.
     */
    @Transactional
    public void generateFilterFieldCombinationsById(Integer filterId) {
        // 특정 ID의 필터 조회
        SearchFilter filter = searchFilterRepository.findById(filterId)
            .orElseThrow(() -> new RuntimeException("필터를 찾을 수 없습니다: " + filterId));
        
        // 해당 필터에 대한 조합 생성
        generateFilterFieldCombinations(filter);
    }

    /**
     * 특정 필터에 대한 필터 필드 조합을 생성하고 저장합니다.
     * 기존 데이터를 모두 삭제하고 새로 생성하는 방식으로 구현합니다.
     */
    @Transactional
    public void generateFilterFieldCombinations(SearchFilter filter) {
        // 기존 조합 삭제
        filterFieldCombinationRepository.deleteByFilterId(filter.getId());

        List<SearchFilterField> fields = filter.getSearchFilterFields();
        if (fields == null || fields.isEmpty()) {
            return;
        }

        // 모든 가능한 필드 조합 생성 (빈 집합 제외)
        List<List<SearchFilterField>> fieldCombinations = FilterCombinationUtils.generateNonEmptySubsets(fields);
        
        // 모든 조합을 담을 리스트
        List<FilterFieldCombination> combinationsToAdd = new ArrayList<>();

        for (List<SearchFilterField> combination : fieldCombinations) {
            FilterFieldCombination fieldCombination = createFilterFieldCombination(filter, combination);
            combinationsToAdd.add(fieldCombination);
        }
        
        // 모든 조합 한 번에 저장
        if (!combinationsToAdd.isEmpty()) {
            filterFieldCombinationRepository.saveAll(combinationsToAdd);
        }
    }

    /**
     * 필터 필드 조합을 생성합니다.
     */
    private FilterFieldCombination createFilterFieldCombination(SearchFilter filter, List<SearchFilterField> combination) {
        // 조합 생성
        return FilterFieldCombination.builder()
            .filterId(filter.getId())
            .filterName(filter.getName())
            .fieldIds(FilterCombinationUtils.createFieldIdsString(combination))
            .fieldNames(FilterCombinationUtils.createFieldNamesString(combination))
            .build();
    }
} 