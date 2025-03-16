package com.matsinger.barofishserver.domain.review.dto;

import com.matsinger.barofishserver.domain.store.domain.StoreSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewStatistic {
    String key;
    Integer count;

    public static List<ReviewStatistic> from(StoreSummary storeSummary) {
        ReviewStatistic taste = ReviewStatistic.builder()
                .key("0")
                .count(storeSummary.getTasteCnt())
                .build();
        ReviewStatistic fresh = ReviewStatistic.builder()
                .key("0")
                .count(storeSummary.getFreshCnt())
                .build();
        ReviewStatistic price = ReviewStatistic.builder()
                .key("0")
                .count(storeSummary.getPriceCnt())
                .build();
        ReviewStatistic packaging = ReviewStatistic.builder()
                .key("0")
                .count(storeSummary.getPackagingCnt())
                .build();
        ReviewStatistic size = ReviewStatistic.builder()
                .key("0")
                .count(storeSummary.getSizeCnt())
                .build();
        return List.of(taste, fresh, price, packaging, size);
    }
}
