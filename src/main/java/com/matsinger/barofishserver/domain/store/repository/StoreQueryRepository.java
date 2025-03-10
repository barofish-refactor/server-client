package com.matsinger.barofishserver.domain.store.repository;

import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.domain.store.domain.StoreState;
import com.matsinger.barofishserver.domain.store.dto.StoreExcelInquiryDto;
import com.matsinger.barofishserver.domain.store.dto.StoreRecommendInquiryDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.matsinger.barofishserver.domain.deliver.domain.QDeliveryCompany.deliveryCompany;
import static com.matsinger.barofishserver.domain.product.domain.QProduct.product;
import static com.matsinger.barofishserver.domain.review.domain.QReview.review;
import static com.matsinger.barofishserver.domain.store.domain.QStore.store;
import static com.matsinger.barofishserver.domain.store.domain.QStoreInfo.storeInfo;
import static com.matsinger.barofishserver.domain.store.domain.QStoreScrap.storeScrap;
import static com.matsinger.barofishserver.order.domain.model.QOrderProductInfo.orderProductInfo;
import static com.matsinger.barofishserver.order.domain.model.QOrders.orders;

@Repository
@RequiredArgsConstructor
public class StoreQueryRepository {

    private final JPAQueryFactory queryFactory;


    public List<StoreExcelInquiryDto> getExcelDataByStoreIds(List<Integer> storeIds) {
        return queryFactory.select(Projections.fields(
                StoreExcelInquiryDto.class,
                storeInfo.name.as("storeName"),
                store.loginId.as("loginId"),
                store.state.as("state"),
                storeInfo.location.as("location"),
                deliveryCompany.name.as("deliveryCompany"),
                storeInfo.settlementRate.as("settlementRate"),
                storeInfo.bankName.as("bankName"),
                storeInfo.bankHolder.as("bankHolder"),
                storeInfo.bankAccount.as("bankAccount"),
                storeInfo.representativeName.as("representativeName"),
                storeInfo.companyId.as("companyId"),
                storeInfo.businessType.as("businessType"),
                storeInfo.mosRegistrationNumber.as("mosRegistrationNumber"),
                storeInfo.businessAddress.as("businessAddress"),
                storeInfo.postalCode.as("postalCode"),
                storeInfo.lotNumberAddress.as("lotNumberAddress"),
                storeInfo.streetNameAddress.as("streetNameAddress"),
                storeInfo.addressDetail.as("addressDetail"),
                storeInfo.tel.as("phoneNumber"),
                storeInfo.email.as("email"),
                storeInfo.faxNumber.as("faxNumber")
                ))
                .from(store)
                .leftJoin(storeInfo).on(store.id.eq(storeInfo.storeId))
                .leftJoin(deliveryCompany).on(storeInfo.deliverCompany.eq(deliveryCompany.code))
                .where(
                        containsStoreIds(storeIds)
                )
                .orderBy(store.id.asc())
                .fetch();
    }

    private BooleanExpression containsStoreIds(List<Integer> storeIds) {
        if (storeIds != null && !storeIds.isEmpty()) {
            return store.id.in(storeIds);
        }
        return store.id.isNotNull();
    }

    public List<StoreRecommendInquiryDto> selectRecommendStoreWithJoinAt(PageRequest pageRequest,
                                                                         String keyword,
                                                                         Integer userId) {
        return queryFactory.select(Projections.fields(
                        StoreRecommendInquiryDto.class,
                        store.id.as("storeId"),
                        store.loginId.as("loginId"),
                        storeInfo.backgroundImage.as("backgroundImage"),
                        storeInfo.isReliable.as("isReliable"),
                        storeInfo.profileImage.as("profileImage"),
                        storeInfo.name.as("name"),
                        storeInfo.location.as("location"),
                        storeInfo.keyword.as("keyword"),
                        storeInfo.visitNote.as("visitNote"),
                        storeInfo.refundDeliverFee.as("refundDeliveryFee"),
                        storeInfo.oneLineDescription.as("oneLineDescription"),
                        storeInfo.deliverCompany.as("deliverCompany"),
                        storeScrap.userId.isNotNull().as("isLike")
                ))
                .from(storeInfo)
                .leftJoin(store).on(store.id.eq(storeInfo.storeId))
                .leftJoin(storeScrap).on(store.id.eq(storeScrap.storeId)
                        .and(userId != null ? storeScrap.userId.eq(userId) : storeScrap.userId.isNull()))
                .where(store.state.eq(StoreState.ACTIVE)
                        .and(storeInfo.name.contains(keyword))
                )
                .orderBy(store.joinAt.desc())
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();
    }

    public List<StoreRecommendInquiryDto> selectRecommendStoreWithScrape(PageRequest pageRequest,
                                                                         String keyword,
                                                                         Integer userId) {
        return queryFactory.select(Projections.fields(
                        StoreRecommendInquiryDto.class,
                        store.id.as("storeId"),
                        store.loginId.as("loginId"),
                        storeInfo.backgroundImage.as("backgroundImage"),
                        storeInfo.isReliable.as("isReliable"),
                        storeInfo.profileImage.as("profileImage"),
                        storeInfo.name.as("name"),
                        storeInfo.location.as("location"),
                        storeInfo.keyword.as("keyword"),
                        storeInfo.visitNote.as("visitNote"),
                        storeInfo.refundDeliverFee.as("refundDeliveryFee"),
                        storeInfo.oneLineDescription.as("oneLineDescription"),
                        storeInfo.deliverCompany.as("deliverCompany"),
                        storeScrap.userId.isNotNull().as("isLike")
                ))
                .from(storeInfo)
                .leftJoin(store).on(store.id.eq(storeInfo.storeId))
                .leftJoin(storeScrap).on(store.id.eq(storeScrap.storeId)
                        .and(userId != null ? storeScrap.userId.eq(userId) : storeScrap.userId.isNull()))
                .where(store.state.eq(StoreState.ACTIVE)
                        .and(storeInfo.name.contains(keyword))
                )
                .orderBy(storeScrap.storeId.count().desc())
                .groupBy(storeInfo.storeId)
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();
    }

