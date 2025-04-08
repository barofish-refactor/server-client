package com.matsinger.barofishserver.domain.product.filter.repository;

import com.matsinger.barofishserver.domain.category.domain.Category;
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

    public List<CategoryFilterProducts> findByFilterIdAndFieldIdsPairs(Category category, Map<Integer, List<Integer>> filterFieldPairs) {

        List<BooleanExpression> orConditions = new ArrayList<>();

        for (Map.Entry<Integer, List<Integer>> entry : filterFieldPairs.entrySet()) {
            orConditions.add(
                            categoryFilterProducts.filterId.eq(entry.getKey())
                            .and(categoryFilterProducts.fieldId.in(entry.getValue()))
            );
        }

        return queryFactory
                .selectFrom(categoryFilterProducts)
                .where(orConditions.stream().reduce(BooleanExpression::or).orElse(null)
                        .and(category.isParent()
                                ? categoryFilterProducts.categoryId.eq(category.getId())
                                    .and(categoryFilterProducts.subCategoryId.isNull())
                                : categoryFilterProducts.categoryId.eq(category.getCategoryId())
                                    .and(categoryFilterProducts.subCategoryId.eq(category.getId()))
                        )
                )
                .fetch();
    }
}
