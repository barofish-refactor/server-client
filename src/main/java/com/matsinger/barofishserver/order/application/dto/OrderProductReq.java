package com.matsinger.barofishserver.order.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class OrderProductReq {
    Integer productId;
    Integer optionId;
    Integer amount;
    Boolean needTaxation;
    Integer deliveryFee;
    Integer taxFreeAmount;
}
