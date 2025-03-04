package com.matsinger.barofishserver.domain.user.paymentMethod.application;

import com.matsinger.barofishserver.domain.payment.dto.CheckValidCardRes;
import com.matsinger.barofishserver.domain.payment.portone.application.PgService;
import com.matsinger.barofishserver.domain.user.dto.AddPaymentMethodReq;
import com.matsinger.barofishserver.domain.user.paymentMethod.domain.PaymentMethod;
import com.matsinger.barofishserver.domain.user.paymentMethod.repository.PaymentMethodRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.utils.AES256;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.RegexConstructor;
import com.siot.IamportRestClient.request.BillingCustomerData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMethodCommandService {

    private final PaymentMethodRepository paymentMethodRepository;

    private final Common util;
    private final AES256 aes256;
    private final RegexConstructor re;
    private final PgService pgService;

    public PaymentMethod addPaymentMethod(AddPaymentMethodReq request, int userId) {

        String name = util.validateString(request.getName(), 20L, "이름");

        String AdjustedCardNo = request.getCardNo().replaceAll(re.cardNo, "$1$2$3$4");
        String hashedCardNo = aes256.encrypt(AdjustedCardNo);
        validateCardInfo(request, userId, hashedCardNo);

        if (request.getPasswordTwoDigit() == null) {
            throw new BusinessException("비밀번호 두자리를 입력해주세요.");
        }
        if (!Pattern.matches(re.cardPassword, request.getPasswordTwoDigit())) {
            throw new BusinessException("비밀번호는 숫자 2자리입니다.");
        }
        String password2Digit = aes256.encrypt(request.getPasswordTwoDigit());

        PaymentMethod paymentMethod = PaymentMethod.builder().name(name).cardNo(hashedCardNo).userId(userId)
                .expiryAt(request.getExpiryAt()).birth(
                        request.getBirth())
                .passwordTwoDigit(password2Digit).cardName("").customerUid("").build();
        paymentMethod = paymentMethodRepository.save(paymentMethod);
        CheckValidCardRes validCardRes = checkValidCard(paymentMethod);
        if (validCardRes == null) {
            paymentMethodRepository.deleteById(paymentMethod.getId());
            throw new BusinessException("유효하지 않은 카드입니다.");
        }
        paymentMethod.setCardName(validCardRes.getCardName());
        paymentMethod.setCustomerUid(validCardRes.getCustomerUid());
        return paymentMethodRepository.save(paymentMethod);
    }

    private void validateCardInfo(AddPaymentMethodReq request, int userId, String hashedCardNo) {
        if (request.getCardNo() == null) {
            throw new BusinessException("카드번호를 입력해주세요.");
        }
        if (!Pattern.matches(re.cardNo, request.getCardNo())) {
            throw new BusinessException("카드번호 형식을 확인해주세요.");
        }
        if (paymentMethodRepository.existsByCardNoAndUserId(hashedCardNo, userId)) {
            throw new BusinessException("이미 등록된 카드입니다.");
        }
        if (request.getExpiryAt() == null) {
            throw new BusinessException("유효기간(월/년) 입력해주세요.");
        }
        if (!Pattern.matches(re.expiryAt, request.getExpiryAt())) {
            throw new BusinessException("유효기간 형식을 확인해주세요.");
        }
        if (request.getBirth() == null) {
            throw new BusinessException("생년월일을 입력해주세요");
        }
        if (!Pattern.matches(re.birth, request.getBirth())) {
            throw new BusinessException("생년월일 형식을 확인해주세요.");
        }
    }

    private CheckValidCardRes checkValidCard(PaymentMethod paymentMethod) {
        String customerUid = "customer_" + paymentMethod.getUserId() + "_" + paymentMethod.getId();
        String cardNo = aes256.decrypt(paymentMethod.getCardNo());
        cardNo = cardNo.replaceAll(re.cardNo, "$1-$2-$3-$4");
        String[] expiryAyData = paymentMethod.getExpiryAt().split("/");
        String expiryMonth = expiryAyData[0];
        String expiryYear = "20" + expiryAyData[1];
        String expiry = expiryYear + "-" + expiryMonth;
        String birth = paymentMethod.getBirth();
        BillingCustomerData billingCustomerData = new BillingCustomerData(customerUid, cardNo, expiry, birth);
        billingCustomerData.setPwd2Digit(aes256.decrypt(paymentMethod.getPasswordTwoDigit()));

        return pgService.processPayment(customerUid, billingCustomerData);
    }
}
