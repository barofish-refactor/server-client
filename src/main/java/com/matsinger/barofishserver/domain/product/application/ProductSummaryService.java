package com.matsinger.barofishserver.domain.product.application;

import com.matsinger.barofishserver.domain.product.domain.ProductSummary;
import com.matsinger.barofishserver.domain.product.repository.ProductSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductSummaryService {
    
    private final ProductSummaryRepository productSummaryRepository;
    
    @Transactional(readOnly = true)
    public Map<Integer, ProductSummary> countReviewsWithoutDeletedByProductIds(List<Integer> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ProductSummary> productSummaries = productSummaryRepository.findByProductIds(productIds);
        
        // ProductSummary 리스트를 Map<제품ID, ProductSummary>로 변환
        return productSummaries.stream()
                .collect(Collectors.toMap(
                        ProductSummary::getProductId,
                        Function.identity(),
                        (existing, replacement) -> existing // 중복 키가 있을 경우 기존 값 유지
                ));
    }
} 