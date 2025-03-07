package com.matsinger.barofishserver.order.application.dto;

import com.matsinger.barofishserver.order.domain.model.OrderPaymentWay;
import com.matsinger.barofishserver.order.domain.model.OrderState;
import com.matsinger.barofishserver.domain.userinfo.dto.UserInfoDto;
import com.matsinger.barofishserver.order.domain.model.Orders;
import lombok.*;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private String id;
    private Integer taxFreeAmount;
    private UserInfoDto user;
    private OrderState state;
    private String ordererName;
    private String ordererTel;
    private OrderPaymentWay paymentWay;
    private Integer originTotalPrice;
    private Integer totalAmount;
    private Integer couponDiscount;
    private String couponName;
    private Integer usePoint;
    private Timestamp orderedAt;
    // Boolean needTaxation;
    private String bankHolder;
    private String bankCode;
    private String bankAccount;
    private String bankName;
    private List<OrderProductDto> productInfos;
    private OrderDeliverPlaceDto deliverPlace;

    public static OrderDto from(Orders order, List<OrderProductDto> productInfos) {
        return OrderDto.builder()
                .id(order.getId())
                .state(order.getState())
                .ordererName(order.getOrdererName())
                .ordererTel(order.getOrdererTel())
                .paymentWay(order.getPaymentWay())
                .originTotalPrice(order.getOriginTotalPrice())
                .totalAmount(order.getTotalPrice())
                .couponDiscount(order.getCouponDiscount())
                .usePoint(order.getUsePoint())
                .orderedAt(order.getOrderedAt())
                .bankHolder(order.getBankHolder())
                .bankCode(order.getBankCode())
                .bankAccount(order.getBankAccount())
                .bankName(order.getBankName())
                .productInfos(productInfos)
                .build();
    }
}
