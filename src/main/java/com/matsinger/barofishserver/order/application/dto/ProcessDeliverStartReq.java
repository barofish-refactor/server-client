package com.matsinger.barofishserver.order.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProcessDeliverStartReq {
    private String deliverCompanyCode;
    private String invoice;
}
