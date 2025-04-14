package com.matsinger.barofishserver.domain.product.infra.redis;

import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.product.domain.ProductSortBy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ProductFilterSortRedisResolver {

    private final RedisTemplate<String, String> redis;

    public byte[] getFilteredProductsBits(
            Category category,
            Map<Integer, List<Integer>> filterFieldsMap) {
        List<String> filterBitmapKeys = new ArrayList<>();

        for (Map.Entry<Integer, List<Integer>> entry : filterFieldsMap.entrySet()) {
            Integer filterId = entry.getKey();
            List<Integer> fieldIds = entry.getValue();

            // OR 키 생성
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

            // OR 연산
            redis.execute((RedisCallback<Void>) connection -> {
                byte[][] keys = sourceKeys.stream().map(String::getBytes).toArray(byte[][]::new);
                connection.bitOp(RedisStringCommands.BitOperation.OR, orKey.getBytes(), keys);
                return null;
            });

            filterBitmapKeys.add(orKey);
        }

        // AND 연산 (filter 끼리)
        String andKey = "temp:and:" + UUID.randomUUID();
        redis.execute((RedisCallback<Void>) connection -> {
            byte[][] keys = filterBitmapKeys.stream().map(String::getBytes).toArray(byte[][]::new);
            connection.bitOp(RedisStringCommands.BitOperation.AND, andKey.getBytes(), keys);
            return null;
        });

        // 비트맵 전체를 한 번에 가져오기
        byte[] bitmap = redis.execute((RedisCallback<byte[]>) connection ->
                connection.get(andKey.getBytes())
        );

        // 임시 키 정리
        redis.delete(filterBitmapKeys);
        redis.delete(andKey);

        return bitmap;
    }

    public String getSortedProductIds(Category category, ProductSortBy sortBy) {
        String redisKey = getRedisKeyBySortType(sortBy, category);
        return redis.opsForValue().get(redisKey);
    }

    private String getRedisKeyBySortType(ProductSortBy sortBy, Category category) {
        String categoryId = category.isParent()
                ? String.valueOf(category.getId())
                : String.valueOf(category.getCategoryId());
        String subCategoryId = category.isParent()
                ? "null"
                : String.valueOf(category.getId());

        String prefix = switch (sortBy) {
            case RECOMMEND -> "product-recommend";
            case NEW      -> "product-new";
            case SALES    -> "product-sales";
            case REVIEW   -> "product-review";
            case LIKE     -> "product-like";
            case LOW_PRICE  -> "product-low-price";
            case HIGH_PRICE -> "product-high-price";
            default       -> "product-recommend";
        };

        return String.format("%s:%s:%s", prefix, categoryId, subCategoryId);
    }
}

