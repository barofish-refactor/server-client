package com.matsinger.barofishserver.domain.basketProduct.repository;


import com.matsinger.barofishserver.domain.basketProduct.dto.BasketProductDtoV2;
import com.matsinger.barofishserver.domain.basketProduct.dto.BasketProductInfoDto;
import com.matsinger.barofishserver.domain.basketProduct.dto.BasketStoreDto;
import com.matsinger.barofishserver.domain.product.optionitem.dto.OptionItemDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.matsinger.barofishserver.domain.basketProduct.domain.QBasketProductInfo.basketProductInfo;
import static com.matsinger.barofishserver.domain.product.domain.QProduct.product;
import static com.matsinger.barofishserver.domain.product.option.domain.QOption.option;
import static com.matsinger.barofishserver.domain.product.optionitem.domain.QOptionItem.optionItem;
import static com.matsinger.barofishserver.domain.store.domain.QStore.store;
import static com.matsinger.barofishserver.domain.store.domain.QStoreInfo.storeInfo;

@Repository
@RequiredArgsConstructor
public class BasketQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager em;

    public List<BasketProductDtoV2> selectBasketProductInfos(Integer userId) {
        return queryFactory
                .select(Projections.fields(
                        BasketProductDtoV2.class,
                        basketProductInfo.id.as("id"),
                        basketProductInfo.amount.as("amount"),
                        product.deliverFeeType.as("deliverFeeType"),
                        product.minOrderPrice.as("minOrderPrice"),
                        storeInfo.isConditional.as("isConditional"),
                        storeInfo.minStorePrice.as("minStorePrice"),
                        product.deliverFee.as("deliveryFee"),
                        Projections.fields(
                                BasketStoreDto.class,
                                store.id.as("storeId"),
                                storeInfo.name.as("name"),
                                storeInfo.backgroundImage.as("backgroundImage"),
                                storeInfo.profileImage.as("profileImage"),
                                storeInfo.isConditional.as("isConditional2"),
                                storeInfo.minStorePrice.as("minStorePrice2"),
                                storeInfo.deliveryFee.as("deliveryFee2")
                        ).as("store"),
                        Projections.fields(
                                BasketProductInfoDto.class,
                                basketProductInfo.id.as("id2"),
                                product.id.as("productId"),
                                product.state.as("state"),
                                product.images.as("image"),
                                product.title.as("title"),
                                product.needTaxation.as("isNeedTaxation"),
                                optionItem.discountPrice.as("discountPrice"),
                                product.originPrice.as("originPrice"),
                                product.storeId.as("storeId2"),
                                product.minOrderPrice.as("minOrderPrice2"),
                                storeInfo.minStorePrice.as("minStorePrice3"),
                                product.deliverFeeType.as("deliverFeeType2")
                        ).as("product"),
                        Projections.fields(
                                OptionItemDto.class,
                                optionItem.id.as("id3"),
                                optionItem.option.id.as("optionId"),
                                optionItem.name.as("name2"),
                                optionItem.discountPrice.as("discountPrice2"),
                                optionItem.amount.as("amount2"),
                                optionItem.purchasePrice.as("purchasePrice"),
                                optionItem.originPrice.as("originPrice2"),
                                optionItem.deliverFee.as("deliveryFee3"),
                                optionItem.deliverBoxPerAmount.as("deliverBoxPerAmount"),
                                optionItem.maxAvailableAmount.as("maxAvailableAmount"),
                                product.pointRate.as("pointRate"),
                                product.minOrderPrice.as("minOrderPrice3")
                        ).as("option")
                ))
                .from(basketProductInfo)
                .leftJoin(product).on(basketProductInfo.productId.eq(product.id))
                .leftJoin(store).on(store.id.eq(basketProductInfo.storeId))
                .leftJoin(storeInfo).on(storeInfo.storeId.eq(basketProductInfo.storeId))
                .leftJoin(option).on(option.id.eq(basketProductInfo.optionId))
                .leftJoin(optionItem).on(optionItem.id.eq(basketProductInfo.optionItemId))
                .where(basketProductInfo.userId.eq(userId))
                .fetch();
    }

    @Transactional
    public void deleteAllBasketByUserIdAndOptionIds(int userId, List<Integer> optionItemIds) {
        queryFactory.delete(basketProductInfo)
                .where(basketProductInfo.userId.eq(userId)
                        .and(basketProductInfo.optionItemId.in(optionItemIds)))
                .execute();
        em.flush();
        em.clear();
    }

