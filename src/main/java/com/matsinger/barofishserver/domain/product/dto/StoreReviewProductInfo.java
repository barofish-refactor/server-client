package com.matsinger.barofishserver.domain.product.dto;

import com.matsinger.barofishserver.domain.review.dto.ReviewStatistic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreReviewProductInfo {
    private List<ReviewStatistic> reviewStatistics;
    private Integer reviewCount;
    private Integer productCount;
} 