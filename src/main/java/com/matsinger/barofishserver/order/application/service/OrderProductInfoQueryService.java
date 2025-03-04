package com.matsinger.barofishserver.order.application.service;

import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.repository.OrderProductInfoRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderProductInfoQueryService {

    private final OrderProductInfoRepository orderProductInfoRepository;

    public OrderProductInfo findById(int id) {
        return orderProductInfoRepository.findById(id)
                .orElseThrow(() -> new BusinessException("주문 상품 정보를 찾을 수 없습니다."));
    }

    public List<OrderProductInfo> findAllByOrderId(String orderId) {
        return orderProductInfoRepository.findAllByOrderId(orderId);
    }
}
