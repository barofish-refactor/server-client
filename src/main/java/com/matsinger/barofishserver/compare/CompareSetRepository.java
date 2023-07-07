package com.matsinger.barofishserver.compare;

import com.matsinger.barofishserver.compare.obejct.CompareSet;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompareSetRepository extends JpaRepository<CompareSet, Integer> {
    List<CompareSet> findAllByUserId(Integer userId);

    @Query(value = "SELECT q1.setId as setId\n" +
            "FROM (SELECT ci.compare_set_id AS setId, GROUP_CONCAT( ci.product_id ORDER BY ci.product_id, '' ) AS compareSet\n" +
            "      FROM compare_item ci\n" +
            "      GROUP BY ci.compare_set_id) AS q1\n" +
            "GROUP BY q1.compareSet\n" +
            "ORDER BY COUNT( q1.setId ) DESC\n" +
            "LIMIT 5;", nativeQuery = true)
    List<Tuple> selectPopularCompareSetIdList();
}
