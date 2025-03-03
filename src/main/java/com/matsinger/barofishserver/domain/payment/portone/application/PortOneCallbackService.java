package com.matsinger.barofishserver.domain.payment.portone.application;

import com.matsinger.barofishserver.domain.payment.dto.CheckValidCardRes;
import com.matsinger.barofishserver.domain.payment.dto.IamPortCertificationRes;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.request.AgainPaymentData;
import com.siot.IamportRestClient.request.BillingCustomerData;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.BillingCustomer;
import com.siot.IamportRestClient.response.Certification;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Slf4j
@Service
@Profile("!local")
public class PortOneCallbackService implements PgService {

    @Value("${iamport.credentials.apiKey}")
    private String accessKey;

    @Value("${iamport.credentials.secretKey}")
    private String secretKey;

    private IamportClient iamportClient;

    @PostConstruct
    public void init() {
        iamportClient = new IamportClient(accessKey, secretKey);
    }

    @Override
    public void cancelPayment(CancelData cancelData) {
        try {
            IamportResponse<Payment> cancelResult = iamportClient.cancelPaymentByImpUid(cancelData);
            if (cancelResult.getCode() != 0) {
                log.error("포트원 환불 실패 메시지 = {}", cancelResult.getMessage());
                log.error("포트원 환불 실패 코드 = {}", cancelResult.getCode());
                throw new BusinessException("환불에 실패하였습니다.");
            }
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public Payment getResponse(String uid) {
        try {
            return iamportClient.paymentByImpUid(uid).getResponse();
        } catch (Exception e) {
            throw new BusinessException("결제 정보를 가져오는데 실패했습니다.");
        }
    }

    @Override
    public IamPortCertificationRes certificate(String uid) {
        IamportResponse<Certification> response =
                Stream.of(uid)
                        .map(u -> {
                            try {
                                IamportResponse<Certification> res = iamportClient.certificationByImpUid(u);
                                if (res.getCode() != 0) {
                                    throw new BusinessException(String.format("환불에 실패했습니다 : %s", res.getMessage()));
                                }
                                return res;
                            } catch (Exception e) {
                                throw new RuntimeException("환불에 실패했습니다.", e);
                            }
                        })
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("알 수 없는 오류 발생"));


        Certification certification = response.getResponse();
        return IamPortCertificationRes.builder()
                .impUid(certification.getImpUid())
                .name(certification.getName())
                .phone(certification.getPhone())
                .certified(certification.isCertified())
                .certifiedAt(certification.getCertifiedAt().toString()
                ).build();
    }

    @Override
    public Boolean againPayment(AgainPaymentData againPaymentData) {
        IamportResponse<Payment> response = Stream.of(againPaymentData)
                .map(data -> {
                    try {
                        return iamportClient.againPayment(againPaymentData);
                    } catch (Exception e) {
                        throw new BusinessException("결제에 실패했습니다.");
                    }
                })
                .findFirst()
                .orElseThrow(() -> new BusinessException("결제에 실패했습니다."));

        if (response.getCode() != 0) {
            System.out.println(response.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public CheckValidCardRes processPayment(String uid, BillingCustomerData customerData) {
        try {
            IamportResponse<BillingCustomer> portoneResponse = iamportClient.postBillingCustomer(uid, customerData);
            BillingCustomer billingCustomer = portoneResponse.getResponse();
            if (portoneResponse.getCode() != 0
                    || billingCustomer == null) {
                log.error(portoneResponse.getCode() + ": " + portoneResponse.getMessage());
                return null;
            }
            return CheckValidCardRes.builder()
                    .cardName(billingCustomer.getCardName())
                    .customerUid(billingCustomer.getCustomerUid())
                    .build();
        } catch (Exception e) {
            throw new BusinessException("결제 요청에 실패했습니다.");
        }
    }
}
