package com.matsinger.barofishserver.order.application.dto;

import com.matsinger.barofishserver.order.domain.model.OrderCancelReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder @NoArgsConstructor @AllArgsConstructor
public class RequestCancelReq {
    private OrderCancelReason cancelReason;
    private String content;
}
