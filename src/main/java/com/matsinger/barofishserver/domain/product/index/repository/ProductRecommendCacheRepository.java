package com.matsinger.barofishserver.domain.product.index.repository;

import com.matsinger.barofishserver.domain.product.index.domain.ProductRecommendCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRecommendCacheRepository extends JpaRepository<ProductRecommendCache, Integer> {
    boolean existsByCategoryIdAndSubCategoryId(Integer pCategoryId, Integer cCategoryId);
}