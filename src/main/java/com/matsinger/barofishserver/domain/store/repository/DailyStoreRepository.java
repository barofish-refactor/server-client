package com.matsinger.barofishserver.domain.store.repository;

import com.matsinger.barofishserver.domain.store.domain.DailyStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyStoreRepository extends JpaRepository<DailyStore, Integer> {
    @Query("SELECT d FROM DailyStore d WHERE d.deleted = false")
    List<DailyStore> findAllDeletedFalse();

    @Query("SELECT d FROM DailyStore d WHERE d.deleted = false ORDER BY d.createdAt DESC LIMIT 1")
    Optional<DailyStore> findLatestActive();

    @Query("SELECT d FROM DailyStore d " +
            "WHERE d.deleted = false " +
            "AND d.createdAt > :today")
    Page<DailyStore> findByCreatedAtTodayAndDeletedIsFalse(Pageable pageable, LocalDateTime today);

    @Query("SELECT COUNT(d) FROM DailyStore d " +
            "WHERE d.deleted = false " +
            "AND d.createdAt > :today")
    Long countByCreatedAtTodayAndDeletedIsFalse(LocalDateTime today);
}
