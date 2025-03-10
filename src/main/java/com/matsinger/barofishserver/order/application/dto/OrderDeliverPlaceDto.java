package com.matsinger.barofishserver.order.application.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliverPlaceDto {
    private String orderId;
    private String name;
    private String receiverName;
    private String tel;
    private String postalCode;
    private String address;
    private String addressDetail;
    private String deliverMessage;
}
