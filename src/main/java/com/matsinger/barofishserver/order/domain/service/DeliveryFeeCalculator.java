package com.matsinger.barofishserver.order.domain.service;

import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryFeeCalculator {

    public void calculateDeliveryFee(StoreInfo store, List<OrderProductInfo> orderProducts) {
        resetDeliveryFees(orderProducts);

        if (store.isConditional()) {
            calculateConditionalStoreDeliveryFee(store, orderProducts);
        } else {
            calculateProductBasedDeliveryFee(orderProducts);
        }
    }

    private void resetDeliveryFees(List<OrderProductInfo> orderProducts) {
        orderProducts.forEach(product -> product.setDeliveryFee(0));
    }

    /**
     * 스토어가 조건부 배송비를 가질 때의 배송비를 계산합니다.
     * 주문 금액이 조건을 만족하면 무료, 아니면 가장 비싼 상품에 배송비 부과
     */
    private void calculateConditionalStoreDeliveryFee(StoreInfo store, List<OrderProductInfo> orderProducts) {
        int totalOrderAmount = calculateTotalOrderAmount(orderProducts);
        
        if (!store.meetConditions(totalOrderAmount)) {
            OrderProductInfo mostExpensiveProduct = findMostExpensiveProduct(orderProducts);
            mostExpensiveProduct.setDeliveryFee(store.getDeliveryFee());
        }
    }

    /**
     * 각 상품의 배송비 정책에 따라 배송비를 계산합니다.
     */
    private void calculateProductBasedDeliveryFee(List<OrderProductInfo> orderProducts) {
        // 상품별로 그룹화
        Map<Integer, List<OrderProductInfo>> productGroups = orderProducts.stream()
                .collect(Collectors.groupingBy(OrderProductInfo::getProductId));

        List<OrderProductInfo> productsNeedDeliveryFee = productGroups.entrySet().stream()
                .filter(entry -> needsDeliveryFee(entry.getValue()))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());

        if (!productsNeedDeliveryFee.isEmpty()) {
            OrderProductInfo mostExpensiveProduct = findMostExpensiveProduct(productsNeedDeliveryFee);
            int maxBaseDeliveryFee = findMaxBaseDeliveryFee(productsNeedDeliveryFee);
            mostExpensiveProduct.setDeliveryFee(maxBaseDeliveryFee);
        }
    }

    private OrderProductInfo findMostExpensiveProduct(List<OrderProductInfo> orderProducts) {
        return orderProducts.stream()
                .max((a, b) -> a.getTotalProductPrice() - b.getTotalProductPrice())
                .orElseThrow(() -> new IllegalArgumentException("상품 목록이 비어있습니다."));
    }

    private int findMaxBaseDeliveryFee(List<OrderProductInfo> orderProducts) {
        return orderProducts.stream()
                .map(OrderProductInfo::getProduct)
                .mapToInt(Product::getDeliverFee)
                .max()
                .orElse(0);
    }

    private boolean needsDeliveryFee(List<OrderProductInfo> productGroup) {
        Product product = productGroup.get(0).getProduct();
        
        if (product.isDeliveryTypeFree()) {
            return false;
        }
        
        if (product.isDeliveryTypeFix()) {
            return true;
        }
        
        if (product.isDeliveryTypeFreeIfOver()) {
            int totalAmount = calculateTotalOrderAmount(productGroup);
            return !product.meetConditions(totalAmount);
        }
        
        return false;
    }

    private int calculateTotalOrderAmount(List<OrderProductInfo> orderProducts) {
        return orderProducts.stream()
                .mapToInt(OrderProductInfo::getTotalProductPrice)
                .sum();
    }
}