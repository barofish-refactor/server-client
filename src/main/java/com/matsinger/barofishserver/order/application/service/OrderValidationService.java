package com.matsinger.barofishserver.order.application.service;

import com.matsinger.barofishserver.domain.coupon.application.CouponQueryService;
import com.matsinger.barofishserver.domain.coupon.domain.Coupon;
import com.matsinger.barofishserver.domain.product.difficultDeliverAddress.application.DifficultDeliverAddressQueryService;
import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import com.matsinger.barofishserver.order.application.dto.OrderReq;
import com.matsinger.barofishserver.order.domain.model.OrderDeliverPlace;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.order.domain.model.Orders;
import com.matsinger.barofishserver.order.domain.model.OrderState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderValidationService {
    private final CouponQueryService couponQueryService;
    private final DifficultDeliverAddressQueryService difficultDeliverAddressQueryService;

    public void validateCouponAndPoint(OrderReq request, int totalOrderPrice, UserInfo userInfo) {
        if (request.getCouponId() != null) {
            Coupon coupon = couponQueryService.findById(request.getCouponId());
            coupon.isAvailable(totalOrderPrice);
        }
        userInfo.validatePoint(request.getPoint());
    }

    public boolean checkDeliveryDifficultProducts(
            OrderDeliverPlace orderDeliverPlace,
            Orders order,
            List<OrderProductInfo> orderProducts) {
        
        // 모든 상품 ID 추출
        Set<Integer> uniqueProductIds = orderProducts.stream()
                .map(OrderProductInfo::getProductId)
                .collect(Collectors.toSet());

        // 배송 불가능한 상품 ID 목록 조회
        List<Integer> difficultDeliveryProductIds = difficultDeliverAddressQueryService.findDifficultDeliveryProductIds(
            uniqueProductIds, 
            orderDeliverPlace.getBcode()
        );

        if (!difficultDeliveryProductIds.isEmpty()) {
            // 배송 불가 상품들의 상태 변경
            orderProducts.stream()
                .filter(product -> difficultDeliveryProductIds.contains(product.getProductId()))
                .forEach(product -> product.setState(OrderProductState.DELIVERY_DIFFICULT));

            // 주문 상태 변경
            order.setState(OrderState.DELIVERY_DIFFICULT);
            return true;
        }

        return false;
    }
} 