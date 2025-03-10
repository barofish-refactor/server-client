package com.matsinger.barofishserver.domain.tastingNote.basketTastingNote.repository;

import com.matsinger.barofishserver.domain.tastingNote.basketTastingNote.dto.TastingNoteCompareBasketProductDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.matsinger.barofishserver.domain.category.domain.QCategory.category;
import static com.matsinger.barofishserver.domain.product.domain.QProduct.product;
import static com.matsinger.barofishserver.domain.product.option.domain.QOption.option;
import static com.matsinger.barofishserver.domain.product.optionitem.domain.QOptionItem.optionItem;
import static com.matsinger.barofishserver.domain.review.domain.QReview.review;
import static com.matsinger.barofishserver.domain.store.domain.QStoreInfo.storeInfo;
import static com.matsinger.barofishserver.domain.tastingNote.basketTastingNote.domain.QBasketTastingNote.basketTastingNote;

@Repository
@RequiredArgsConstructor
public class BasketTastingNoteQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<TastingNoteCompareBasketProductDto> findBasketProductsByUserId(int userId) {
        return queryFactory.select(Projections.fields(
                        TastingNoteCompareBasketProductDto.class,
                        basketTastingNote.id.as("id"),
                        product.id.as("productId"),
                        product.title.as("title"),
                        product.state.as("state"),
                        product.images.as("image"),
                        product.needTaxation.as("isNeedTaxation"),
                        optionItem.discountPrice.as("discountPrice"),
                        optionItem.originPrice.as("originPrice"),
                        review.id.count().as("reviewCount"),
                        storeInfo.storeId.as("storeId"),
                        storeInfo.name.as("storeName"),
                        product.minOrderPrice.as("minOrderPrice"),
                        storeInfo.profileImage.as("storeImage"),
                        product.deliverFeeType.as("deliveryFeeType"),
                        category.parentCategory.id.as("parentCategoryId"))
                ).from(basketTastingNote)
                .leftJoin(product).on(product.id.eq(basketTastingNote.product.id))
                .leftJoin(option).on(option.product.id.eq(product.id))
                .leftJoin(optionItem).on(optionItem.option.id.eq(option.id))
                .leftJoin(review).on(review.productId.eq(product.id))
                .leftJoin(storeInfo).on(storeInfo.storeId.eq(product.storeId))
                .leftJoin(category).on(product.category.id.eq(category.id))
                .where(basketTastingNote.user.id.eq(userId))
                .groupBy(product.id)
                .fetch();
    }

    public void deleteAllByUserIdAndProductId(Integer userId, List<Integer> productIds) {
        queryFactory.delete(basketTastingNote)
                .where(basketTastingNote.user.id.eq(userId)
                        .and(basketTastingNote.product.id.in(productIds))
                ).execute();
    }
}
