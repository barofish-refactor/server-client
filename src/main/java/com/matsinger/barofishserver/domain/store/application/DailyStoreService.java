package com.matsinger.barofishserver.domain.store.application;

import com.matsinger.barofishserver.domain.store.domain.DailyStore;
import com.matsinger.barofishserver.domain.store.domain.Store;
import com.matsinger.barofishserver.domain.store.repository.DailyStoreRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DailyStoreService {
    private final DailyStoreRepository dailyStoreRepository;
    private final StoreService storeService;

    @PersistenceContext
    private final EntityManager em;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshDailyStores() {
        if (dailyStoreExists()) {
            return;
        }

        List<DailyStore> oldStores = dailyStoreRepository.findAllDeletedFalse();

        oldStores.forEach(DailyStore::markAsDeleted);
        dailyStoreRepository.saveAll(oldStores);

        List<Store> reliableStores = storeService.selectReliableStore();
        Collections.shuffle(reliableStores);

        List<DailyStore> newStores = reliableStores.stream()
            .map(DailyStore::createFrom)
            .toList();

        dailyStoreRepository.saveAll(newStores);
        em.flush();
        em.clear();
    }

    private boolean dailyStoreExists() {
        Optional<DailyStore> latestStore = dailyStoreRepository.findLatestActive();
        if (latestStore.isPresent()) {
            DailyStore dailyStore = latestStore.get();
            return dailyStore.isToday(LocalDateTime.now());
        }
        return false;
    }

    public Page<DailyStore> getTodayReliableStores(Pageable pageable) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        return dailyStoreRepository.findByCreatedAtTodayAndDeletedIsFalse(pageable, today);
    }
}
