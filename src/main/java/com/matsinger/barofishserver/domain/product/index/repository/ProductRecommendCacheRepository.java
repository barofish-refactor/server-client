package com.matsinger.barofishserver.domain.product.index.repository;

import com.matsinger.barofishserver.domain.product.index.domain.ProductRecommendCache;
import com.matsinger.barofishserver.domain.product.index.domain.ProductRecommendCacheId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRecommendCacheRepository extends JpaRepository<ProductRecommendCache, ProductRecommendCacheId> {
} 