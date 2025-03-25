package com.matsinger.barofishserver.domain.product.filter.repository;

import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterFieldCombination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryFilterFieldCombinationRepository extends JpaRepository<CategoryFilterFieldCombination, Long> {
    List<CategoryFilterFieldCombination> findByCategoryId(Integer categoryId);
    void deleteByCategoryId(Integer categoryId);
} 