package com.matsinger.barofishserver.domain.category.repository;

import com.matsinger.barofishserver.domain.category.domain.CategorySearchFilterMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategorySearchFilterMapRepository extends JpaRepository<CategorySearchFilterMap, Long> {

    @Query("SELECT csm FROM CategorySearchFilterMap csm WHERE csm.category.id = :categoryId")
    List<CategorySearchFilterMap> findByCategoryId(@Param("categoryId") Integer categoryId);
} 