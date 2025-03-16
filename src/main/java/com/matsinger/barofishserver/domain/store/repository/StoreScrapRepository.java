package com.matsinger.barofishserver.domain.store.repository;

import com.matsinger.barofishserver.domain.store.domain.StoreScrap;
import com.matsinger.barofishserver.domain.store.domain.StoreScrapId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreScrapRepository extends JpaRepository<StoreScrap, StoreScrapId> {
    List<StoreScrap> findByUserId(Integer userId);

    Boolean existsByStoreIdAndUserId(Integer storeId, Integer userId);

    List<StoreScrap> findByUserIdAndStoreIdIn(Integer userId, List<Integer> storeIds);
}
