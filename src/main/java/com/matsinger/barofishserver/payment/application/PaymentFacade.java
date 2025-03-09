package com.matsinger.barofishserver.payment.application;

import com.matsinger.barofishserver.order.application.dto.OrderReq;
import com.matsinger.barofishserver.order.domain.model.Orders;
import com.matsinger.barofishserver.payment.domain.service.PaymentProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentFacade {
    private final PaymentProcessor paymentProcessor;

    @Transactional
    public void processPayment(OrderReq request, Orders order) {
        paymentProcessor.processPayment(request, order);
    }
} 