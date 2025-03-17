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
    
    /**
     * 여러 큐레이션 ID에 해당하는 모든 큐레이션-상품 맵핑을 조회합니다.
     *
     * @param curationIds 큐레이션 ID 목록
     * @return 큐레이션-상품 맵핑 목록
     */
    List<CurationProductMap> findAllByCuration_IdIn(List<Integer> curationIds);
    
    /**
     * 여러 큐레이션 ID에 해당하는 큐레이션-상품 맵핑을 페이징 처리하여 조회합니다.
     * 각 큐레이션별로 최대 limit개의 상품을 조회합니다.
     *
     * @param curationIds 큐레이션 ID 목록
     * @param limit 각 큐레이션별 최대 상품 수
     * @return 큐레이션-상품 맵핑 목록
     */
    @Query(value = "SELECT cpm.* FROM curation_product_map cpm " +
            "JOIN (SELECT cpm2.curation_id, cpm2.product_id, " +
            "ROW_NUMBER() OVER (PARTITION BY cpm2.curation_id ORDER BY cpm2.id) as rn " +
            "FROM curation_product_map cpm2 " +
            "WHERE cpm2.curation_id IN (:curationIds)) ranked " +
            "ON cpm.curation_id = ranked.curation_id AND cpm.product_id = ranked.product_id " +
            "WHERE ranked.rn <= :limit", nativeQuery = true)
    List<CurationProductMap> findAllByCurationIdInWithPaging(@Param("curationIds") List<Integer> curationIds, @Param("limit") int limit);
    
    @Query(value = "delete from curation_product_map WHERE curation_id = :curationId and product_id in (:productIds);", nativeQuery = true)
    void deleteAllByProductIdIn(@Param("curationId") Integer curationId, @Param("productIds") List<Integer> productIds);

    Boolean existsByCurationIdAndProductId(Integer curationId, Integer productId);

    void deleteAllByProductId(Integer productId);
}
