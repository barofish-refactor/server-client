package com.matsinger.barofishserver.domain.product.infra.redis;

import com.matsinger.barofishserver.domain.category.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisCategoryFilterResolver {

    private final StringRedisTemplate redis;

    public Long countProduct(Category category, Map<Integer, List<Integer>> filterFieldsMap) {
        List<String> filterBitmapKeys = new ArrayList<>();

        for (Map.Entry<Integer, List<Integer>> entry : filterFieldsMap.entrySet()) {
            Integer filterId = entry.getKey();
            List<Integer> fieldIds = entry.getValue();

            // 필터 하나의 OR 키
            String orKey = "temp:or:" + UUID.randomUUID();
            List<String> sourceKeys = new ArrayList<>();

            for (Integer fieldId : fieldIds) {
                String redisKey;
                if (category.isParent()) {
                    redisKey = String.format("category-filter:%d:null:%d:%d", category.getId(), filterId, fieldId);
                } else {
                    redisKey = String.format("category-filter:%d:%d:%d:%d", category.getCategoryId(), category.getId(), filterId, fieldId);
                }
                sourceKeys.add(redisKey);
            }

            // BITOP OR
            redis.execute((RedisCallback<Void>) connection -> {
                byte[][] keys = sourceKeys.stream().map(String::getBytes).toArray(byte[][]::new);
                connection.bitOp(RedisStringCommands.BitOperation.OR, orKey.getBytes(), keys);
                return null;
            });

            filterBitmapKeys.add(orKey);
        }

        // AND 연산 결과 키
        String andKey = "temp:and:" + UUID.randomUUID();

        redis.execute((RedisCallback<Void>) connection -> {
            byte[][] keys = filterBitmapKeys.stream().map(String::getBytes).toArray(byte[][]::new);
            connection.bitOp(RedisStringCommands.BitOperation.AND, andKey.getBytes(), keys);
            return null;
        });

        // 최종 매칭 상품 수 (BITCOUNT)
        Long count = redis.execute((RedisCallback<Long>) connection ->
                connection.bitCount(andKey.getBytes())
        );

        // 임시 키 정리
        redis.delete(filterBitmapKeys);
        redis.delete(andKey);

        return count != null ? count : 0;
    }
}
