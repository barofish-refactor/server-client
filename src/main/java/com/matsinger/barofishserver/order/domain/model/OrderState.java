package com.matsinger.barofishserver.order.domain.model;

public enum OrderState {
    WAIT_DEPOSIT,
    PAYMENT_DONE,
    DELIVERY_READY,
    ON_DELIVERY,
    DELIVERY_DONE,
    EXCHANGE_REQUEST,
    EXCHANGE_ACCEPT,
    FINAL_CONFIRM,
    CANCELED,
    CANCEL_REQUEST,
    REFUND_REQUEST,
    REFUND_ACCEPT,
    REFUND_DONE,
    DELIVERY_DIFFICULT
}
