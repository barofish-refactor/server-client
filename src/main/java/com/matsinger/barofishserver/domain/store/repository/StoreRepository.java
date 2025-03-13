package com.matsinger.barofishserver.domain.store.repository;

import com.matsinger.barofishserver.domain.store.domain.StoreState;
import com.matsinger.barofishserver.domain.store.domain.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Integer>, JpaSpecificationExecutor<Store> {

    Optional<Store> findByLoginId(String loginId);

    Page<Store> findAllByStateEquals(StoreState state, PageRequest pageRequest);

    List<Store> findAllByStateAndStoreInfo_IsReliableTrue(StoreState state);

}
