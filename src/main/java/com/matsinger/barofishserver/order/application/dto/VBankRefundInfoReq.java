package com.matsinger.barofishserver.order.application.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VBankRefundInfoReq {
    String bankHolder;
    Integer bankCodeId;
    String bankAccount;
}
