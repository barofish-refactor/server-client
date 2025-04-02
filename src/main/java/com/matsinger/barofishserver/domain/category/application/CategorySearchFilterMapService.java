package com.matsinger.barofishserver.domain.category.application;

import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.category.domain.CategorySearchFilterMap;
import com.matsinger.barofishserver.domain.category.repository.CategoryRepository;
import com.matsinger.barofishserver.domain.category.repository.CategorySearchFilterMapRepository;
import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilter;
import com.matsinger.barofishserver.domain.searchFilter.repository.SearchFilterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 카테고리와 검색 필터 간의 매핑을 관리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategorySearchFilterMapService {

    private final CategorySearchFilterMapRepository categorySearchFilterMapRepository;

    /**
     * 카테고리에 연결된 모든 검색 필터 ID 조회
     */
    @Transactional(readOnly = true)
    public List<CategorySearchFilterMap> getSearchFilterIdsByCategory(Integer categoryId) {
        return categorySearchFilterMapRepository.findByCategoryId(categoryId);
    }
}
