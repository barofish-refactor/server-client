package com.matsinger.barofishserver.domain.product.index.application;

import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.product.application.ProductQueryService;
import com.matsinger.barofishserver.domain.product.index.infra.RedisProductIndexBitmapIndexer;
import com.matsinger.barofishserver.domain.product.processor.AbstractCategoryProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductNewIndexInitService extends AbstractCategoryProcessor {

    private final ProductQueryService productQueryService;
    private final RedisProductIndexBitmapIndexer redisProductIndexBitmapIndexer;

    @Override
    protected void processCategory(Category category) {
        if (category.isParent()) {
            List<Integer> categoryIds = category.getCategoryList().stream()
                    .map(Category::getId)
                    .collect(Collectors.toList());

            List<Integer> productIds = productQueryService.getProductIdsByCategoryIdsOrderByCreatedAtDesc(categoryIds);
            redisProductIndexBitmapIndexer.saveToRedis(category.getId(), null, "product-new", productIds);
        } else {
            List<Integer> productIds = productQueryService.findProductIdsByCategoryIdOrderByCreatedAtDesc(category.getId());
            redisProductIndexBitmapIndexer.saveToRedis(category.getCategoryId(), category.getId(), "product-new", productIds);
        }
    }

    @Override
    protected void processAll() {
        List<Integer> productIds = productQueryService.findIdsByOrderByCreatedAtDesc();
        redisProductIndexBitmapIndexer.saveToRedis(null, null, "product-new", productIds);
    }
}
