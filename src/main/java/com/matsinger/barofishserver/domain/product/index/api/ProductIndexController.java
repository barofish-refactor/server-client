package com.matsinger.barofishserver.domain.product.index.api;

import com.matsinger.barofishserver.domain.product.index.application.ProductIndexInitService;
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

    @PostMapping("/initialize-recommend-products-cache")
    public ResponseEntity<Map<String, Integer>> initializeRecommendProductsCache() {
        filterProductCacheInitService.processCategories();
        return ResponseEntity.ok(null);
    }

    @PostMapping("/initialize-recommend-products-cache-redis")
    public ResponseEntity<Map<String, Integer>> initializeRecommendProductsCacheRedis() {
        redisProductIndexBitmapIndexer.buildBitmapsInBatches();
        return ResponseEntity.ok(null);
    }
}
