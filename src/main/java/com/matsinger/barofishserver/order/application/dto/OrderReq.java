package com.matsinger.barofishserver.order.application.dto;

import com.matsinger.barofishserver.order.domain.model.OrderPaymentWay;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class OrderReq {
    private String name;
    private String tel;
    private Integer couponId;
    private OrderPaymentWay paymentWay;
    private Integer point;
    private Integer totalPrice;
    private Integer totalDeliveryFee;
    private Integer couponDiscountPrice;
    private List<OrderProductReq> products;
    private Integer taxFreeAmount;
    private Integer deliverPlaceId;
    private Integer paymentMethodId;
    private VBankRefundInfoReq vbankRefundInfo;
}
