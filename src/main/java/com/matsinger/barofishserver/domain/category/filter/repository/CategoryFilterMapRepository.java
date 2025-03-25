package com.matsinger.barofishserver.domain.category.filter.repository;

import com.matsinger.barofishserver.domain.category.filter.domain.CategoryFilterId;
import com.matsinger.barofishserver.domain.category.filter.domain.CategoryFilterMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryFilterMapRepository extends JpaRepository<CategoryFilterMap, CategoryFilterId> {
    void deleteAllByCategoryId(Integer categoryId);

    void deleteAllByCompareFilterId(Integer compareFilterId);

    List<CategoryFilterMap> findAllByCategoryId(Integer categoryId);
}
