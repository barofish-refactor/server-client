package com.matsinger.barofishserver.domain.dashboard.dto;

import com.matsinger.barofishserver.domain.inquiry.dto.InquiryDto;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashBoard {
    Integer dailyJoinCount;
    List<InquiryDto> inquiries;
    Integer dailyOrderCount;
    Integer dailyOrderAmount;
    List<OrderProductInfo> orderSituation;
    Integer monthlyJoinCount;
    Integer monthlyOrderCount;
    Integer monthlyOrderAmount;
    List<ProductRankDto> dailyMostSoldProduct;
    List<ProductRankDto> monthlyMostSoldProduct;
}
