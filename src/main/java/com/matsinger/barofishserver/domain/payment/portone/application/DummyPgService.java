package com.matsinger.barofishserver.domain.payment.portone.application;

import com.matsinger.barofishserver.domain.payment.dto.CheckValidCardRes;
import com.matsinger.barofishserver.domain.payment.dto.IamPortCertificationRes;
import com.siot.IamportRestClient.request.AgainPaymentData;
import com.siot.IamportRestClient.request.BillingCustomerData;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.Payment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
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
}
