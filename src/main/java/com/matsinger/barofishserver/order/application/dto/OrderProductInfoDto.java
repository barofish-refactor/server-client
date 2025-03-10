package com.matsinger.barofishserver.order.application.dto;

import com.matsinger.barofishserver.order.domain.model.OrderCancelReason;
import com.matsinger.barofishserver.domain.product.optionitem.dto.OptionItemDto;
import com.matsinger.barofishserver.order.application.dto.OrderDto;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.domain.product.domain.ProductDeliverFeeType;
import com.matsinger.barofishserver.domain.product.dto.ProductListDto;

import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;

@Getter
@Builder
public class OrderProductInfoDto {
    private int id;
    private String orderId;
    private int productId;
    private int optionItemId;
    private OptionItemDto optionItem;
    private OrderProductState state;
    private Integer settlePrice;
    private Integer originPrice;
    private int price;
    private int amount;
    private int deliveryFee;
    private ProductDeliverFeeType deliverFeeType;
    private OrderCancelReason cancelReason;
    private String cancelReasonContent;
    private String deliverCompany;
    private String deliverCompanyCode;
    private String invoiceCode;
    private Boolean isSettled;
    private Timestamp settledAt;
    private Boolean needTaxation;
    private Timestamp finalConfirmedAt;
    ProductListDto product;
    private OrderDto order;
    private Float settlementRate;
}
