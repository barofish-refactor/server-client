package com.matsinger.barofishserver.domain.product.index.repository;

import com.matsinger.barofishserver.domain.product.index.domain.ProductRecommend;
import com.matsinger.barofishserver.domain.product.index.domain.ProductRecommendId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRecommendRepository extends JpaRepository<ProductRecommend, ProductRecommendId> {
    @Query(value = "SELECT product_id FROM product_recommend WHERE category_id = :categoryId order by weight DESC", nativeQuery = true)
    List<Integer> findProductIdsByCategoryIdOrderByWeightDesc(@Param("categoryId") int categoryId);

    @Query(value = "SELECT product_id FROM product_recommend WHERE category_id IN :categoryIds ORDER BY weight DESC", nativeQuery = true)
    List<Integer> findProductIdsByCategoryIdsOrderByWeightDesc(@Param("categoryIds") List<Integer> categoryIds);
}