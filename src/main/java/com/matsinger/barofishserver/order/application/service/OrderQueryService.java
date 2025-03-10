package com.matsinger.barofishserver.order.application.service;

import com.matsinger.barofishserver.order.domain.model.Orders;
import com.matsinger.barofishserver.order.domain.repository.OrderRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public Orders findById(String orderId) {
        return orderRepository.findById(orderId)
                              .orElseThrow(() -> new BusinessException("주문 정보를 찾을 수 없습니다."));
    }

    public String getOrderId() {
        return orderRepository.selectOrderId().get("id").toString();
    }
}
