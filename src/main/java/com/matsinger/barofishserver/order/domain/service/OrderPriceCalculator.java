package com.matsinger.barofishserver.order.domain.service;

import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.StoreOrderProducts;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OrderPriceCalculator {

    @Getter
    @Builder
    public static class OrderPriceResult {
        private final int totalOrderProductPrice;
        private final int totalOrderDeliveryFee;
        private final int totalOrderPriceWithDelivery;
        private final int taxablePrice;
        private final int nonTaxablePrice;
        private final int finalPrice;
    }

    public OrderPriceResult calculate(
            List<OrderProductInfo> orderProducts,
            Map<StoreInfo, StoreOrderProducts> storeOrderProducts,
            int couponDiscountPrice,
            int pointDiscount
    ) {
        // 총 주문 금액 계산
        int totalOrderProductPrice = storeOrderProducts.values().stream()
                .mapToInt(StoreOrderProducts::getTotalOrderProductPrice)
                .sum();
        int totalOrderDeliveryFee = storeOrderProducts.values().stream()
                .mapToInt(StoreOrderProducts::getTotalOrderDeliveryFee)
                .sum();

        int totalOrderPriceWithDelivery = totalOrderProductPrice + totalOrderDeliveryFee;
        
        // 면세/과세 금액 계산
        int totalTaxFreePrice = orderProducts.stream()
                .mapToInt(OrderProductInfo::getTaxFreeAmount)
                .sum();
        int taxablePrice = totalOrderPriceWithDelivery - totalTaxFreePrice;

        // 할인금액이 과세금액보다 큰 경우 면세금액에서 차감
        int discountPrice = couponDiscountPrice + pointDiscount;
        int nonTaxablePrice = totalTaxFreePrice;
        if (taxablePrice < discountPrice) {
            int remainingDiscount = discountPrice - taxablePrice;
            taxablePrice = 0;
            nonTaxablePrice = totalTaxFreePrice - remainingDiscount;
        }
        
        // 최종 금액 계산
        int finalPrice = totalOrderPriceWithDelivery - couponDiscountPrice - pointDiscount;

        return OrderPriceResult.builder()
                .totalOrderProductPrice(totalOrderProductPrice)
                .totalOrderDeliveryFee(totalOrderDeliveryFee)
                .totalOrderPriceWithDelivery(totalOrderPriceWithDelivery)
                .taxablePrice(taxablePrice)
                .nonTaxablePrice(nonTaxablePrice)
                .finalPrice(finalPrice)
                .build();
    }
} 