//    public List<BasketProductDtoV2> selectBasketProductInfos(Integer userId) {
//        List<Tuple> result = queryFactory
//                .select(basketProductInfo, product, store, storeInfo, option, optionItem)
//                .from(basketProductInfo)
//                .leftJoin(product).on(basketProductInfo.productId.eq(product.id))
//                .leftJoin(store).on(store.id.eq(basketProductInfo.storeId))
//                .leftJoin(storeInfo).on(storeInfo.storeId.eq(basketProductInfo.storeId))
//                .leftJoin(option).on(option.id.eq(basketProductInfo.optionId))
//                .leftJoin(optionItem).on(optionItem.id.eq(basketProductInfo.optionItemId))
//                .where(basketProductInfo.userId.eq(userId))
//                .fetch();
//
//        return result.stream()
//                .map(tuple -> {
//
//                    BasketProductDtoV2 basketProductDtoV2 = BasketProductDtoV2.builder()
//                            .id(tuple.get(basketProductInfo.id))
//                            .amount(tuple.get(basketProductInfo.amount))
//                            .deliverFeeType(tuple.get(product.deliverFeeType))
//                            .minOrderPrice(tuple.get(product.minOrderPrice))
//                            .isConditional(tuple.get(storeInfo.isConditional))
//                            .minStorePrice(tuple.get(storeInfo.minStorePrice))
//                            .deliveryFee(tuple.get(product.deliverFee))
//                            .build();
//
//                    basketProductDtoV2.setStore(
//                            BasketStoreDto.builder()
//                            .storeId(tuple.get(store.id))
//                            .name(tuple.get(storeInfo.name))
//                            .backgroundImage(tuple.get(storeInfo.backgroundImage))
//                            .profileImage(tuple.get(storeInfo.profileImage))
//                            .isConditional(tuple.get(storeInfo.isConditional))
//                            .minStorePrice(tuple.get(storeInfo.minStorePrice))
//                            .deliveryFee(tuple.get(storeInfo.deliveryFee))
//                            .build()
//                    );
//
//                    BasketProductInfoDto basketProductInfoDto = BasketProductInfoDto.builder()
//                            .id(tuple.get(basketProductInfo.id))
//                            .productId(tuple.get(basketProductInfo.productId))
//                            .state(tuple.get(product.state))
//                            .image(tuple.get(product.images))
//                            .title(tuple.get(product.title))
//                            .isNeedTaxation(tuple.get(product.needTaxation))
//                            .discountPrice(tuple.get(optionItem.discountPrice))
//                            .originPrice(tuple.get(product.originPrice))
//                            .storeId(tuple.get(product.storeId))
//                            .minOrderPrice(tuple.get(product.minOrderPrice))
//                            .minStorePrice(tuple.get(storeInfo.minStorePrice))
//                            .deliverFeeType(tuple.get(product.deliverFeeType))
//                            .build();
//
//                    basketProductInfoDto.convertImageUrlsToFirstUrl(); // convertImageUrlsToFirstUrl 메서드 적용
//                    basketProductDtoV2.setProduct(basketProductInfoDto);
//
//                    basketProductDtoV2.setOption(
//                            OptionItemDto.builder()
//                            .id(tuple.get(optionItem.id))
//                            .optionId(tuple.get(optionItem.optionId))
//                            .name(tuple.get(optionItem.name))
//                            .discountPrice(tuple.get(optionItem.discountPrice))
//                            .amount(tuple.get(optionItem.amount))
//                            .purchasePrice(tuple.get(optionItem.purchasePrice))
//                            .originPrice(tuple.get(optionItem.originPrice))
//                            .deliveryFee(tuple.get(optionItem.deliverFee))
//                            .deliverBoxPerAmount(tuple.get(optionItem.deliverBoxPerAmount))
//                            .maxAvailableAmount(tuple.get(optionItem.maxAvailableAmount))
//                            .pointRate(tuple.get(product.pointRate))
//                            .minOrderPrice(tuple.get(product.minOrderPrice))
//                            .build()
//                    );
//
//                    return basketProductDtoV2;
//                })
//                .toList();
//    }
}
