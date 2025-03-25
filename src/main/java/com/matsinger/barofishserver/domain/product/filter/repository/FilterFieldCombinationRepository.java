package com.matsinger.barofishserver.domain.product.filter.repository;

import com.matsinger.barofishserver.domain.product.filter.domain.FilterFieldCombination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilterFieldCombinationRepository extends JpaRepository<FilterFieldCombination, Long> {
    List<FilterFieldCombination> findByFilterId(Integer filterId);
    void deleteByFilterId(Integer filterId);
} 