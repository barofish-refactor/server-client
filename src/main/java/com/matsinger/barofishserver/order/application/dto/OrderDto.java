package com.matsinger.barofishserver.order.application.dto;

import com.matsinger.barofishserver.order.domain.model.OrderPaymentWay;
import com.matsinger.barofishserver.order.domain.model.OrderState;
import com.matsinger.barofishserver.domain.userinfo.dto.UserInfoDto;
import lombok.*;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    String id;
    Integer taxFreeAmount;
    UserInfoDto user;
    OrderState state;
    String ordererName;
    String ordererTel;
    OrderPaymentWay paymentWay;
    Integer originTotalPrice;
    Integer totalAmount;
    Integer couponDiscount;
    String couponName;
    Integer usePoint;
    Timestamp orderedAt;
    // Boolean needTaxation;
    String bankHolder;
    String bankCode;
    String bankAccount;
    String bankName;
    List<OrderProductDto> productInfos;
    OrderDeliverPlaceDto deliverPlace;
}
