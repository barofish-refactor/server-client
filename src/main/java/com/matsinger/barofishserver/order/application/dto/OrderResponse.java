package com.matsinger.barofishserver.order.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder @AllArgsConstructor @NoArgsConstructor
public class OrderResponse {

    private String orderId;
    private boolean canDeliver;
    private int taxablePrice;
    private int nonTaxablePrice;
    private List<Integer> cannotDeliverProductIds;
}
