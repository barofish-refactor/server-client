package com.matsinger.barofishserver.order.domain.service;

import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.order.domain.model.Orders;
import com.matsinger.barofishserver.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderReader {
    private final OrderRepository orderRepository;

    public Orders getOrder(String orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException("주문 정보를 찾을 수 없습니다."));
    }
} 