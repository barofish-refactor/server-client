package com.matsinger.barofishserver.domain.product.filter.repository;

import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterProducts;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.matsinger.barofishserver.domain.product.filter.domain.QCategoryFilterProducts.categoryFilterProducts;

@Repository
@RequiredArgsConstructor
public class CategoryFilterProductsQueryRepository {
    private final JPAQueryFactory queryFactory;

    public List<CategoryFilterProducts> findByFilterIdAndFieldIdsPairs(Map<Integer, String> filterFieldPairs) {
        List<BooleanExpression> orConditions = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : filterFieldPairs.entrySet()) {
            orConditions.add(
                            categoryFilterProducts.filterId.eq(entry.getKey())
                            .and(categoryFilterProducts.fieldIds.eq(entry.getValue()))
            );
        }

        return queryFactory
                .selectFrom(categoryFilterProducts)
                .where(orConditions.stream().reduce(BooleanExpression::or).orElse(null))
                .fetch();
    }

    public List<CategoryFilterProducts> findBy(int categoryId, int cCategoryId, Integer filterId, String fieldIds) {
        return queryFactory
                .selectFrom(categoryFilterProducts)
                .where(categoryFilterProducts.categoryId.eq(categoryId)
                        .and(categoryFilterProducts.subCategoryId.eq(cCategoryId))
                        .and(categoryFilterProducts.filterId.eq(filterId))
                        .and(categoryFilterProducts.fieldIds.eq(fieldIds)))
                .fetch();

    }
}
