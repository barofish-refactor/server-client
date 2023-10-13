package com.matsinger.barofishserver.review.repository;

import com.matsinger.barofishserver.review.domain.ReviewOrderByType;
import com.matsinger.barofishserver.review.dto.v2.ReviewDtoV2;
import com.matsinger.barofishserver.review.dto.v2.ReviewEvaluationSummaryDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepositoryCustom {

    List<ReviewDtoV2> getPagedProductReviews(int productId, ReviewOrderByType orderType, Pageable pageable);

    List<ReviewEvaluationSummaryDto> getProductReviewEvaluations(int productId);

    List<ReviewDtoV2> getPagedProductSumStoreReviews(Integer storeId, ReviewOrderByType orderCondition, Pageable pageable);

    List<ReviewEvaluationSummaryDto> getProductSumStoreReviewEvaluations(Integer storeId);

    Long getUserReviewCount(Integer userId);

    List<ReviewDtoV2> getPagedUserReview(Integer userId, ReviewOrderByType orderType, PageRequest pageRequest);
}
