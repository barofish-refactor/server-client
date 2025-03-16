package com.matsinger.barofishserver.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewProductInfoResponse {
    private Map<Integer, StoreReviewProductInfo> storeInfos;
    
    /**
     * 스토어 ID로 리뷰 및 상품 정보를 조회합니다.
     * 정보가 없는 경우 기본값을 가진 객체를 반환합니다.
     */
    public StoreReviewProductInfo getStoreInfo(Integer storeId) {
        return storeInfos.getOrDefault(storeId, StoreReviewProductInfo.builder()
                .reviewStatistics(Collections.emptyList())
                .reviewCount(0)
                .productCount(0)
                .build());
    }
} 