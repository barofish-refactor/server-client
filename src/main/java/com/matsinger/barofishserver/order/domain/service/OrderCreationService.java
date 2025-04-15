package com.matsinger.barofishserver.order.domain.service;

import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.domain.user.deliverplace.DeliverPlace;
import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.order.application.dto.OrderReq;
import com.matsinger.barofishserver.order.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderCreationService {
    private final OrderProductCreator orderProductCreator;
    private final StoreOrderCalculator storeOrderCalculator;
    private final OrderPriceCalculator orderPriceCalculator;

    public OrderCreationResult createOrder(
            OrderReq request,
            UserInfo userInfo,
            String orderId,
            DeliverPlace deliverPlace,
            List<Product> products,
            List<StoreInfo> stores) {
        
        OrderDeliverPlace orderDeliverPlace = validateDeliverPlace(deliverPlace, orderId);

        // 상품 ID와 스토어 ID로 맵 생성
        Map<Integer, Product> productMap = createProductMap(products);
//        Map<Integer, StoreInfo> storeMap = createStoreMap(stores);

        // 주문 상품 정보 생성
        List<OrderProductInfo> orderProducts = orderProductCreator.createOrderProducts(request, orderId, productMap);

        // 스토어별 주문 상품 그룹화 및 계산
        Map<StoreInfo, StoreOrderProducts> storeOrderProducts = storeOrderCalculator.calculateStoreOrders(
                orderProducts);

        // 가격 계산
        OrderPriceCalculator.OrderPriceResult priceResult = orderPriceCalculator.calculate(
                orderProducts,
                storeOrderProducts,
                request.getCouponDiscountPrice(),
                request.getPoint()
        );

        // 최종 금액 검증
        validateFinalPrice(priceResult.getFinalPrice(), request.getTotalPrice());

        // 주문 생성
        Orders order = createOrder(request, userInfo, orderId, priceResult);

        return OrderCreationResult.builder()
                .order(order)
                .storeOrderProducts(storeOrderProducts)
                .orderDeliverPlace(orderDeliverPlace)
                .taxablePrice(priceResult.getTaxablePrice())
                .nonTaxablePrice(priceResult.getNonTaxablePrice())
                .build();
    }

    private OrderDeliverPlace validateDeliverPlace(DeliverPlace deliverPlace, String orderId) {
        OrderDeliverPlace orderDeliverPlace = deliverPlace.toOrderDeliverPlace(orderId);
        if (orderDeliverPlace.getBcode().length() < 5) {
            throw new BusinessException("배송지에서 법정동코드가 누락되었습니다." + "\n" + "동일한 주소로 다시 배송지를 설정해주세요.");
        }
        return orderDeliverPlace;
    }

    private Map<Integer, Product> createProductMap(List<Product> products) {
        return products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    private Map<Integer, StoreInfo> createStoreMap(List<StoreInfo> stores) {
        return stores.stream()
                .collect(Collectors.toMap(StoreInfo::getStoreId, store -> store));
    }

    private void validateFinalPrice(int calculatedPrice, int requestPrice) {
        if (calculatedPrice != requestPrice) {
            throw new BusinessException("총 금액을 확인해주세요.");
        }
    }

    private Orders createOrder(OrderReq request, UserInfo userInfo, String orderId, OrderPriceCalculator.OrderPriceResult priceResult) {
        return Orders.builder()
                .id(orderId)
                .userId(userInfo.getUserId())
                .paymentWay(request.getPaymentWay())
                .state(OrderState.WAIT_DEPOSIT)
                .couponId(request.getCouponId())
                .orderedAt(Timestamp.valueOf(LocalDateTime.now()))
                .totalPrice(priceResult.getTotalOrderPriceWithDelivery())
                .usePoint(request.getPoint())
                .couponDiscount(request.getCouponDiscountPrice())
                .ordererName(request.getName())
                .ordererTel(request.getTel())
                .originTotalPrice(priceResult.getTotalOrderProductPrice())
                .build();
    }
}
