package com.matsinger.barofishserver.domain.product.index.infra;

import com.matsinger.barofishserver.domain.product.filter.utils.FilterConverter;
import com.matsinger.barofishserver.domain.product.index.domain.ProductRecommendCache;
import com.matsinger.barofishserver.domain.product.index.service.ProductRecommendCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisProductIndexBitmapIndexer {
    private final ProductRecommendCacheService productRecommendCacheService;
    private final StringRedisTemplate redis;

    private static final int BATCH_SIZE = 20;
    public void buildBitmapsInBatches() {
        long totalCnt = productRecommendCacheService.count();
        long totalPages = (totalCnt + BATCH_SIZE - 1) / BATCH_SIZE;

        for (int page = 0; page < totalPages; page++) {
            PageRequest pageRequest = PageRequest.of(page, BATCH_SIZE);
            List<ProductRecommendCache> productRecommendCaches = productRecommendCacheService.findAll(pageRequest);

            redis.executePipelined((RedisCallback<Object>) connection -> {
                StringRedisConnection stringConn = (StringRedisConnection) connection;
                for (ProductRecommendCache productRecommendCache : productRecommendCaches) {
                    String redisKey = String.format("product-recommend:%s:%s", productRecommendCache.getCategoryId(), productRecommendCache.getSubCategoryId());

                    List<String> productIdsStr = FilterConverter.splitCsv(productRecommendCache.getProductIds());

                    // CSV 문자열로 변환
                    String csv = String.join(",", productIdsStr);

                    // Redis에 저장
                    stringConn.set(redisKey, csv);
                }

                return null;
            });

            System.out.println("Batch " + (page + 1) + "/" + totalPages + " done");
        }
    }
}
