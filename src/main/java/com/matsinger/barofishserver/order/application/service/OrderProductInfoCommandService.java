package com.matsinger.barofishserver.order.application.service;

import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.repository.OrderProductInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderProductInfoCommandService {

    private final OrderProductInfoRepository orderProductInfoRepository;

    public void saveAll(List<OrderProductInfo> orderProductInfos) {
        orderProductInfoRepository.saveAll(orderProductInfos);
    }
}
