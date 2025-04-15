package com.matsinger.barofishserver.domain.product.index.api;

import com.matsinger.barofishserver.domain.product.index.application.ProductDiscountIndexInitService;
import com.matsinger.barofishserver.domain.product.index.application.ProductIndexInitService;
import com.matsinger.barofishserver.domain.product.index.application.ProductNewIndexInitService;
import com.matsinger.barofishserver.domain.product.index.application.ProductReviewIndexInitService;
import com.matsinger.barofishserver.domain.product.index.infra.RedisProductIndexBitmapIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/product-index")
@RequiredArgsConstructor
public class ProductIndexController {

    private final ProductIndexInitService filterProductCacheInitService;
    private final RedisProductIndexBitmapIndexer redisProductIndexBitmapIndexer;

    private final ProductNewIndexInitService productNewIndexInitService;
    private final ProductReviewIndexInitService productReviewIndexInitService;
    private final ProductDiscountIndexInitService productDiscountIndexInitService;

    @PostMapping("/initialize-recommend-products-cache")
    public ResponseEntity<Map<String, Integer>> initializeRecommendProductsCache() {
        filterProductCacheInitService.processCategories();
        return ResponseEntity.ok(null);
    }

    @PostMapping("/initialize-recommend-products-cache-redis")
    public ResponseEntity<Map<String, Integer>> initializeRecommendProductsCacheRedis() {
        redisProductIndexBitmapIndexer.processRecommendProducts();
        return ResponseEntity.ok(null);
    }

    @PostMapping("/initialize-new-products-cache-redis")
    public ResponseEntity<Map<String, Integer>> initializeNewProductsCacheRedis() {
        productNewIndexInitService.processCategories();
        return ResponseEntity.ok(null);
    }

    @PostMapping("/initialize-reviews-products-cache-redis")
    public ResponseEntity<Map<String, Integer>> initializeReviewsProductsCacheRedis() {
        productReviewIndexInitService.processCategories();
        return ResponseEntity.ok(null);
    }
//
    @PostMapping("/initialize-discount-products-cache-redis")
    public ResponseEntity<Map<String, Integer>> initializeDiscountProductsCacheRedis() {
        productDiscountIndexInitService.processCategories();
        return ResponseEntity.ok(null);
    }
}
