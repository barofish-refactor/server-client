package com.matsinger.barofishserver.domain.payment.application;

import com.matsinger.barofishserver.domain.payment.domain.PaymentState;
import com.matsinger.barofishserver.domain.payment.domain.Payments;
import com.matsinger.barofishserver.domain.order.dto.VBankRefundInfo;
import com.matsinger.barofishserver.domain.payment.dto.IamPortCertificationRes;
import com.matsinger.barofishserver.domain.payment.dto.KeyInPaymentReq;
import com.matsinger.barofishserver.domain.payment.portone.application.PgService;
import com.matsinger.barofishserver.domain.payment.portone.application.PortOneCallbackService;
import com.matsinger.barofishserver.domain.payment.repository.PaymentRepository;
import com.matsinger.barofishserver.utils.AES256;
import com.matsinger.barofishserver.utils.RegexConstructor;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.*;
import com.siot.IamportRestClient.response.Payment;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;

@RequiredArgsConstructor
@Slf4j
@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final AES256 aes256;
    private final RegexConstructor re;
    @Value("${iamport.webhook.url}")
    private String webhookUrl;
    private final PgService pgService;

    public Payments selectPayment(String id) {
        return paymentRepository.findFirstByMerchantUid(id);
    }

    public Payments updatePayment(Payments payment) {
        return paymentRepository.save(payment);
    }

    private PaymentState str2PaymentState(String str) {
        switch (str) {
            case "ready":
                return PaymentState.READY;
            case "paid":
                return PaymentState.PAID;
            case "cancelled":
                return PaymentState.CANCELED;
            case "failed":
                return PaymentState.FAILED;
        }
        return PaymentState.FAILED;
    }

    public Payments getPaymentInfoFromPortOne(String orderId, String impUid) {
        Payment payment = pgService.getResponse(impUid);

        return Payments.builder()
                .orderId(orderId)
                .impUid(impUid)
                .merchantUid(payment.getMerchantUid())
                .payMethod(payment.getPayMethod())
                .paidAmount(payment.getAmount().intValue())
                .status(str2PaymentState(payment.getStatus()))
                .name(payment.getName() != null ? payment.getName() : null)
                .pgProvider(payment.getPgProvider() != null ? payment.getPgProvider() : null)
                .embPgProvider(payment.getEmbPgProvider() != null ? payment.getEmbPgProvider() : null)
                .pgTid(payment.getPgTid() != null ? payment.getPgTid() : null)
                .buyerName(payment.getBuyerName() != null ? payment.getBuyerName() : null)
                .buyerEmail(payment.getBuyerEmail() != null ? payment.getBuyerEmail() : null)
                .buyerTel(payment.getBuyerTel() != null ? payment.getBuyerTel() : null)
                .buyerAddress(payment.getBuyerAddr() != null ? payment.getBuyerAddr() : null)
                .paidAt(payment.getPaidAt() != null ? Timestamp.from(payment.getPaidAt().toInstant()) : null)
                .receiptUrl(payment.getReceiptUrl() != null ? payment.getReceiptUrl() : null)
                .applyNum(payment.getApplyNum() != null ? payment.getApplyNum() : null)
                .vbankNum(payment.getVbankNum() != null ? payment.getVbankNum() : null)
                .vbankCode(payment.getVbankCode() != null ? payment.getVbankCode() : null)
                .vbankName(payment.getVbankName() != null ? payment.getVbankName() : null)
                .vbankHolder(payment.getVbankHolder() != null ? payment.getVbankHolder() : null)
                .vbankDate(payment.getVbankDate() != null ? Timestamp.from(payment.getVbankDate().toInstant()) : null)
                .build();
    }

    public void cancelPayment(String impUid, Integer amount, Integer taxFreeAmount, VBankRefundInfo vBankRefundInfo) {
        CancelData
                cancelData =
                amount != null
                        ? new CancelData(impUid, true, BigDecimal.valueOf(amount))
                        : new CancelData(impUid, true);
        if (taxFreeAmount != null) cancelData.setTax_free(BigDecimal.valueOf(taxFreeAmount));
        if (vBankRefundInfo != null) {
            cancelData.setRefund_holder(vBankRefundInfo.getBankHolder());
            cancelData.setRefund_bank(vBankRefundInfo.getBankCode());
            cancelData.setRefund_account(vBankRefundInfo.getBankAccount());
        }
        pgService.cancelPayment(cancelData);
    }

    public IamPortCertificationRes certificateWithImpUid(String impUid) {
        return pgService.certificate(impUid);
    }

    public Boolean processKeyInPayment(KeyInPaymentReq data) throws IamportResponseException, IOException {
        String cardNo = aes256.decrypt(data.getPaymentMethod().getCardNo());
        cardNo = cardNo.replaceAll(re.cardNo, "$1-$2-$3-$4");
        String[] expiryAyData = data.getPaymentMethod().getExpiryAt().split("/");
        String expiryMonth = expiryAyData[0];
        String expiryYear = "20" + expiryAyData[1];
        String expiry = expiryYear + "-" + expiryMonth;
        String password2Digit = aes256.decrypt(data.getPaymentMethod().getPasswordTwoDigit());
        CardInfo cardInfo = new CardInfo(cardNo, expiry, data.getPaymentMethod().getBirth(), password2Digit);
        AgainPaymentData
                againPaymentData =
                new AgainPaymentData(data.getPaymentMethod().getCustomerUid(),
                        data.getOrderId(),
                        BigDecimal.valueOf(data.getTotal_amount()));
        againPaymentData.setTaxFree(BigDecimal.valueOf(data.getTaxFree()));
        againPaymentData.setName(data.getOrder_name());
        againPaymentData.setNoticeUrl(webhookUrl);

        return pgService.againPayment(againPaymentData);
    }

    public Payments findPaymentByImpUid(String impUid) {
        return paymentRepository.findFirstByImpUid(impUid);
    }

    public void upsertPayments(Payments payments) {
        log.info("payments 저장");
        paymentRepository.save(payments);
    }

    public void save(Payments payments) {
        paymentRepository.save(payments);
    }
}
