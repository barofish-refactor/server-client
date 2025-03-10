package com.matsinger.barofishserver.order.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@Getter
@NoArgsConstructor
public class VBankRefundInfo {
    String bankHolder;
    String bankCode;
    String bankName;
    String bankAccount;
}
