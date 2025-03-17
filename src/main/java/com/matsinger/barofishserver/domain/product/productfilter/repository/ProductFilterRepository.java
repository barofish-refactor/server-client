package com.matsinger.barofishserver.domain.product.productfilter.repository;

import com.matsinger.barofishserver.domain.product.productfilter.domain.ProductFilterValue;
import com.matsinger.barofishserver.domain.product.productfilter.domain.ProductFilterValueId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductFilterRepository extends JpaRepository<ProductFilterValue, ProductFilterValueId> {
    void deleteAllByCompareFilterId(Integer compareFilterId);

    List<ProductFilterValue> findAllByProductId(Integer productId);

    void deleteAllByProductId(Integer productId);
    
    /**
     * 여러 상품 ID에 해당하는 필터 값을 조회합니다.
     *
     * @param productIds 상품 ID 목록
     * @return 필터 값 목록
     */
    List<ProductFilterValue> findAllByProductIdIn(List<Integer> productIds);
}
