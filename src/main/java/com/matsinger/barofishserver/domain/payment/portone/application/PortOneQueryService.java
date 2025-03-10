package com.matsinger.barofishserver.domain.payment.portone.application;

import com.matsinger.barofishserver.order.domain.model.BankCode;
import com.matsinger.barofishserver.order.domain.repository.BankCodeRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
