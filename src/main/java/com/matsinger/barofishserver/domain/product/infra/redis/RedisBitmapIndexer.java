package com.matsinger.barofishserver.domain.product.infra.redis;

import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterProducts;
import com.matsinger.barofishserver.domain.product.filter.repository.CategoryFilterProductsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisBitmapIndexer {
    private final CategoryFilterProductsRepository categoryFilterProductsRepository;
    private final StringRedisTemplate redis;

    private static final int BATCH_SIZE = 20;

    public void buildBitmapsInBatches() {
        long totalCnt = categoryFilterProductsRepository.count();
        long totalPages = (totalCnt + BATCH_SIZE - 1) / BATCH_SIZE;

        for (int page = 0; page < totalPages; page++) {
            PageRequest pageRequest = PageRequest.of(page, BATCH_SIZE);
            List<CategoryFilterProducts> filterProducts = categoryFilterProductsRepository.findAll(pageRequest).getContent();

            redis.executePipelined((RedisCallback<Object>) connection -> {
                StringRedisConnection stringConn = (StringRedisConnection) connection;

                for (CategoryFilterProducts filterProduct : filterProducts) {
                    String categoryId = filterProduct.getCategoryId().toString();
                    String subCategoryId = filterProduct.getSubCategoryId() != null ? filterProduct.getSubCategoryId().toString() : "null";
                    String filterId = filterProduct.getFilterId().toString();
                    String fieldId = filterProduct.getFieldId().toString();
                    List<String> productIds = splitCsv(filterProduct.getProductIds());

                    String redisKey = String.format("category-filter:%s:%s:%s:%s", categoryId, subCategoryId, filterId, fieldId);

                    for (String productIdStr : productIds) {
                        long productId = Long.parseLong(productIdStr);
                        stringConn.setBit(redisKey, productId, true);
                    }
                }

                return null;
            });

            System.out.println("Batch " + (page + 1) + "/" + totalPages + " done");
        }
    }

    private List<String> splitCsv(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
