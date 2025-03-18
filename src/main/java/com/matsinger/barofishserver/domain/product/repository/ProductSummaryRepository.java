package com.matsinger.barofishserver.domain.product.repository;

import com.matsinger.barofishserver.domain.product.domain.ProductSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ProductSummaryRepository extends JpaRepository<ProductSummary, Integer> {
    
    @Query("SELECT ps FROM ProductSummary ps WHERE ps.deleted = false AND ps.productId IN :productIds")
    List<ProductSummary> findByProductIds(@Param("productIds") List<Integer> productIds);
} 