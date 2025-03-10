package com.matsinger.barofishserver.order.domain.model;

import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OrderCreationResult {
    private final Orders order;
    private final Map<StoreInfo, StoreOrderProducts> storeOrderProducts;
    private final OrderDeliverPlace orderDeliverPlace;
    private final int taxablePrice;
    private final int nonTaxablePrice;
} 