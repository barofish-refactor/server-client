package com.matsinger.barofishserver.order.domain.model;

import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.domain.user.deliverplace.DeliverPlace;
import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrderPreparationData {
    private final UserInfo userInfo;
    private final String orderId;
    private final DeliverPlace deliverPlace;
    private final List<Product> products;
    private final List<StoreInfo> stores;
} 