package com.matsinger.barofishserver.order.application.service;

import com.matsinger.barofishserver.order.domain.model.BankCode;
import com.matsinger.barofishserver.order.domain.repository.BankCodeRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BankCodeQueryService {
    private BankCodeRepository bankCodeRepository;

    public BankCode findByBankCode(Integer bankCodeId) {
        if (bankCodeId == null) {
            throw new BusinessException("은행 코드 아이디를 입력해주세요.");
        }
        return bankCodeRepository.findById(bankCodeId)
                .orElseThrow(() -> new BusinessException("잘못된 은행 코드 정보입니다."));
    }
}
