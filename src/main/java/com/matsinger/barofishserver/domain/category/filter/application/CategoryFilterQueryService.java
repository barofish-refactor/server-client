package com.matsinger.barofishserver.domain.category.filter.application;

import com.matsinger.barofishserver.domain.category.filter.domain.CategoryFilterMap;
import com.matsinger.barofishserver.domain.category.filter.repository.CategoryFilterMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
@RequiredArgsConstructor
@Slf4j
@Service
public class CategoryFilterQueryService {
    private final CategoryFilterMapRepository categoryFilterRepository;

    public List<Integer> selectCompareFilterIdList(Integer categoryId) {
        return categoryFilterRepository.findAllByCategoryId(categoryId).stream().map(CategoryFilterMap::getCompareFilterId).toList();
    }
}
