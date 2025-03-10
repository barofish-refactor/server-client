package com.matsinger.barofishserver.order.domain.model;

import com.matsinger.barofishserver.global.exception.BusinessException;

public enum OrderPaymentWay {
    CARD("신용카드"),
    KEY_IN("키인결제"),
    NAVER("네이버페이"),
    KAKAO_PAY("카카오페이"),
    PHONE("휴대폰결제"),
    DEPOSIT("계좌이체"),
    VIRTUAL_ACCOUNT("가상계좌"),
    TOSS_PAY("토스페이");

    private final String orderPaymentWay;

    OrderPaymentWay(String orderPaymentWay) {
        this.orderPaymentWay = orderPaymentWay;
    }

    public static String findByOrderPaymentWay(OrderPaymentWay orderPaymentWay) {
        for (OrderPaymentWay paymentWay : values()) {
            if (orderPaymentWay.equals(paymentWay)) {
                return paymentWay.name();
            }
        }
        throw new BusinessException("결제 수단을 찾을 수 없습니다.");
    }
}
