package com.matsinger.barofishserver.domain.payment.portone.application;

import com.matsinger.barofishserver.domain.payment.dto.CheckValidCardRes;
import com.matsinger.barofishserver.domain.payment.dto.IamPortCertificationRes;
import com.siot.IamportRestClient.request.AgainPaymentData;
import com.siot.IamportRestClient.request.BillingCustomerData;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.Payment;

public interface PgService {
    void cancelPayment(CancelData cancelData);
    Payment getResponse(String uid);
    IamPortCertificationRes certificate(String uid);
    Boolean againPayment(AgainPaymentData againPaymentData);
    CheckValidCardRes processPayment(String uid, BillingCustomerData customerData);
}
