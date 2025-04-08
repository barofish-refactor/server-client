package com.matsinger.barofishserver.domain.product.filter.repository;

import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterProducts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryFilterProductsRepository extends JpaRepository<CategoryFilterProducts, String> {
    boolean existsByCategoryIdAndSubCategoryIdAndFilterIdAndFieldId(Integer pCategoryId, Integer cCategoryId, Integer filterId, Integer fieldId);
}