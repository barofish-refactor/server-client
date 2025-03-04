package com.matsinger.barofishserver.domain.payment.portone.application;

import com.matsinger.barofishserver.domain.order.domain.BankCode;
import com.matsinger.barofishserver.domain.payment.dto.CheckValidCardRes;
import com.matsinger.barofishserver.domain.payment.dto.IamPortCertificationRes;
import com.matsinger.barofishserver.domain.payment.portone.dto.PortOneVbankHolderResponse;
import com.siot.IamportRestClient.request.AgainPaymentData;
import com.siot.IamportRestClient.request.BillingCustomerData;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.Payment;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

public interface PgService {
    void cancelPayment(CancelData cancelData);
    Payment getResponse(String uid);
    IamPortCertificationRes certificate(String uid);
    Boolean againPayment(AgainPaymentData againPaymentData);
    CheckValidCardRes processPayment(String uid, BillingCustomerData customerData);

    String checkVbankAccountVerification(BankCode bankCode, String bankNum, String holderName);

    ResponseEntity<PortOneVbankHolderResponse> checkBankAccountValid(URI uri, RestTemplate restTemplate, HttpEntity request);
}
