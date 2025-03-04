package com.matsinger.barofishserver.domain.payment.portone.application;

import com.matsinger.barofishserver.domain.order.domain.BankCode;
import com.matsinger.barofishserver.domain.payment.dto.CheckValidCardRes;
import com.matsinger.barofishserver.domain.payment.dto.IamPortCertificationRes;
import com.matsinger.barofishserver.domain.payment.portone.dto.PortOneAccessKeyRequest;
import com.matsinger.barofishserver.domain.payment.portone.dto.PortOneAccessKeyResponse;
import com.matsinger.barofishserver.domain.payment.portone.dto.PortOneVbankHolderResponse;
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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.stream.Stream;

@Slf4j
@Service
@Profile("!local")
public class PgServiceImpl implements PgService {

    @Value("${iamport.credentials.apiKey}")
    private String accessKey;

    @Value("${iamport.credentials.secretKey}")
    private String secretKey;

    @Value("${iamport.credentials.keyin-pg}")
    public String keyinPg;

    @Value("${iamport.credentials.mid}")
    public String mid;

    @Value("${iamport.webhook.url}")
    private String webhookUrl;
    private String portOneUrl = "https://api.iamport.kr/";
    private String accessToken;

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
        againPaymentData.setNoticeUrl(webhookUrl);
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
        customerData.setPg(keyinPg);
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

    @Override
    public String checkVbankAccountVerification(BankCode bankCode, String bankNum, String holderName) {
        URI uri = UriComponentsBuilder
                .fromUriString(portOneUrl)
                .path("vbanks/holder")
                .queryParam("bank_code", bankCode.getCode())
                .queryParam("bank_num", bankNum)
                .encode()
                .build()
                .toUri();

        generateAccessToken();
        return sendPortOneAccountInfo(uri, holderName);
    }

    private String sendPortOneAccountInfo(URI uri, String holderName) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        HttpEntity request = new HttpEntity(headers);

        String errorMessage = null;
        int trialCount = 0;
        try {
            ResponseEntity<PortOneVbankHolderResponse> responseEntity = checkBankAccountValid(uri, restTemplate, request);
            String bankHolderResponse = responseEntity.getBody().getResponse().getBank_holder();
            if (!bankHolderResponse.equals(holderName)) {
                errorMessage = "계좌 소유주 명이 일치하지 않습니다.";
            }
        } catch (HttpClientErrorException e) {
            trialCount++;
            if (trialCount < 2) {
                if (e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                    generateAccessToken();
                    ResponseEntity<PortOneVbankHolderResponse> responseEntity = checkBankAccountValid(uri, restTemplate, request);
                    String bankHolderResponse = responseEntity.getBody().getResponse().getBank_holder();
                    if (!bankHolderResponse.equals(holderName)) {
                        errorMessage = "계좌 소유주 명이 일치하지 않습니다.";
                    }
                }
            }

            if (e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                errorMessage = "은행, 또는 계좌번호를 입력해주세요.";
            }
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                errorMessage = "계좌 번호가 일치하지 않습니다.";
            }
        }
        return errorMessage;
    }

    @Override
    public ResponseEntity<PortOneVbankHolderResponse> checkBankAccountValid(URI uri, RestTemplate restTemplate, HttpEntity request) {
        ResponseEntity<PortOneVbankHolderResponse> responseEntity = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                request,
                PortOneVbankHolderResponse.class
        );
        return responseEntity;
    }

    public void generateAccessToken() {
        URI uri = UriComponentsBuilder
                .fromUriString(portOneUrl)
                .path("users/getToken")
                .encode()
                .build()
                .toUri();
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        PortOneAccessKeyRequest requestDto = new PortOneAccessKeyRequest(accessKey, secretKey);
        HttpEntity<PortOneAccessKeyRequest> request = new HttpEntity<>(requestDto, headers);

        ResponseEntity<PortOneAccessKeyResponse> responseEntity = restTemplate.exchange(
                uri,
                HttpMethod.POST,
                request,
                PortOneAccessKeyResponse.class
        );

        this.accessToken = responseEntity.getBody().getResponse().getAccess_token();
    }
}
