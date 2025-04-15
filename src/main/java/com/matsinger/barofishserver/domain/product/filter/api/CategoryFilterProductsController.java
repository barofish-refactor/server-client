package com.matsinger.barofishserver.domain.product.filter.api;

import com.matsinger.barofishserver.domain.product.filter.application.FilterProductCacheInitService;
import com.matsinger.barofishserver.domain.product.filter.infra.redis.RedisCategoryFilterBitmapIndexer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 필터 상품 캐시 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/filter-product-cache")
@RequiredArgsConstructor
@Tag(name = "필터 상품 캐시", description = "필터 상품 캐시 관련 API")
public class CategoryFilterProductsController {

    private final FilterProductCacheInitService filterProductCacheInitService;
    private final RedisCategoryFilterBitmapIndexer redisCategoryFilterBitmapIndexer;

    /**
     * 필터 상품 캐시 초기화 API
     * 모든 카테고리, 서브카테고리, 필터에 대한 상품 캐시를 생성합니다.
     */
    @PostMapping("/initialize")
    @Operation(summary = "필터 상품 캐시 초기화", description = "모든 카테고리, 서브카테고리, 필터에 대한 상품 캐시를 새로 생성합니다.")
    public ResponseEntity<Map<String, Integer>> initializeFilterProductCache() {
        filterProductCacheInitService.processCategories();
        return ResponseEntity.ok(null);
    }

    @PostMapping("/initialize-redis")
    public ResponseEntity<Map<String, Integer>> initializeFilterProductRedisCache() {
        redisCategoryFilterBitmapIndexer.buildBitmapsInBatches();
        return ResponseEntity.ok(null);
    }
} 