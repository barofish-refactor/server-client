package com.matsinger.barofishserver.order.domain.service;

import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.StoreOrderProducts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StoreOrderCalculator {
    private final DeliveryFeeCalculator deliveryFeeCalculator;

    public Map<StoreInfo, StoreOrderProducts> calculateStoreOrders(
            List<OrderProductInfo> orderProducts
    ) {
        // 스토어별로 주문 상품 그룹화
        Map<StoreInfo, List<OrderProductInfo>> storeProductsMap = orderProducts.stream()
                .collect(Collectors.groupingBy(OrderProductInfo::getStoreInfo));

        return storeProductsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> {
                            List<OrderProductInfo> storeProducts = entry.getValue();
                            
                            // 배송비 계산
                            deliveryFeeCalculator.calculateDeliveryFee(entry.getKey(), storeProducts);
                            
                            return calculateStoreOrder(storeProducts);
                        }
                ));
    }

    private StoreOrderProducts calculateStoreOrder(List<OrderProductInfo> storeProducts) {
        int totalOrderProductPrice = storeProducts.stream()
                .mapToInt(OrderProductInfo::getPrice)
                .sum();

        int totalOrderDeliveryFee = storeProducts.stream()
                .mapToInt(OrderProductInfo::getDeliveryFee)
                .sum();

        int totalTaxFreePrice = storeProducts.stream()
                .mapToInt(OrderProductInfo::getTaxFreeAmount)
                .sum();

        return StoreOrderProducts.builder()
                .totalOrderDeliveryFee(totalOrderDeliveryFee)
                .totalOrderProductPrice(totalOrderProductPrice)
                .totalTaxFreePrice(totalTaxFreePrice)
                .orderProducts(storeProducts)
                .build();
    }
} 