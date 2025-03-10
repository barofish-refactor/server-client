package com.matsinger.barofishserver.domain.store.application;

import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.domain.store.repository.StoreInfoRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreInfoQueryService {

    private final StoreInfoRepository storeInfoRepository;

    public StoreInfo findByStoreId(Integer storeId) {
        return storeInfoRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessException("스토어 정보를 찾을 수 없습니다."));
    }

    public List<StoreInfo> findByStoreIds(List<Integer> storeIds) {
        
        return storeInfoRepository.findAllById(storeIds);
    }
}
