package com.matsinger.barofishserver.domain.store.application;

import com.matsinger.barofishserver.domain.store.domain.DailyStore;
import com.matsinger.barofishserver.domain.store.domain.Store;
import com.matsinger.barofishserver.domain.store.repository.DailyStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DailyStoreService {
    private final DailyStoreRepository dailyStoreRepository;
    private final StoreService storeService;

    @Transactional
    public void refreshDailyStores() {
        // 오늘 생성된 active한 리스트가 있는지 확인
        Optional<DailyStore> latestStore = dailyStoreRepository.findLatestActive();
        if (latestStore.isPresent() && latestStore.get().isCreatedToday()) {
            return; // 오늘 이미 생성된 리스트가 있다면 종료
        }

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<DailyStore> oldStores = dailyStoreRepository.findAllUpdatedYesterday(yesterday);
        
        // 이전 리스트들을 삭제 처리
        oldStores.forEach(DailyStore::markAsDeleted);

        // 신뢰도 있는 스토어 목록을 가져와서 랜덤 정렬
        List<Store> reliableStores = storeService.selectReliableStore();
        Collections.shuffle(reliableStores);
        
        // 새로운 데일리 스토어 생성
        List<DailyStore> newStores = reliableStores.stream()
            .map(DailyStore::createFrom)
            .toList();
        
        dailyStoreRepository.saveAll(newStores);
    }

    public Page<DailyStore> getReliableStores(Pageable pageable) {
        Page<DailyStore> dailyStores = dailyStoreRepository.findByDeletedIsFalse(pageable);
        if (dailyStores.isEmpty()) {
            refreshDailyStores();
        }
        return dailyStoreRepository.findByDeletedIsFalse(pageable);
    }
}
