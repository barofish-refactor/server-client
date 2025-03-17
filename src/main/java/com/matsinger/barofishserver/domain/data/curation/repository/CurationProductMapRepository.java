package com.matsinger.barofishserver.domain.data.curation.repository;

import com.matsinger.barofishserver.domain.data.curation.domain.CurationProductMap;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurationProductMapRepository extends JpaRepository<CurationProductMap, Integer> {
    List<CurationProductMap> findAllByCuration_Id(Integer curationId);
    List<CurationProductMap> findAllByCuration_Id(Integer curationId, Pageable pageable);

    @Query(value = "delete from curation_product_map WHERE curation_id = :curationId and product_id in (:productIds);", nativeQuery = true)
    void deleteAllByProductIdIn(@Param("curationId") Integer curationId, @Param("productIds") List<Integer> productIds);

    Boolean existsByCurationIdAndProductId(Integer curationId, Integer productId);

    void deleteAllByProductId(Integer productId);
}
