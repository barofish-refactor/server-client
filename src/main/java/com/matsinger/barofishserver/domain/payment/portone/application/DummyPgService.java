package com.matsinger.barofishserver.domain.payment.portone.application;

import com.matsinger.barofishserver.order.domain.model.BankCode;
import com.matsinger.barofishserver.domain.payment.dto.CheckValidCardRes;
import com.matsinger.barofishserver.domain.payment.dto.IamPortCertificationRes;
import com.matsinger.barofishserver.domain.payment.portone.dto.PortOneVbankHolderResponse;
import com.siot.IamportRestClient.request.AgainPaymentData;
import com.siot.IamportRestClient.request.BillingCustomerData;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.Payment;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Service
@Profile({"local", "default"})
public class DummyPgService implements PgService {
    @Override
    public void cancelPayment(CancelData cancelData) {
    }

    @Override
    public Payment getResponse(String uid) {
        return null;
    }

    @Override
    public IamPortCertificationRes certificate(String uid) {
        return null;
    }

    @Override
    public Boolean againPayment(AgainPaymentData againPaymentData) {
        return null;
    }

    @Override
    public CheckValidCardRes processPayment(String uid, BillingCustomerData customerData) {
        return null;
    }

    @Override
    public String checkVbankAccountVerification(BankCode bankCode, String bankNum, String holderName) {
        return null;
    }

    @Override
    public ResponseEntity<PortOneVbankHolderResponse> checkBankAccountValid(URI uri, RestTemplate restTemplate, HttpEntity request) {
        return null;
    }
}
