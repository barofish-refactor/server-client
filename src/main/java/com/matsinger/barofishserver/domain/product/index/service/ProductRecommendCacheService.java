package com.matsinger.barofishserver.domain.product.index.service;

import com.matsinger.barofishserver.domain.product.index.domain.ProductRecommendCache;
import com.matsinger.barofishserver.domain.product.index.repository.ProductRecommendCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductRecommendCacheService {
    private final ProductRecommendCacheRepository productRecommendCacheRepository;

    @Transactional
    public void saveRecommendCache(Integer categoryId, Integer subCategoryId, String productIds) {
        productRecommendCacheRepository.save(
                ProductRecommendCache.from(categoryId, subCategoryId, productIds)
        );
    }

    public long count() {
        return productRecommendCacheRepository.count();
    }

    public List<ProductRecommendCache> findAll(PageRequest pageRequest) {
        return productRecommendCacheRepository.findAll(pageRequest).getContent();
    }

    public boolean existsByCategoryIdAndSubCategoryId(Integer pCategoryId, Integer cCategoryId) {
        return productRecommendCacheRepository.existsByCategoryIdAndSubCategoryId(pCategoryId, cCategoryId);
    }
}