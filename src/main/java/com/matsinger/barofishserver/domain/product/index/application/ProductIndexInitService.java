package com.matsinger.barofishserver.domain.product.index.application;

import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.product.filter.utils.FilterConverter;
import com.matsinger.barofishserver.domain.product.index.service.ProductRecommendCacheService;
import com.matsinger.barofishserver.domain.product.index.service.ProductRecommendService;
import com.matsinger.barofishserver.domain.product.processor.AbstractCategoryProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexInitService extends AbstractCategoryProcessor {

    private final ProductRecommendService productRecommendService;
    private final ProductRecommendCacheService productRecommendCacheService;

    @Override
    @Transactional
    public void processCategory(Category category) {
        if (category.isParent()) {
            if (exists(category.getId(), null)) return;

            List<Integer> categoryIds = category.getCategoryList().stream()
                    .map(Category::getId)
                    .collect(Collectors.toList());

            List<Integer> productIds = productRecommendService.findProductIdsByCategoryIdsOrderByWeightDesc(categoryIds);
            productRecommendCacheService.saveRecommendCache(category.getId(), null, FilterConverter.convert(productIds));
        } else {
            if (exists(category.getCategoryId(), category.getId())) return;

            List<Integer> productIds = productRecommendService.findProductIdsByCategoryIdOrderByWeightDesc(category.getId());
            productRecommendCacheService.saveRecommendCache(category.getCategoryId(), category.getId(), FilterConverter.convert(productIds));
        }
    }

    @Override
    protected void processAll() {
    }

    private boolean exists(Integer pCategoryId, Integer cCategoryId) {
        return productRecommendCacheService.existsByCategoryIdAndSubCategoryId(pCategoryId, cCategoryId);
    }
}
