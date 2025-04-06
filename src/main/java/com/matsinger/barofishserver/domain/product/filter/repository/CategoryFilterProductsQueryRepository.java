package com.matsinger.barofishserver.domain.product.filter.repository;

import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterProducts;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.matsinger.barofishserver.domain.product.filter.domain.QFilterProductCache.filterProductCache;

@Repository
@RequiredArgsConstructor
public class CategoryFilterProductsQueryRepository {
    private final JPAQueryFactory queryFactory;

    public List<CategoryFilterProducts> findByFilterIdAndFieldIdsPairs(Map<Integer, String> filterFieldPairs) {
        List<BooleanExpression> orConditions = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : filterFieldPairs.entrySet()) {
            orConditions.add(
                    filterProductCache.filterId.eq(entry.getKey())
                            .and(filterProductCache.fieldIds.eq(entry.getValue()))
            );
        }

        return queryFactory
                .selectFrom(filterProductCache)
                .where(orConditions.stream().reduce(BooleanExpression::or).orElse(null))
                .fetch();
    }
}
