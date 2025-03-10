package com.matsinger.barofishserver.order.application.dto;

import com.matsinger.barofishserver.order.domain.model.OrderCreationResult;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.order.domain.model.OrderState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder @AllArgsConstructor @NoArgsConstructor
public class OrderResponse {

    private String orderId;
    private boolean canDeliver;
    private int taxablePrice;
    private int nonTaxablePrice;
    private List<Integer> cannotDeliverProductIds;

    public static OrderResponse from(OrderCreationResult result) {
        List<OrderProductInfo> allOrderProducts = result.getOrder().getProductInfos();
        
        return OrderResponse.builder()
                .orderId(result.getOrder().getId())
                .taxablePrice(result.getTaxablePrice())
                .nonTaxablePrice(result.getNonTaxablePrice())
                .canDeliver(result.getOrder().getState() != OrderState.DELIVERY_DIFFICULT)
                .cannotDeliverProductIds(
                    result.getOrder().getState() == OrderState.DELIVERY_DIFFICULT ? 
                    allOrderProducts.stream()
                        .filter(p -> p.getState() == OrderProductState.DELIVERY_DIFFICULT)
                        .map(OrderProductInfo::getProductId)
                        .distinct()
                        .collect(Collectors.toList()) : 
                    new ArrayList<>()
                )
                .build();
    }
}
