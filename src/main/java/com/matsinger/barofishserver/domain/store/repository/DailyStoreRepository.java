package com.matsinger.barofishserver.domain.store.repository;

import com.matsinger.barofishserver.domain.store.domain.DailyStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyStoreRepository extends JpaRepository<DailyStore, Integer> {
    @Query("SELECT d FROM DailyStore d WHERE DATE(d.updatedAt) = DATE(:yesterday)")
    List<DailyStore> findAllUpdatedYesterday(@Param("yesterday") LocalDateTime yesterday);

    @Query("SELECT d FROM DailyStore d WHERE d.deleted = false ORDER BY d.createdAt DESC LIMIT 1")
    Optional<DailyStore> findLatestActive();
}
