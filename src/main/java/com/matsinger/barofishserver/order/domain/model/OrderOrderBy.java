package com.matsinger.barofishserver.order.domain.model;

public enum OrderOrderBy {
    ordererName("ordererName"), id("id"), email("user.userInfo.email"), phone("orderTel"), receiverName(
            "deliverPlace.receiverName"), address("deliverPlace.address"), postalCode("deliverPlace.postalCode"), couponDiscount(
            "couponDiscount"), totalAmount("totalPrice"), orderAt("orderedAt"), paymentWay("paymentWay");
    public final String label;

    OrderOrderBy(String label) {
        this.label = label;
    }
}
