package com.matsinger.barofishserver.store;


import com.matsinger.barofishserver.product.ProductRepository;
import com.matsinger.barofishserver.review.ReviewRepository;
import com.matsinger.barofishserver.review.object.Review;
import com.matsinger.barofishserver.review.object.ReviewDto;
import com.matsinger.barofishserver.review.object.ReviewStatistic;
import com.matsinger.barofishserver.store.object.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class StoreService {

    private final StoreRepository storeRepository;
    private final ReviewRepository reviewRepository;
    private final StoreInfoRepository storeInfoRepository;
    private final ProductRepository productRepository;
    private final StoreScrapRepository storeScrapRepository;

    public StoreDto convert2Dto(Store store, Boolean isUser) {
        StoreInfo storeInfo = selectStoreInfo(store.getId());
        StoreAdditionalDto
                additionalDto =
                isUser ? null : StoreAdditionalDto.builder().settlementRate(storeInfo.getSettlementRate()).bankName(
                        storeInfo.getBankName()).bankHolder(storeInfo.getBankHolder()).bankAccount(storeInfo.getBankAccount()).representativeName(
                        storeInfo.getRepresentativeName()).companyId(storeInfo.getCompanyId()).businessType(storeInfo.getBusinessType()).mosRegistrationNumber(
                        storeInfo.getMosRegistrationNumber()).businessAddress(storeInfo.getBusinessAddress()).postalCode(
                        storeInfo.getPostalCode()).lotNumberAddress(storeInfo.getLotNumberAddress()).streetNameAddress(
                        storeInfo.getStreetNameAddress()).addressDetail(storeInfo.getAddressDetail()).tel(storeInfo.getTel()).email(
                        storeInfo.getEmail()).faxNumber(storeInfo.getFaxNumber()).mosRegistration(storeInfo.getMosRegistration()).businessRegistration(
                        storeInfo.getBusinessRegistration()).bankAccountCopy(storeInfo.getBankAccountCopy()).build();
        return StoreDto.builder().id(store.getId()).state(store.getState()).loginId(store.getLoginId()).joinAt(store.getJoinAt()).backgroundImage(
                storeInfo.getBackgroudImage()).profileImage(storeInfo.getProfileImage()).name(storeInfo.getName()).location(
                storeInfo.getLocation()).visitNote(storeInfo.getVisitNote()).deliverFeeType(storeInfo.getDeliverFeeType()).deliverFee(
                storeInfo.getDeliverFee()).minOrderPrice(storeInfo.getMinOrderPrice()).keyword(storeInfo.getKeyword().split(
                ",")).oneLineDescription(storeInfo.getOneLineDescription()).additionalData(additionalDto).build();
    }

    public SimpleStore convert2SimpleDto(StoreInfo storeInfo, Integer userId) {
        Boolean isLike = userId != null ? checkLikeStore(storeInfo.getStoreId(), userId) : false;
        List<ReviewStatistic>
                reviewStatistics =
                reviewRepository.selectReviewStatisticsWithStoreId(storeInfo.getStoreId()).stream().map(tuple -> ReviewStatistic.builder().key(
                        tuple.get("evaluation").toString()).count(Integer.valueOf(tuple.get("count").toString())).build()).toList();
        List<ReviewDto>
                reviewDtos =
                reviewRepository.findAllByStoreId(storeInfo.getStoreId(),
                        PageRequest.of(0, 20)).getContent().stream().map(Review::convert2Dto).toList();
        Integer reviewCount = reviewRepository.countAllByStoreId(storeInfo.getStoreId());
        Integer productCount = productRepository.countAllByStoreId(storeInfo.getStoreId());
        SimpleStore
                simpleStore =
                SimpleStore.builder().storeId(storeInfo.getStoreId()).backgroundImage(storeInfo.getBackgroudImage()).profileImage(
                        storeInfo.getProfileImage()).name(storeInfo.getName()).location(storeInfo.getLocation()).keyword(
                        storeInfo.getKeyword().split(",")).visitNote(storeInfo.getVisitNote()).deliverFeeType(storeInfo.getDeliverFeeType()).deliverFee(
                        storeInfo.getDeliverFee()).minOrderPrice(storeInfo.getMinOrderPrice()).oneLineDescription(
                        storeInfo.getOneLineDescription()).isLike(isLike).reviewStatistic(reviewStatistics).reviews(
                        reviewDtos).reviewCount(reviewCount).productCount(productCount).build();
        return simpleStore;
    }

    public Optional<Store> selectStoreOptional(Integer id) {
        try {
            return storeRepository.findById(id);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public List<StoreInfo> selectRecommendStore(StoreRecommendType type, Integer page, Integer take, String keyword) {
        List<StoreInfo> infos = new ArrayList<>();
        switch (type) {
            case RECENT:
                infos =
                        storeInfoRepository.selectRecommendStoreWithJoinAt(Pageable.ofSize(take).withPage(page),
                                keyword);
                break;
            case BOOKMARK:
                infos =
                        storeInfoRepository.selectRecommendStoreWithScrap(Pageable.ofSize(take).withPage(page),
                                keyword);
                break;
            case ORDER:
                infos =
                        storeInfoRepository.selectRecommendStoreWithOrder(Pageable.ofSize(take).withPage(page),
                                keyword);
                break;
            case REVIEW:
                infos =
                        storeInfoRepository.selectRecommendStoreWithReview(Pageable.ofSize(take).withPage(page),
                                keyword);
                break;
        }
        return infos;
    }


    public void updateStores(List<Store> stores) {
        storeRepository.saveAll(stores);
    }

    public Store selectStore(Integer id) {
        return storeRepository.findById(id).orElseThrow(() -> {
            throw new Error("상점 정보를 찾을 수 없습니다.");
        });
    }

    public StoreInfo selectStoreInfo(Integer id) {
        return storeInfoRepository.findById(id).orElseThrow(() -> {
            throw new Error("상점 정보를 찾을 수 없습니다.");
        });
    }

    public Page<Store> selectStoreList(Boolean isAdmin, PageRequest pageRequest, Specification<Store> spec) {
        if (isAdmin) {
            return storeRepository.findAll(spec, pageRequest);
        } else {
            return storeRepository.findAllByStateEquals(StoreState.ACTIVE, pageRequest);
        }
    }

    public Store selectStoreByLoginId(String loginId) {
        Store store = storeRepository.findByLoginId(loginId).orElseThrow(() -> {
            throw new Error("스토어 정보를 찾을 수 없습니다.");
        });
        return store;
    }

    public Optional<Store> selectOptionalStoreByLoginId(String loginId) {
        return storeRepository.findByLoginId(loginId);
    }

    public List<StoreInfo> selectStoreInfoList() {
        return storeInfoRepository.findAll();
    }

    public Boolean checkStoreLoginIdValid(String loginId) {
        try {
            Optional<Store> store = storeRepository.findByLoginId(loginId);
            return store.isEmpty();
        } catch (Error e) {
            return true;
        }
    }

    public Store addStore(Store data) {
        return storeRepository.save(data);
    }

    public StoreInfo addStoreInfo(StoreInfo data) {
        return storeInfoRepository.save(data);
    }

    public Store updateStore(Store data) {
        return storeRepository.save(data);
    }

    public StoreInfo updateStoreInfo(StoreInfo data) {
        return storeInfoRepository.save(data);
    }

    public List<StoreInfo> selectScrapedStore(Integer userId) {
        List<StoreScrap> storeScraps = storeScrapRepository.findByUserId(userId);
        List<Integer> storeIds = new ArrayList<>();
        for (StoreScrap storeScrap : storeScraps) {
            storeIds.add(storeScrap.getStoreId());
        }
        List<StoreInfo> storeInfos = storeInfoRepository.findAllByStoreIdIn(storeIds);
        return storeInfos;
    }

    public void deleteScrapedStore(Integer userId, List<Integer> storeIds) {
        List<StoreScrap> storeScraps = new ArrayList<>();
        for (Integer storeId : storeIds) {
            storeScraps.add(StoreScrap.builder().storeId(storeId).userId(userId).build());
        }

        storeScrapRepository.deleteAll(storeScraps);
    }

    public void likeStore(Integer storeId, Integer userId) {
        storeScrapRepository.save(StoreScrap.builder().storeId(storeId).userId(userId).build());
    }

    public void unlikeStore(Integer storeId, Integer userId) {
        storeScrapRepository.deleteById(StoreScrapId.builder().storeId(storeId).userId(userId).build());
    }

    public Boolean checkLikeStore(Integer storeId, Integer userId) {
        return storeScrapRepository.existsByStoreIdAndUserId(storeId, userId);
    }

    public Integer getDeliverFee(StoreInfo storeInfo, Integer totalPrice) {
        switch (storeInfo.getDeliverFeeType()) {
            case FREE -> {
                return 0;
            }
            case FIX -> {
                return storeInfo.getDeliverFee();
            }
            case FREE_IF_OVER -> {
                if (totalPrice > storeInfo.getMinOrderPrice()) return 0;
                else return storeInfo.getDeliverFee();
            }
        }
        return 0;
    }
}
