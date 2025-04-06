package com.matsinger.barofishserver.domain.product.filter.repository;

import com.matsinger.barofishserver.domain.product.filter.domain.FilterProductCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FilterProductCacheRepository extends JpaRepository<FilterProductCache, String> {
    boolean existsByFilterIdAndFieldIds(Integer filterId, String fieldList);
}