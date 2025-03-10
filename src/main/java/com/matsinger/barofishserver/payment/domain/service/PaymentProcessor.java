package com.matsinger.barofishserver.payment.domain.service;

import com.matsinger.barofishserver.domain.payment.application.PaymentService;
import com.matsinger.barofishserver.domain.payment.dto.KeyInPaymentReq;
import com.matsinger.barofishserver.domain.user.paymentMethod.application.PaymentMethodService;
import com.matsinger.barofishserver.domain.user.paymentMethod.domain.PaymentMethod;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.order.application.dto.OrderReq;
import com.matsinger.barofishserver.order.domain.model.Orders;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentProcessor {
    private final PaymentService paymentService;
    private final PaymentMethodService paymentMethodService;

    public void processPayment(OrderReq request, Orders order) {
        switch (request.getPaymentWay()) {
            case KEY_IN -> processKeyInPayment(request, order);
            default -> throw new BusinessException("지원하지 않는 결제 방식입니다.");
        }
    }

    private void processKeyInPayment(OrderReq request, Orders order) {
        PaymentMethod paymentMethod = paymentMethodService.selectPaymentMethod(request.getPaymentMethodId());
        
        int totalTaxFreeAmount = order.getTotalTaxFreePrice();
        int totalTaxableAmount = order.getTotalPrice() - totalTaxFreeAmount;

        KeyInPaymentReq req = KeyInPaymentReq.builder()
                .paymentMethod(paymentMethod)
                .orderId(order.getId())
                .total_amount(order.getTotalPrice())
                .order_name(request.getName())
                .taxFree(totalTaxFreeAmount)
                .build();

        try {
            Boolean keyInResult = paymentService.processKeyInPayment(req);
            if (!keyInResult) {
                throw new BusinessException("결제에 실패하였습니다.");
            }
        } catch (Exception e) {
            throw new BusinessException("결제에 실패하였습니다.");
        }
    }
} 