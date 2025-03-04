package com.matsinger.barofishserver.domain.payment.portone.application;

import com.matsinger.barofishserver.domain.order.domain.BankCode;
import com.matsinger.barofishserver.domain.order.repository.BankCodeRepository;
import com.matsinger.barofishserver.domain.payment.portone.dto.PortOneAccessKeyRequest;
import com.matsinger.barofishserver.domain.payment.portone.dto.PortOneAccessKeyResponse;
import com.matsinger.barofishserver.domain.payment.portone.dto.PortOneVbankHolderResponse;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class PortOneQueryService {

    private final BankCodeRepository bankCodeRepository;
    private final PgService pgService;
    public String checkVbankAccountVerification(Integer bankCodeId, String bankNum, String holderName) {
        BankCode bankCode = bankCodeRepository.findById(bankCodeId)
                .orElseThrow(() -> new BusinessException("은행 코드를 찾을 수 없습니다."));
        return pgService.checkVbankAccountVerification(bankCode, bankNum, holderName);
    }
}
