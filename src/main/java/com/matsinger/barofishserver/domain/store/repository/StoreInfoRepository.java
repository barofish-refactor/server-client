package com.matsinger.barofishserver.domain.store.repository;

import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface StoreInfoRepository extends JpaRepository<StoreInfo, Integer> {
    Optional<StoreInfo> findByName(String name);

    List<StoreInfo> findAllByStoreIdIn(List<Integer> storeIds);

    @Query(value = "SELECT si.*\n" +
            "FROM store_info si\n" +
            "         JOIN store s ON s.id = si.store_id\n" +
            "WHERE s.state = 'ACTIVE'\n" +
            "AND INSTR(si.name, :keyword) > 0\n" +
            "ORDER BY s.join_at DESC ", nativeQuery = true)
    List<StoreInfo> selectRecommendStoreWithJoinAt(Pageable pageable, @Param("keyword") String keyword);

    @Query(value = "SELECT si.*\n" +
            "FROM store_info si\n" +
            "         JOIN store s ON s.id = si.store_id\n" +
            "WHERE si.is_reliable = TRUE AND si.is_deleted = FALSE\n" +
            "AND s.state = 'ACTIVE'", nativeQuery = true)
    List<StoreInfo> findAllByIsReliableTrueAndIsDeletedFalse();

    @Query(value = "SELECT si.*\n" +
            "FROM store_info si\n" +
            "         JOIN store s ON s.id = si.store_id\n" +
            "         LEFT JOIN store_scrap ss ON s.id = ss.store_id\n" +
            "WHERE s.state = 'ACTIVE'\n" +
            "AND INSTR(si.name, :keyword) > 0\n" +
            "GROUP BY si.store_id\n" +
            "ORDER BY COUNT( * ) DESC\n", nativeQuery = true)
    List<StoreInfo> selectRecommendStoreWithScrap(Pageable pageable, @Param("keyword") String keyword);

    @Query(value = "SELECT si.*\n" +
            "FROM store_info si\n" +
            "         JOIN store s ON s.id = si.store_id\n" +
            "         LEFT JOIN review r ON s.id = r.store_id\n" +
            "WHERE s.state = 'ACTIVE'\n" +
            "AND INSTR(si.name, :keyword) > 0\n" +
            "GROUP BY si.store_id\n" +
            "ORDER BY COUNT( r.store_id ) DESC\n", nativeQuery = true)
    List<StoreInfo> selectRecommendStoreWithReview(Pageable pageable, @Param("keyword") String keyword);

    @Query(value = "SELECT si.*\n" +
            "FROM store_info si\n" +
            "         JOIN store s ON s.id = si.store_id\n" +
            "         LEFT JOIN product p ON s.id = p.store_id\n" +
            "         LEFT JOIN order_product_info opi ON p.id = opi.product_id\n" +
            "         LEFT JOIN orders o ON opi.order_id = o.id\n" +
            "WHERE s.state = 'ACTIVE'\n" +
            "AND INSTR(si.name, :keyword) > 0\n" +
            "GROUP BY si.store_id\n" +
            "ORDER BY COUNT( CASE WHEN o.state = 'FINAL_CONFIRM' THEN 1 END ) DESC\n", nativeQuery = true)
    List<StoreInfo> selectRecommendStoreWithOrder(Pageable pageable, @Param("keyword") String keyword);

    @Query(value = "SELECT si.*\n" +
            "FROM store_info si\n" +
            "WHERE si.is_reliable = TRUE\n" +
            "ORDER BY RAND( );", nativeQuery = true)
    List<StoreInfo> selectReliableStoreRandomOrder();

    Optional<StoreInfo> findByStoreId(Integer storeId);
}

