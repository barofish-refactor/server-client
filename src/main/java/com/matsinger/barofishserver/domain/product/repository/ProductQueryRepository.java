package com.matsinger.barofishserver.domain.product.repository;

import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.domain.ProductSortBy;
import com.matsinger.barofishserver.domain.product.domain.ProductState;
import com.matsinger.barofishserver.domain.product.dto.ProductListDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.matsinger.barofishserver.domain.category.domain.QCategory.category;
import static com.matsinger.barofishserver.domain.data.curation.domain.QCurationProductMap.curationProductMap;
import static com.matsinger.barofishserver.domain.product.domain.QProduct.product;
import static com.matsinger.barofishserver.domain.product.optionitem.domain.QOptionItem.optionItem;
import static com.matsinger.barofishserver.domain.review.domain.QReview.review;
import static com.matsinger.barofishserver.domain.searchFilter.domain.QProductSearchFilterMap.productSearchFilterMap;
import static com.matsinger.barofishserver.domain.store.domain.QStore.store;
import static com.matsinger.barofishserver.domain.store.domain.QStoreInfo.storeInfo;
import static com.matsinger.barofishserver.domain.userinfo.domain.QUserInfo.userInfo;
import static com.matsinger.barofishserver.order.domain.model.QOrderProductInfo.orderProductInfo;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<ProductListDto> selectNewerProducts(PageRequest pageRequest, Map<Integer, List<Integer>> filterFieldsMap, int count) {

        return queryFactory
                .select(Projections.fields(
                        ProductListDto.class,
                        product.id.as("id"),
                        product.state.as("state"),
                        product.images.as("image"),
                        product.title.as("title"),
                        product.needTaxation.as("isNeedTaxation"),
                        product.minOrderPrice.as("minOrderPrice"),
                        product.deliverFeeType.as("delieverFeeType"),
                        optionItem.discountPrice.as("discountPrice"),
                        optionItem.originPrice.as("originPrice"),
                        storeInfo.storeId.as("storeId"),
                        storeInfo.name.as("storeName"),
                        storeInfo.profileImage.as("storeImage")
                ))
                .from(product)
                .leftJoin(storeInfo).on(product.storeId.eq(storeInfo.storeId))
                .leftJoin(optionItem).on(product.representOptionItemId.eq(optionItem.id))
                .where(product.state.eq(ProductState.ACTIVE),
//                        isPromotionInProgress(),
                        isIncludedSearchFilter(filterFieldsMap)
                )
                .groupBy(product.id)
//                .orderBy(orderSpecifiers)
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();
    }

    private OrderSpecifier[] createNewerSpecifier() {
        List<OrderSpecifier> orderSpecifiers = new ArrayList<>();

        orderSpecifiers.add(new OrderSpecifier(
                Order.DESC, product.createdAt
        ));
        return orderSpecifiers.toArray(new OrderSpecifier[orderSpecifiers.size()]);
    }

    public List<ProductListDto> selectPopularProducts(PageRequest pageRequest,
                                                          Map<Integer, List<Integer>> filterFieldsIds, int count) {

        OrderSpecifier[] orderSpecifiers = createPopularOrderSpecifier();

        return queryFactory
                .select(Projections.fields(
                        ProductListDto.class,
                        product.id.as("id"),
                        product.state.as("state"),
                        product.images.as("image"),
                        product.title.as("title"),
                        product.needTaxation.as("isNeedTaxation"),
                        product.deliverFeeType.as("delieverFeeType"),
                        product.minOrderPrice.as("minOrderPrice"),
                        optionItem.discountPrice.as("discountPrice"),
                        optionItem.originPrice.as("originPrice"),
                        storeInfo.storeId.as("storeId"),
                        storeInfo.name.as("storeName"),
                        storeInfo.profileImage.as("storeImage")
                ))
                .from(product)
                .leftJoin(storeInfo).on(product.storeId.eq(storeInfo.storeId))
                .leftJoin(optionItem).on(product.representOptionItemId.eq(optionItem.id))
                .leftJoin(review).on(product.id.eq(review.productId))
                .where(product.state.eq(ProductState.ACTIVE),
                        review.isDeleted.eq(false),
                        isPromotionInProgress(),
                        isIncludedSearchFilter(filterFieldsIds)
                )
                .groupBy(product.id)
                .orderBy(orderSpecifiers)
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();
    }

    private OrderSpecifier[] createPopularOrderSpecifier() {
        List<OrderSpecifier> orderSpecifiers = new ArrayList<>();

        // 주문 많은 순
//        orderSpecifiers.add(new OrderSpecifier(
//                Order.DESC, orderProductInfo.productId.count()
//        ));

        // 리뷰 많은 순
        orderSpecifiers.add(new OrderSpecifier(
                Order.DESC, review.id.count()
        ));
        return orderSpecifiers.toArray(new OrderSpecifier[orderSpecifiers.size()]);
    }

    public List<ProductListDto> selectDiscountProducts(PageRequest pageRequest,
                                                           Map<Integer, List<Integer>> filterFieldsIds, int count) {

        OrderSpecifier[] orderSpecifiers = createDiscountOrderSpecifier();

        return queryFactory
                .select(Projections.fields(
                        ProductListDto.class,
                        product.id.as("id"),
                        product.state.as("state"),
                        product.images.as("image"),
                        product.title.as("title"),
                        product.needTaxation.as("isNeedTaxation"),
                        product.deliverFeeType.as("delieverFeeType"),
                        product.minOrderPrice.as("minOrderPrice"),
                        optionItem.discountPrice.as("discountPrice"),
                        optionItem.originPrice.as("originPrice"),
                        storeInfo.storeId.as("storeId"),
                        storeInfo.name.as("storeName"),
                        storeInfo.profileImage.as("storeImage")
                ))
                .from(product)
                .leftJoin(storeInfo).on(product.storeId.eq(storeInfo.storeId))
                .leftJoin(optionItem).on(product.representOptionItemId.eq(optionItem.id))
                .where(product.state.eq(ProductState.ACTIVE),
                        isPromotionInProgress(),
                        isIncludedSearchFilter(filterFieldsIds),
                        isDiscountApplied()
                )
                .groupBy(product.id)
                .orderBy(orderSpecifiers)
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();}

    private OrderSpecifier[] createDiscountOrderSpecifier() {
        List<OrderSpecifier> orderSpecifiers = new ArrayList<>();

        orderSpecifiers.add(new OrderSpecifier(
                Order.DESC, optionItem.discountPrice.divide(optionItem.originPrice)
                )
        );
        return orderSpecifiers.toArray(new OrderSpecifier[orderSpecifiers.size()]);
    }

    public PageImpl<ProductListDto> getProducts(PageRequest pageRequest,
                                                ProductSortBy sortBy,
                                                List<Integer> categoryIds,
                                                Map<Integer, List<Integer>> filterFieldsMap,
                                                Integer curationId,
                                                String keyword,
                                                List<Integer> productIds,
                                                Integer storeId,
                                                Integer count) {

        OrderSpecifier[] orderSpecifiers = createProductSortSpecifier(sortBy);
        List<ProductListDto> inquiryData = queryFactory
                .select(Projections.fields(
                        ProductListDto.class,
                        product.id.as("id"),
                        product.state.as("state"),
                        product.images.as("image"),
                        product.title.as("title"),
                        product.needTaxation.as("isNeedTaxation"),
                        optionItem.discountPrice.as("discountPrice"),
                        optionItem.originPrice.as("originPrice"),
                        storeInfo.storeId.as("storeId"),
                        storeInfo.name.as("storeName"),
                        product.minOrderPrice.as("minOrderPrice"),
                        storeInfo.profileImage.as("storeImage"),
                        product.deliverFeeType.as("delieverFeeType"),
                        category.parentCategory.id.as("parentCategoryId")
                ))
                .from(product)
                .leftJoin(storeInfo).on(product.storeId.eq(storeInfo.storeId))
                .leftJoin(optionItem).on(product.representOptionItemId.eq(optionItem.id))
                .leftJoin(category).on(category.id.eq(product.category.id))
                .where(product.state.eq(ProductState.ACTIVE),
                        eqCuration(curationId),
                        isPromotionInProgress(),
                        eqStore(storeId),
                        isProductTitleLikeKeyword(keyword),
                        isIncludedCategory(categoryIds),
                        isIncludedSearchFilter(filterFieldsMap)
                )
                .groupBy(product.id)
                .orderBy(orderSpecifiers)
                .offset(pageRequest.getOffset())
                .limit(pageRequest.getPageSize())
                .fetch();

        return new PageImpl<>(inquiryData, pageRequest, count);
    }

    private BooleanExpression matches(StringPath storeName, String[] keywords) {
        BooleanExpression keywordMatchesStoreName = null;
        for (String keyword : keywords) {
            if (keywordMatchesStoreName == null) {
                keywordMatchesStoreName = storeName.contains(keyword);
            } else {
                keywordMatchesStoreName.or(storeName.contains(keyword));
            }
        }
        return keywordMatchesStoreName;
    }

    public int countDiscountProducts(List<Integer> categoryIds, Map<Integer, List<Integer>> filterFieldsMap) {
        int count =(int) queryFactory
                .select(product.count())
                .from(product)
                .leftJoin(optionItem).on(product.representOptionItemId.eq(optionItem.id))
                .where(product.state.eq(ProductState.ACTIVE),
//                        isPromotionInProgress(),
                        isIncludedCategory(categoryIds),
                        isIncludedSearchFilter(filterFieldsMap),
                        isDiscountApplied()
                )
                .groupBy(product.id)
                .stream().count();
        return count;
    }

    public Integer countNewerProducts(List<Integer> categoryIds, Map<Integer, List<Integer>> filterFieldsMap) {
        Integer count = (int) queryFactory.select(product.count())
                .from(product)
                .where(product.state.eq(ProductState.ACTIVE),
//                        isPromotionInProgress(),
                        isIncludedCategory(categoryIds),
                        isIncludedSearchFilter(filterFieldsMap)
                )
                .groupBy(product.id)
                .stream().count();
        return count;
    }

    public int countPopularProducts(List<Integer> categoryIds, Map<Integer, List<Integer>> filterFieldsMap) {
        int count = (int) queryFactory
                .select(product.count())
                .from(product)
                .where(product.state.eq(ProductState.ACTIVE),
//                        isPromotionInProgress(),
                        isIncludedCategory(categoryIds),
                        isIncludedSearchFilter(filterFieldsMap)
                )
                .groupBy(product.id)
                .stream().count();
        return count;
    }

    public Integer countProducts(List<Integer> categoryIds,
                                 Map<Integer, List<Integer>> filterFieldIds,
                                 Integer curationId,
                                 String keyword,
                                 List<Integer> productIds,
                                 Integer storeId) {
        int count = (int) queryFactory
                .select(product.id)
                .from(product)
                .where(product.state.eq(ProductState.ACTIVE),
                        eqCuration(curationId),
                        isPromotionInProgress(),
                        eqStore(storeId),
                        isProductTitleLikeKeyword(keyword),
                        isIncludedCategory(categoryIds),
                        isIncludedSearchFilter(filterFieldIds),
                        isInProductIds(productIds)
                )
                .groupBy(product.id)
                .stream().count();
        return count;
    }

    private BooleanExpression isInProductIds(List<Integer> productIds) {
        if (productIds == null) {
            return null;
        }
        return product.id.in(productIds);
    }

    private CaseBuilder sortByProductIds(List<Integer> productIds) {
        CaseBuilder caseBuilder = new CaseBuilder();
        int productIdsSize = productIds.size();
        for (Integer productId : productIds) {
            caseBuilder
                    .when(product.id.eq(productId)).then(productIdsSize).otherwise(0);
            productIdsSize--;
        }
        return caseBuilder;
    }
    private OrderSpecifier[] createProductSortSpecifier(ProductSortBy sortBy) {

        List<OrderSpecifier> orderSpecifiers = new ArrayList<>();

        if (sortBy.equals(ProductSortBy.NEW)) {
            orderSpecifiers.add(new OrderSpecifier(Order.DESC, product.createdAt));
        }
        if (sortBy.equals(ProductSortBy.SALES)) {
            orderSpecifiers.add(new OrderSpecifier(
                    Order.DESC,
                    queryFactory.select(orderProductInfo.id.isNotNull().count())
                            .from(orderProductInfo)
                            .where(orderProductInfo.productId.eq(product.id)
                                    .and(orderProductInfo.state.in(OrderProductState.PAYMENT_DONE, OrderProductState.FINAL_CONFIRM)
                                            .and(orderProductInfo.orderId.notLike("2311281459324003"))
                                            .and(orderProductInfo.price.notIn(0))))
                            .groupBy(product.id)
            ));
        }
        if (sortBy.equals(ProductSortBy.REVIEW)) {
            orderSpecifiers.add(new OrderSpecifier(
                    Order.DESC,
                    queryFactory.select(review.id.count())
                            .from(review)
                            .where(review.productId.eq(product.id)
                                    .and(review.isDeleted.isFalse()))
                            .groupBy(product.id)
            ));
        }
        if (sortBy.equals(ProductSortBy.LOW_PRICE)) {
            orderSpecifiers.add(new OrderSpecifier(Order.ASC, optionItem.discountPrice));
        }
        if (sortBy.equals(ProductSortBy.HIGH_PRICE)) {
            orderSpecifiers.add(new OrderSpecifier(Order.DESC, optionItem.discountPrice));
        }
        return orderSpecifiers.toArray(new OrderSpecifier[orderSpecifiers.size()]);
    }

    private BooleanExpression excludeIntendedReviews() {
        return userInfo.email.notLike("baroTastingNote")
                .and(userInfo.email.notLike("baroReviewId"));
    }

    private BooleanExpression isDiscountApplied() {
        return optionItem.originPrice.notIn(0);
    }

    private BooleanExpression isIncludedSearchFilter(Map<Integer, List<Integer>> filterFieldsMap) {
        if (filterFieldsMap == null) {
            return null;
        }
        BooleanExpression booleanExpression = null;
        for (Integer filterId : filterFieldsMap.keySet()) {

            BooleanExpression filterCondition = product.id.in(
                    JPAExpressions
                            .select(productSearchFilterMap.productId)
                            .from(productSearchFilterMap)
                            .where(productSearchFilterMap.fieldId.in(filterFieldsMap.get(filterId)))
            );

            if (booleanExpression == null) {
                booleanExpression = filterCondition;
            } else {
                booleanExpression = booleanExpression.and(filterCondition);
            }
        }
        return booleanExpression;
    }

    private BooleanExpression isIncludedCategory(List<Integer> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }
        return category.parentCategory.id.in(categoryIds)
                .or(category.id.in(categoryIds));
    }

    private BooleanExpression isProductTitleLikeKeyword(String keyword) {
        if (StringUtils.isEmpty(keyword)) {
            return null;
        }
        return product.title.contains(keyword);
    }

    private BooleanExpression eqCuration(Integer curationId) {
        if (curationId == null) {
            return null;
        }

        return product.id.in(
                JPAExpressions
                        .select(curationProductMap.product.id)
                        .from(curationProductMap)
                        .where(curationProductMap.curation.id.eq(curationId))
        );
    }

    private BooleanExpression eqStore(Integer storeId) {
        if (storeId == null) {
            return null;
        }
        return product.storeId.eq(storeId);
    }

    private BooleanBuilder isPromotionInProgress() {
        BooleanBuilder builder = new BooleanBuilder();
        builder
                // or 조건을 만들기 위해 사용
                .andAnyOf(
                        // product.promotionStartAt이 null이거나 현재 시간보다 작은 product를 선택
                        product.promotionStartAt.isNull(),
                        product.promotionStartAt.lt(Timestamp.valueOf(LocalDateTime.now()))
                )
                .andAnyOf(
                        product.promotionEndAt.isNull(),
                        product.promotionEndAt.gt(Timestamp.valueOf(LocalDateTime.now()))
                );
        return builder;
    }

    public List<Product> findAllActiveProductsByStoreId(int storeId) {
        return queryFactory.select(product)
                .from(product)
                .leftJoin(store).on(store.id.eq(product.storeId))
                .where(store.id.eq(storeId)
                        .and(product.state.eq(ProductState.ACTIVE)))
                .fetch();
    }

    public List<Product> findAllTemporaryInactiveProductsByStoreId(int storeId) {
        return queryFactory.select(product)
                .from(product)
                .leftJoin(store).on(store.id.eq(product.storeId))
                .where(store.id.eq(storeId)
                        .and(product.state.eq(ProductState.INACTIVE_PARTNER)))
                .fetch();
    }

    public List<Integer> findCategoryFieldsProduct(int categoryId, List<Integer> fields) {
        return queryFactory
                .select(productSearchFilterMap.productId)
                .from(product)
                .leftJoin(productSearchFilterMap).on(productSearchFilterMap.productId.eq(product.id))
                .where(
                        product.category.id.eq(categoryId),
                        productSearchFilterMap.fieldId.in(fields)
                )
                .fetch();
    }
}