    public List<StoreRecommendInquiryDto> selectRecommendStoreWithReview(PageRequest pageRequest,
                                                                         String keyword,
                                                                         Integer userId) {
        return queryFactory.select(Projections.fields(
                        StoreRecommendInquiryDto.class,
                        store.id.as("storeId"),
                        store.loginId.as("loginId"),
                        storeInfo.backgroundImage.as("backgroundImage"),
                        storeInfo.isReliable.as("isReliable"),
                        storeInfo.profileImage.as("profileImage"),
                        storeInfo.name.as("name"),
                        storeInfo.location.as("location"),
                        storeInfo.keyword.as("keyword"),
                        storeInfo.visitNote.as("visitNote"),
                        storeInfo.refundDeliverFee.as("refundDeliveryFee"),
                        storeInfo.oneLineDescription.as("oneLineDescription"),
                        storeInfo.deliverCompany.as("deliverCompany"),
                        storeScrap.userId.isNotNull().as("isLike")
                ))
                .from(storeInfo)
                .leftJoin(store).on(storeInfo.storeId.eq(store.id))
                .leftJoin(review).on(storeInfo.storeId.eq(review.storeId).and(review.isDeleted.eq(false)))
                .leftJoin(storeScrap).on(store.id.eq(storeScrap.storeId)
                        .and(userId != null ? storeScrap.userId.eq(userId) : storeScrap.userId.isNull()))
                .where(store.state.eq(StoreState.ACTIVE)
                        .and(storeInfo.name.contains(keyword))
                )
                .groupBy(storeInfo.storeId)
                .orderBy(review.storeId.count().desc())
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();
    }

    public List<StoreRecommendInquiryDto> selectRecommendStoreWithOrder(PageRequest pageRequest,
                                                                        String keyword,
                                                                        Integer userId) {
        return queryFactory.select(Projections.fields(
                        StoreRecommendInquiryDto.class,
                        store.id.as("storeId"),
                        store.loginId.as("loginId"),
                        storeInfo.backgroundImage.as("backgroundImage"),
                        storeInfo.isReliable.as("isReliable"),
                        storeInfo.profileImage.as("profileImage"),
                        storeInfo.name.as("name"),
                        storeInfo.location.as("location"),
                        storeInfo.keyword.as("keyword"),
                        storeInfo.visitNote.as("visitNote"),
                        storeInfo.refundDeliverFee.as("refundDeliverFee"),
                        storeInfo.oneLineDescription.as("oneLineDescription"),
                        storeInfo.deliverCompany.as("deliverCompany"),
                        storeScrap.userId.isNotNull().as("isLike")
                ))
                .from(storeInfo)
                .leftJoin(product).on(storeInfo.storeId.eq(product.storeId))
                .leftJoin(store).on(storeInfo.storeId.eq(store.id))
                .leftJoin(orderProductInfo).on(orderProductInfo.productId.eq(product.id))
                .leftJoin(orders).on(orders.id.eq(orderProductInfo.orderId))
                .leftJoin(storeScrap).on(store.id.eq(storeScrap.storeId)
                        .and(userId != null ? storeScrap.userId.eq(userId) : storeScrap.userId.isNull()))
                .where(store.state.eq(StoreState.ACTIVE)
//                        .and(storeInfo.name.contains(keyword))
                        .and(keyword != null ? storeInfo.name.contains(keyword) : null)
                )
                .groupBy(storeInfo.storeId)
                .orderBy(
                        orderProductInfo.state.eq(OrderProductState.FINAL_CONFIRM).count().desc()
                )
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();
    }

    public List<StoreRecommendInquiryDto> selectReliableStoreRandomOrder(Integer userId) {
        return queryFactory.select(Projections.fields(
                        StoreRecommendInquiryDto.class,
                        store.id.as("storeId"),
                        store.loginId.as("loginId"),
                        storeInfo.backgroundImage.as("backgroundImage"),
                        storeInfo.isReliable.as("isReliable"),
                        storeInfo.profileImage.as("profileImage"),
                        storeInfo.name.as("name"),
                        storeInfo.location.as("location"),
                        storeInfo.keyword.as("keyword"),
                        storeInfo.visitNote.as("visitNote"),
                        storeInfo.refundDeliverFee.as("refundDeliveryFee"),
                        storeInfo.oneLineDescription.as("oneLineDescription"),
                        storeInfo.deliverCompany.as("deliverCompany"),
                        storeScrap.userId.isNotNull().as("isLike")
                ))
                .from(storeInfo)
                .leftJoin(storeScrap).on(store.id.eq(storeScrap.storeId)
                        .and(userId != null ? storeScrap.userId.eq(userId) : storeScrap.userId.isNull()))
                .where(storeInfo.isReliable)
                .orderBy(NumberExpression.random().asc())
                .fetch();
    }
}
