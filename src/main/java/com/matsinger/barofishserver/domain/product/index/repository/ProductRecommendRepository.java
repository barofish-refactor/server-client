package com.matsinger.barofishserver.domain.product.index.repository;

import com.matsinger.barofishserver.domain.product.index.domain.ProductRecommend;
import com.matsinger.barofishserver.domain.product.index.domain.ProductRecommendId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRecommendRepository extends JpaRepository<ProductRecommend, ProductRecommendId> {
} 