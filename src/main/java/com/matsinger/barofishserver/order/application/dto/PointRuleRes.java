package com.matsinger.barofishserver.order.application.dto;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointRuleRes {
    Float pointRate;
    Integer maxReviewPoint;
    Integer ImageReviewPoint;
}
