package com.matsinger.barofishserver.order.domain.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final JPAQueryFactory queryFactory;

//    public List<OrderStoreInquiryDto> selectOrderProducts(List<Integer> optionItemIds) {
//
//        return queryFactory.selectFrom(optionItem)
//                .leftJoin(option).on(optionItem.optionId.eq(option.id))
//                .leftJoin(product).on(product.id.eq(option.productId))
//                .leftJoin(storeInfo).on(storeInfo.storeId.eq(product.storeId))
//                .where(optionItem.id.in(optionItemIds))
//                .transform(groupBy(storeInfo.storeId)
//                        .list(Projections.fields(
//                                OrderStoreInquiryDto.class,
//                                storeInfo.storeId.as("storeId"),
//
//                                GroupBy.list(Projections.fields(
//                                        OrderProductInquiryDto.class,
//                                        product.id.as("productId"),
//
//                                )).as("products")
//                        ))
//                );
//    }
}
