package com.matsinger.barofishserver.domain.product.index.infra;

import com.matsinger.barofishserver.domain.product.filter.utils.FilterConverter;
import com.matsinger.barofishserver.domain.product.index.domain.ProductRecommendCache;
import com.matsinger.barofishserver.domain.product.index.service.ProductRecommendCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisProductIndexBitmapIndexer {
    private final ProductRecommendCacheService productRecommendCacheService;
    private final StringRedisTemplate redis;

    private static final int BATCH_SIZE = 20;

    public void processRecommendProducts() {
        long totalCnt = productRecommendCacheService.count();
        long totalPages = (totalCnt + BATCH_SIZE - 1) / BATCH_SIZE;
        for (int page = 0; page < totalPages; page++) {
            PageRequest pageRequest = PageRequest.of(page, BATCH_SIZE);
            List<ProductRecommendCache> productRecommendCaches = productRecommendCacheService.findAll(pageRequest);

            saveToRedis(productRecommendCaches, "product-recommend");

            System.out.println("Batch " + (page + 1) + "/" + totalPages + " done");
        }
    }

    private <T extends RedisProductIdCsvTarget> void saveToRedis(List<T> targets, String keyName) {
        redis.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection stringConn = (StringRedisConnection) connection;
            for (T target : targets) {
                String redisKey = String.format("%s:%s:%s", keyName, target.getCategoryId(), target.getSubCategoryId());

                List<String> productIdsStr = FilterConverter.splitCsv(target.getProductIds());

                // CSV 문자열로 변환
                String csv = String.join(",", productIdsStr);

                // Redis에 저장
                stringConn.set(redisKey, csv);
            }

            return null;
        });
    }

    public void saveToRedis(Integer pCategoryId,
                            Integer cCategoryId,
                            String keyName,
                            List<Integer> productIds) {
        redis.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection stringConn = (StringRedisConnection) connection;
            String redisKey = String.format("%s:%s:%s", keyName, pCategoryId, cCategoryId);

            String csv = productIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            stringConn.set(redisKey, csv);

            log.info("{} is done", redisKey);
            return null;
        });
    }
}
