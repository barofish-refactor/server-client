package com.matsinger.barofishserver.order.application.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class OrderStoreInquiryDto {

    private List<OrderProductInquiryDto> products;
}
