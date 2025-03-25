package com.matsinger.barofishserver.domain.product.filter.repository;

import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryFilterRepository extends JpaRepository<CategoryFilter, Long> {
    List<CategoryFilter> findByCategoryId(Integer categoryId);
    void deleteByCategoryId(Integer categoryId);
} 