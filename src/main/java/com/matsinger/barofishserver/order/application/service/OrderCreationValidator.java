package com.matsinger.barofishserver.order.application.service;

import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.order.application.dto.OrderReq;
import com.matsinger.barofishserver.order.domain.model.OrderCreationResult;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderCreationValidator {
    private final OrderValidationService orderValidationService;

    public void validateInitialRequest(OrderReq request) {
        if (request.getTotalPrice() == 0) {
            throw new BusinessException("주문 금액은 0원일 수 없습니다.");
        }
    }

    public void validateOrderCreation(
            OrderReq request,
            OrderCreationResult result,
            UserInfo userInfo
    ) {
        // 쿠폰과 포인트 검증
        orderValidationService.validateCouponAndPoint(
            request, 
            result.getOrder().getOriginTotalPrice(), 
            userInfo
        );

        // 배송 불가 지역 체크
        List<OrderProductInfo> allOrderProducts = result.getOrder().getProductInfos();
        orderValidationService.checkDeliveryDifficultProducts(
            result.getOrderDeliverPlace(), 
            result.getOrder(),
            allOrderProducts
        );
    }
} 