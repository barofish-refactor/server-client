package com.matsinger.barofishserver.domain.store.application;

import com.matsinger.barofishserver.domain.store.domain.StoreSummary;
import com.matsinger.barofishserver.domain.store.repository.StoreSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreSummaryQueryService {
    private final StoreSummaryRepository storeSummaryRepository;

    public Map<Integer, StoreSummary> getStoreSummaryMapByStoreIds(List<Integer> storeIds) {
        List<StoreSummary> storeSummaries = storeSummaryRepository.findAllByStoreIdIn(storeIds);

        return storeSummaries.stream()
                .collect(Collectors.toMap(StoreSummary::getStoreId, Function.identity()));
    }
}
