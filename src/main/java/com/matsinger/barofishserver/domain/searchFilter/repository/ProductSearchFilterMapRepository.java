package com.matsinger.barofishserver.domain.searchFilter.repository;

import com.matsinger.barofishserver.domain.searchFilter.domain.ProductSearchFilterMap;
import com.matsinger.barofishserver.domain.searchFilter.domain.ProductSearchFilterMapId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductSearchFilterMapRepository
        extends JpaRepository<ProductSearchFilterMap, ProductSearchFilterMapId> {
    boolean existsByFieldIdAndProductId(Integer fieldId, Integer productId);

    void deleteAllByFieldIdIn(List<Integer> ids);

    void deleteAllByProductId(Integer id);

    List<ProductSearchFilterMap> findAllByProductId(Integer productId);

    @Query("SELECT DISTINCT p.id FROM Product p JOIN ProductSearchFilterMap psm ON p.id = psm.productId WHERE psm.fieldId IN :fieldCombination")
    List<Integer> findProductIdsByFieldIdIn(List<Integer> fieldCombination);
}
