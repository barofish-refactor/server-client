package com.matsinger.barofishserver.domain.payment.portone.api;

import com.matsinger.barofishserver.domain.basketProduct.application.BasketCommandService;
import com.matsinger.barofishserver.domain.coupon.application.CouponCommandService;
import com.matsinger.barofishserver.domain.notification.application.NotificationCommandService;
import com.matsinger.barofishserver.domain.notification.dto.NotificationMessage;
import com.matsinger.barofishserver.domain.notification.dto.NotificationMessageType;
import com.matsinger.barofishserver.order.application.service.OrderService;
import com.matsinger.barofishserver.order.domain.model.OrderPaymentWay;
import com.matsinger.barofishserver.order.domain.model.OrderState;
import com.matsinger.barofishserver.order.domain.model.Orders;
import com.matsinger.barofishserver.order.application.dto.GetCancelPriceDto;
import com.matsinger.barofishserver.order.application.dto.VBankRefundInfo;
import com.matsinger.barofishserver.order.domain.model.OrderCancelReason;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.domain.payment.application.PaymentService;
import com.matsinger.barofishserver.domain.payment.domain.PaymentState;
import com.matsinger.barofishserver.domain.payment.domain.Payments;
import com.matsinger.barofishserver.domain.payment.dto.GetVBankAccountReq;
import com.matsinger.barofishserver.domain.payment.portone.dto.PortOneBodyData;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.user.application.UserCommandService;
import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import com.matsinger.barofishserver.utils.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
//@RequestMapping("/callback/iamport_pay_result")
@RequestMapping("v1/callback/iamport_pay_result")
public class PortOneCallbackHandler {
    private final PaymentService paymentService;
    private final UserCommandService userService;
    private final OrderService orderService;
    private final ProductService productService;
    private final CouponCommandService couponCommandService;
    private final SmsService sms;
    private final NotificationCommandService notificationCommandService;
    private final BasketCommandService basketCommandService;


