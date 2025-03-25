package com.matsinger.barofishserver.domain.product.filter.repository;

import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterCombination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryFilterCombinationRepository extends JpaRepository<CategoryFilterCombination, Long> {
    List<CategoryFilterCombination> findByCategoryId(Integer categoryId);
    void deleteByCategoryId(Integer categoryId);
} 