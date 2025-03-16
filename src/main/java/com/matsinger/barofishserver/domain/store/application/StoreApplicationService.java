package com.matsinger.barofishserver.domain.store.application;

import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.dto.ReviewProductInfoResponse;
import com.matsinger.barofishserver.domain.product.dto.StoreReviewProductInfo;
import com.matsinger.barofishserver.domain.store.domain.DailyStore;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.domain.store.domain.StoreScrap;
import com.matsinger.barofishserver.domain.store.dto.SimpleStore;
import com.matsinger.barofishserver.domain.store.repository.StoreScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreApplicationService {
    private final DailyStoreService dailyStoreService;
    private final StoreScrapRepository storeScrapRepository;
    private final ProductService productService;

    public List<SimpleStore> selectRandomReliableStores(Integer userId, PageRequest pageRequest) {
        Page<DailyStore> reliableStores = Optional.of(dailyStoreService.getTodayReliableStores(pageRequest))
                .filter(stores -> !stores.isEmpty())
                .orElseGet(() -> {
                    dailyStoreService.refreshDailyStores();
                    return dailyStoreService.getTodayReliableStores(pageRequest);
                });

        List<Integer> storeIds = reliableStores.stream()
                .map(DailyStore::getStoreId)
                .toList();

        Map<Integer, Boolean> likeMap = new HashMap<>();
        if (userId != null) {
            List<StoreScrap> scrapedStores = storeScrapRepository.findByUserIdAndStoreIdIn(userId, storeIds);
            for (StoreScrap scrap : scrapedStores) {
                likeMap.put(scrap.getStoreId(), true);
            }
        }

        ReviewProductInfoResponse reviewAndProductInfos = productService.getReviewAndProductInfos(storeIds);

        return reliableStores.getContent().stream()
                .map(dailyStore -> createSimpleStore(
                        dailyStore,
                        likeMap.getOrDefault(dailyStore.getStoreId(), false),
                        reviewAndProductInfos
                ))
                .collect(Collectors.toList());
    }

    private SimpleStore createSimpleStore(DailyStore dailyStore, boolean isLike, ReviewProductInfoResponse reviewProductInfo) {
        Integer storeId = dailyStore.getStoreId();
        StoreInfo storeInfo = dailyStore.getStoreInfo();

        StoreReviewProductInfo info = reviewProductInfo.getStoreInfo(storeId);

        return SimpleStore.builder()
                .storeId(storeId)
                .backgroundImage(storeInfo.getbackgroundImage())
                .profileImage(storeInfo.getProfileImage())
                .name(storeInfo.getName())
                .location(storeInfo.getLocation())
                .isReliable(storeInfo.getIsReliable())
                .keyword(storeInfo.getKeyword().split(","))
                .visitNote(storeInfo.getVisitNote())
                .refundDeliverFee(storeInfo.getRefundDeliverFee())
                .oneLineDescription(storeInfo.getOneLineDescription())
                .isLike(isLike)
                .reviewStatistic(info.getReviewStatistics())
                .reviewCount(info.getReviewCount())
                .productCount(info.getProductCount())
                .deliverCompany(storeInfo.getDeliverCompany())
                .minStorePrice(storeInfo.getMinStorePrice())
                .deliveryFee(storeInfo.getDeliveryFee())
                .isConditional(storeInfo.getIsConditional())
                .build();
    }
}
