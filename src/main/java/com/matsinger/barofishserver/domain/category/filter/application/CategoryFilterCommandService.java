package com.matsinger.barofishserver.domain.category.filter.application;

import com.matsinger.barofishserver.domain.category.filter.domain.CategoryFilterId;
import com.matsinger.barofishserver.domain.category.filter.domain.CategoryFilterMap;
import com.matsinger.barofishserver.domain.category.filter.repository.CategoryFilterMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class CategoryFilterCommandService {
    private final CategoryFilterMapRepository categoryFilterRepository;

    public void addCategoryFilterMap(CategoryFilterMap filter) {
        categoryFilterRepository.save(filter);
    }

    public void deleteCategoryFilterMap(CategoryFilterId id) {
        categoryFilterRepository.deleteById(id);
    }
}
