package com.matsinger.barofishserver.order.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StoreOrderProducts {
    private final Integer totalOrderDeliveryFee;
    private final Integer totalOrderProductPrice;
    private final Integer totalTaxFreePrice;
    private final List<OrderProductInfo> orderProducts;
} 