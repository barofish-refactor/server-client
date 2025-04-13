package com.matsinger.barofishserver.domain.product.index.service;

import com.matsinger.barofishserver.domain.product.index.repository.ProductRecommendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductRecommendService {
    private final ProductRecommendRepository productRecommendRepository;

    @Transactional(readOnly = true)
    public List<Integer> findProductIdsByCategoryIdsOrderByWeightDesc(List<Integer> categoryIds) {
        return productRecommendRepository.findProductIdsByCategoryIdsOrderByWeightDesc(categoryIds);
    }

    @Transactional(readOnly = true)
    public List<Integer> findProductIdsByCategoryIdOrderByWeightDesc(int categoryId) {
        return productRecommendRepository.findProductIdsByCategoryIdOrderByWeightDesc(categoryId);
    }
}