    @PostMapping("")
    public ResponseEntity<Object> portOneCallback(@RequestHeader(value = "x-real-ip", required = false) String XRealIp,
                                                  @RequestBody(required = false) PortOneBodyData data) {
        System.out.println("Portone callback received");
        if (!XRealIp.equals("52.78.100.19") && !XRealIp.equals("52.78.48.223"))
            return ResponseEntity.status(403).body(null);

        try {
            Orders order = orderService.selectOrder(data.getMerchant_uid());
            if (order != null) {
                if (data.getStatus().equals("ready")) {
                    Payments paymentData = paymentService.getPaymentInfoFromPortOne(order.getId(), data.getImp_uid());
                    GetVBankAccountReq
                            vBankReq =
                            GetVBankAccountReq.builder().orderId(order.getId()).price(order.getTotalPrice()).vBankCode(
                                    paymentData.getVbankCode()).vBankDue(Math.toIntExact((paymentData.getVbankDate().getTime() /
                                    1000))).vBankHolder(paymentData.getVbankHolder()).build();
                    paymentService.upsertPayments(paymentData);
                    order.setImpUid(data.getImp_uid());
                    orderService.updateOrder(order);
                    if (paymentData.getPayMethod().equals("vbank")) {
                        String
                                smsContent =
                                "[바로피쉬] 가상계좌 결제 요청\n" +
                                        String.format("주문번호: %s\n", data.getMerchant_uid()) +
                                        String.format("결제 금액: %d원\n", paymentData.getPaidAmount()) +
                                        String.format("가상계좌은행: %s\n", paymentData.getVbankName()) +
                                        String.format("가상계좌번호: %s\n",
                                                paymentData.getVbankNum().replaceAll("[^\\d]", "") + "\n") +
                                        String.format("가상계좌 예금주명: %s\n", paymentData.getVbankHolder()) +
                                        "24시간 이내로 이체해주세요.";
                        sms.sendSms(paymentData.getBuyerTel(), smsContent, "가상 계좌 결제 요청");
                    }
                } else if (data.getStatus().equals("paid")) {
                    Payments paymentData = paymentService.getPaymentInfoFromPortOne(order.getId(), data.getImp_uid());
                    List<OrderProductInfo> infos = orderService.selectOrderProductInfoListWithOrderId(order.getId());
                    infos.forEach(info -> {
                        if (!orderService.canDeliver(order.getDeliverPlace(), info)) {
                            int cancelPrice = 0;
                            try {
                                GetCancelPriceDto
                                        cancelData =
                                        orderService.getCancelPrice(order, List.of(info));
                                cancelPrice = cancelData.getCancelPrice();
                            } catch (Exception e) {
                                System.out.println(e);
                            }
                            try {
//                                int taxFreeAmount = info.getTaxFreeAmount();
                                int
                                        taxFreeAmount =
                                        info.getTaxFreeAmount() != 0 &&
                                                info.getTaxFreeAmount() != null ? info.getPrice() : 0;
                                VBankRefundInfo vBankRefundInfo =
                                        order.getPaymentWay().equals(OrderPaymentWay.VIRTUAL_ACCOUNT)
                                                ? VBankRefundInfo.builder()
                                                    .bankHolder(order.getBankHolder())
                                                    .bankCode(order.getBankCode())
                                                    .bankName(order.getBankName())
                                                    .bankAccount(order.getBankAccount())
                                                    .build()
                                                : null;
                                paymentService.cancelPayment(data.getImp_uid(),
                                        cancelPrice,
                                        taxFreeAmount,
                                        vBankRefundInfo);
                            } catch (Exception e) {

                                System.out.println(e);
                            }
                            info.setCancelReasonContent("배송 불가 지역");
                            info.setCancelReason(OrderCancelReason.ORDER_FAULT);
                            info.setState(OrderProductState.CANCELED);
                            notificationCommandService.sendFcmToUser(order.getUserId(),
                                    NotificationMessageType.ORDER_CANCEL,
                                    NotificationMessage.builder().productName(info.getProduct().getTitle()).isCanceledByRegion(
                                            true).build());
                        } else {
                            OptionItem optionItem = productService.selectOptionItem(info.getOptionItemId());
                            if (optionItem.getAmount() != null)
                                optionItem.setAmount(optionItem.getAmount() - info.getAmount());
                            productService.addOptionItem(optionItem);
                            info.setState(OrderProductState.PAYMENT_DONE);
                            notificationCommandService.sendFcmToUser(order.getUserId(),
                                    NotificationMessageType.PAYMENT_DONE,
                                    NotificationMessage.builder().productName(info.getProduct().getTitle()).build());
                        }
                    });
                    boolean allCanceled = infos.stream().allMatch(v -> v.getState().equals(OrderProductState.CANCELED));
                    if (!allCanceled) {
                        order.setState(OrderState.PAYMENT_DONE);
                        order.setImpUid(data.getImp_uid());
                        UserInfo userInfo = userService.selectUserInfo(order.getUserId());
                        userInfo.setPoint(userInfo.getPoint() - order.getUsePoint());
                        userService.updateUserInfo(userInfo);
                        couponCommandService.useCoupon(order.getUserId(), order.getCouponId());
                    } else {
                        order.setState(OrderState.PAYMENT_DONE);
                        order.setImpUid(data.getImp_uid());
                    }

                    basketCommandService.deleteBasketAfterOrder(order, infos);
                    orderService.updateOrderProductInfo(infos);
                    orderService.updateOrder(order);
                    paymentService.upsertPayments(paymentData);
                } else if (data.getStatus().equals("canceled")) {
                    Payments payment = paymentService.findPaymentByImpUid(data.getImp_uid());
                    payment.setStatus(PaymentState.CANCELED);
                    order.setState(OrderState.CANCELED);
                    orderService.updateOrder(order);
                    paymentService.upsertPayments(payment);
                }
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return ResponseEntity.ok(null);
    }
}
