package com.matsinger.barofishserver.domain.product.infra.redis;

import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.product.domain.ProductSortBy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductFilterSortRedisResolver {

    private final RedisTemplate<String, String> redis;

    public List<Integer> getFilteredSortedProductIds(
            Category category,
            ProductSortBy productSortBy,
            Map<Integer, List<Integer>> filterFieldsMap) {
        byte[] bitmap = getFilteredProductIds(category, filterFieldsMap);

        List<Integer> sortedProductIds = getSortedProductIds(category, productSortBy);
        return doAndProcess(sortedProductIds, bitmap);
    }

    @Nullable
    public byte[] getFilteredProductIds(Category category, Map<Integer, List<Integer>> filterFieldsMap) {
        List<String> filterBitmapKeys = generateOrKeys(category, filterFieldsMap);
        String andKey = generateAndKeys(filterBitmapKeys);

        // 비트맵 전체를 한 번에 가져오기
        byte[] bitmap = redis.execute((RedisCallback<byte[]>) connection ->
                connection.get(andKey.getBytes())
        );

        // 임시 키 정리
        redis.delete(filterBitmapKeys);
        redis.delete(andKey);
        return bitmap;
    }

    public Long getFilteredProductCnt(Category category, Map<Integer, List<Integer>> filterFieldsMap) {
        List<String> filterBitmapKeys = generateOrKeys(category, filterFieldsMap);
        String andKey = generateAndKeys(filterBitmapKeys);

        Long count = redis.execute((RedisCallback<Long>) connection ->
                connection.bitCount(andKey.getBytes())
        );

        // 임시 키 정리
        redis.delete(filterBitmapKeys);
        redis.delete(andKey);
        return count;
    }

    @NotNull
    private String generateAndKeys(List<String> filterBitmapKeys) {
        // AND 연산 (filter 끼리)
        String andKey = "temp:and:" + UUID.randomUUID();
        redis.execute((RedisCallback<Void>) connection -> {
            byte[][] keys = filterBitmapKeys.stream().map(String::getBytes).toArray(byte[][]::new);
            connection.bitOp(RedisStringCommands.BitOperation.AND, andKey.getBytes(), keys);
            return null;
        });
        return andKey;
    }

    private List<String> generateOrKeys(Category category, Map<Integer, List<Integer>> filterFieldsMap) {
        List<String> filterBitmapKeys = new ArrayList<>();

        for (Map.Entry<Integer, List<Integer>> entry : filterFieldsMap.entrySet()) {
            Integer filterId = entry.getKey();
            List<Integer> fieldIds = entry.getValue();

            // OR 키 생성
            String orKey = "temp:or:" + UUID.randomUUID();
            List<String> sourceKeys = new ArrayList<>();

            for (Integer fieldId : fieldIds) {
                String redisKey;
                if (category == null) {
                    redisKey = String.format("category-filter:null:null:%d:%d", filterId, fieldId);
                } else if (category.isParent()) {
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
        return filterBitmapKeys;
    }

    public List<Integer> getSortedProductIds(Category category, ProductSortBy sortBy) {
        String redisKey = getRedisKeyBySortType(sortBy, category);
        String productIdsString = redis.opsForValue().get(redisKey);

        return Arrays.stream(productIdsString.split(","))
                .map(Integer::parseInt).toList();
    }

    private List<Integer> doAndProcess(List<Integer> sortedProductIds, byte[] filteredProductsBits) {
        List<Integer> result = new ArrayList<>();
        for (int productId : sortedProductIds) {
            if (isBitSet(filteredProductsBits, productId)) {
                result.add(productId);
            }
        }
        return result;
    }

    private boolean isBitSet(byte[] bitmap, int bitIndex) {
        int byteIndex = bitIndex / 8;
        int bitOffset = 7 - (bitIndex % 8); // Redis는 big-endian
        return byteIndex < bitmap.length &&
                ((bitmap[byteIndex] >> bitOffset) & 1) == 1;
    }

    private String getRedisKeyBySortType(ProductSortBy sortBy, Category category) {
        String categoryId;
        String subCategoryId;

        if (category == null) {
            categoryId = "null";
            subCategoryId = "null";
        } else if (category.isParent()) {
            categoryId = String.valueOf(category.getId());
            subCategoryId = "null";
        } else {
            categoryId = String.valueOf(category.getCategoryId());
            subCategoryId = String.valueOf(category.getId());
        }

        String prefix = switch (sortBy) {
            case RECOMMEND -> "product-recommend";
            case NEW      -> "product-new";
            case SALES    -> "product-sales";
            case REVIEW   -> "product-review";
            case LIKE     -> "product-like";
            case LOW_PRICE  -> "product-low-price";
            case HIGH_PRICE -> "product-high-price";
            case DISCOUNT -> "product-discount";
            default       -> "product-recommend";
        };

        return String.format("%s:%s:%s", prefix, categoryId, subCategoryId);
    }
}

