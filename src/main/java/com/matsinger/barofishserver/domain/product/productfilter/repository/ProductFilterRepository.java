package com.matsinger.barofishserver.domain.product.productfilter.repository;

import com.matsinger.barofishserver.domain.product.productfilter.domain.ProductFilterValue;
import com.matsinger.barofishserver.domain.product.productfilter.domain.ProductFilterValueId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductFilterRepository extends JpaRepository<ProductFilterValue, ProductFilterValueId> {
    void deleteAllByCompareFilterId(Integer compareFilterId);

    List<ProductFilterValue> findAllByProductId(Integer productId);

    void deleteAllByProductId(Integer productId);

    List<ProductFilterValue> findAllByProductIdIn(List<Integer> productIds);
}
