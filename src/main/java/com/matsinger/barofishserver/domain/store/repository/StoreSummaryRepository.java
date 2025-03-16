package com.matsinger.barofishserver.domain.store.repository;

import com.matsinger.barofishserver.domain.store.domain.StoreSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreSummaryRepository extends JpaRepository<StoreSummary, Integer> {
    List<StoreSummary> findAllByStoreIdIn(List<Integer> storeIds);
}
