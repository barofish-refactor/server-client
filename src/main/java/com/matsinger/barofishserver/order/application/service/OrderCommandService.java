package com.matsinger.barofishserver.order.application.service;

import com.matsinger.barofishserver.domain.coupon.application.CouponCommandService;
import com.matsinger.barofishserver.domain.notification.application.NotificationCommandService;
import com.matsinger.barofishserver.domain.notification.dto.NotificationMessage;
import com.matsinger.barofishserver.domain.notification.dto.NotificationMessageType;
import com.matsinger.barofishserver.domain.payment.domain.PaymentState;
import com.matsinger.barofishserver.domain.payment.domain.Payments;
import com.matsinger.barofishserver.domain.payment.dto.CancelManager;
import com.matsinger.barofishserver.domain.payment.portone.application.PgService;
import com.matsinger.barofishserver.domain.payment.repository.PaymentRepository;
import com.matsinger.barofishserver.domain.product.application.ProductQueryService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.optionitem.application.OptionItemQueryService;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.product.optionitem.repository.OptionItemRepository;
import com.matsinger.barofishserver.domain.userinfo.application.UserInfoQueryService;
import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import com.matsinger.barofishserver.domain.userinfo.repository.UserInfoRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.order.application.dto.OrderReq;
import com.matsinger.barofishserver.order.application.dto.OrderResponse;
import com.matsinger.barofishserver.order.application.dto.RequestCancelReq;
import com.matsinger.barofishserver.order.application.dto.VBankRefundInfo;
import com.matsinger.barofishserver.order.domain.model.*;
import com.matsinger.barofishserver.order.domain.repository.OrderProductInfoRepository;
import com.matsinger.barofishserver.order.domain.repository.OrderRepository;
import com.matsinger.barofishserver.order.domain.service.OrderCreationHandler;
import com.matsinger.barofishserver.payment.application.PaymentFacade;
import com.matsinger.barofishserver.utils.Common;
import com.siot.IamportRestClient.request.CancelData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCommandService {

    private final OrderService orderService;
    private final Common utils;
    private final ProductQueryService productQueryService;
    private final UserInfoQueryService userInfoQueryService;
    private final OrderRepository orderRepository;
    private final OptionItemQueryService optionItemQueryService;
    private final NotificationCommandService notificationCommandService;
    private final OrderProductInfoRepository orderProductInfoRepository;
    private final OrderProductInfoQueryService orderProductInfoQueryService;
    private final CouponCommandService couponCommandService;
    private final UserInfoRepository userInfoRepository;
    private final PaymentRepository paymentRepository;
    private final OptionItemRepository optionItemRepository;
    private final PgService pgService;
    private final OrderCreationHandler orderCreationHandler;
    private final PaymentFacade paymentFacade;

    @Transactional
    public OrderResponse proceedOrder(OrderReq request, Integer userId) {
        // 주문 생성 (별도 트랜잭션)
        OrderCreationResult orderResult = orderCreationHandler.createOrder(request, userId);

        // 결제 처리 (별도 트랜잭션)
        if (orderResult.getOrder().getState() != OrderState.DELIVERY_DIFFICULT) {
            paymentFacade.processPayment(request, orderResult.getOrder());
        }

        return OrderResponse.from(orderResult);
    }

    public void setVbankInfo(OrderReq request, Orders orders) {
        if (request.getPaymentWay().equals(OrderPaymentWay.VIRTUAL_ACCOUNT)) {
            if (request.getVbankRefundInfo() == null)
                throw new BusinessException("가상계좌 환불 정보를 입력해주세요.");
            if (request.getVbankRefundInfo().getBankCodeId() == null)
                throw new BusinessException("은행 코드 아이디를 입력해주세요.");
            BankCode bankCode = orderService.selectBankCode(request.getVbankRefundInfo().getBankCodeId());
            String bankHolder = utils.validateString(request.getVbankRefundInfo().getBankHolder(), 20L, "환불 예금주명");
            String bankAccount = request.getVbankRefundInfo().getBankAccount().replaceAll("-", "");
            bankAccount = utils.validateString(bankAccount, 30L, "환불 계좌번호");

            orders.setVbankRefundInfo(bankCode.getCode(), bankHolder, bankCode.getName(), bankAccount);
        }
    }

    @Transactional
    public void cancelOrder(TokenInfo tokenInfo, List<Integer> orderProductInfoIds, RequestCancelReq request) {

        List<OrderProductInfo> cancelRequested = orderProductInfoRepository.findAllById(orderProductInfoIds);

        List<Integer> uniqueStoreIds = cancelRequested.stream()
                .map(v -> v.getStoreId())
                .distinct()
                .toList();

        List<OrderProductInfo> storeOrderProducts = cancelRequested.stream()
                .filter(v -> uniqueStoreIds.contains(v.getStoreId()))
                .toList();

        if (tokenInfo.getType().equals(TokenAuthType.PARTNER)) {
            if (uniqueStoreIds.size() > 1) {
                throw new BusinessException("타 파트너사의 주문과 같이 있어 취소 불가합니다.");
            }
        }

        OrderProductInfo orderProductInfo = cancelRequested.get(0);
        Orders order = orderProductInfo.getOrder();

        TokenAuthType authType = tokenInfo.getType();

        if (authType.equals(TokenAuthType.USER)) {
            validateRequest(tokenInfo.getId(), request, order);
        }

        List<OrderProductInfo> allOrderProducts = orderProductInfoRepository.findAllByOrderId(order.getId());

        log.info("isCouponUsed = {}", order.isCouponUsed());
        int seq = 1;
        String firstProductTitle = null;
        for (Integer storeId : uniqueStoreIds) {
            log.info("storeId = {}", storeId);

            if (order.isCouponUsed() && !order.getState().equals(OrderState.WAIT_DEPOSIT)) {
                CancelManager cancelManager = new CancelManager(
                        order, allOrderProducts, List.of());

                boolean isCancelable = allOrderProducts.stream()
                        .noneMatch(v -> !v.getState().equals(OrderProductState.PAYMENT_DONE));
                if (!isCancelable) {
                    throw new BusinessException("출고, 배송중, 배송이 완료된 상품이 있어 취소가 불가능합니다.");
                }
                cancel(order, cancelManager, request, authType);
                break;
            }
            if (order.getState().equals(OrderState.WAIT_DEPOSIT)) {
                CancelManager cancelManager = new CancelManager(
                        order, allOrderProducts, List.of());
                cancel(order, cancelManager, request, authType);
                break;
            }

            if (!order.isCouponUsed()) {
                List<OrderProductInfo> tobeCanceled = allOrderProducts.stream()
                        .filter(v -> !OrderProductState.isCanceled(v.getState()))
                        .filter(v -> v.getStoreId() == storeId)
                        .toList();
                List<OrderProductInfo> notTobeCanceled = allOrderProducts.stream()
                        .filter(v -> !OrderProductState.isCanceled(v.getState()))
                        .filter(v -> v.getStoreId() != storeId)
                        .toList();
                log.info("tobeCanceled = {}", tobeCanceled.stream().map(v -> v.getProduct().getTitle()).toList().toString());
                log.info("notTobeCanceled = {}", notTobeCanceled.stream().map(v -> v.getProduct().getTitle()).toList().toString());

                CancelManager cancelManager = new CancelManager(
                        order, tobeCanceled, notTobeCanceled);

                cancel(order, cancelManager, request, authType);
            }

            Product product = productQueryService.findById(orderProductInfo.getProductId());
            OptionItem optionItem = optionItemQueryService.findById(orderProductInfo.getOptionItemId());
            if (seq == 1) {
                firstProductTitle = product.getTitle() + " " + optionItem.getName();
            }
            seq++;
        }

        log.info("OrderCommandService.productName = {}", firstProductTitle);

        notificationCommandService.sendFcmToUser(
                order.getUserId(),
                convertType(authType),
                NotificationMessage.builder()
                        .productName(firstProductTitle)
                        .isCanceledByRegion(false)
                        .build()
        );
    }

    private NotificationMessageType convertType(TokenAuthType authType) {
        if (authType.equals(TokenAuthType.USER)) {
            return NotificationMessageType.ORDER_CANCEL;
        }
        if (authType.equals(TokenAuthType.PARTNER)) {
            return NotificationMessageType.CANCELED_BY_PARTNER;
        }
        if (authType.equals(TokenAuthType.ADMIN)) {
            return NotificationMessageType.CANCELED_BY_ADMIN;
        }
        throw new BusinessException("토큰 타입이 유효하지 않습니다." + "\n" + "다시 로그인해 주세요.");
    }

    private void cancel(Orders order,
                        CancelManager cancelManager,
                        RequestCancelReq request,
                        TokenAuthType authType) {
        OrderProductState state = null;
        if (authType.equals(TokenAuthType.PARTNER)) {
            state = OrderProductState.CANCELED_BY_PARTNER;
        }
        if (authType.equals(TokenAuthType.ADMIN)) {
            state = OrderProductState.CANCELED_BY_ADMIN;
        }
        if (authType.equals(TokenAuthType.USER)) {
            state = OrderProductState.CANCELED;
        }

        if (order.getState().equals(OrderState.WAIT_DEPOSIT)) {
            List<OrderProductInfo> orderProductInfos = orderProductInfoQueryService.findAllByOrderId(order.getId());
            OrderProductState finalState = state;
            orderProductInfos.forEach(v -> v.setState(finalState));
            orderProductInfoRepository.saveAll(cancelManager.getAllOrderProducts());
            orderRepository.save(order);
            return;
        }

        cancelManager.setCancelProductState(state);

        CancelData cancelData = null;
//        log.info("impUid = {}", order.getImpUid());
//        log.info("totalCancelPrice = {}", cancelPrice);
//        log.info("taxFreePrice = {}", cancelManager.getNonTaxablePriceTobeCanceled());
        setVbankRefundInfo(order, cancelData);


        cancelData = new CancelData(
                order.getImpUid(),
                true,
                BigDecimal.valueOf(cancelManager.getCancelPrice())
        );
        cancelData.setTax_free(BigDecimal.valueOf(
                cancelManager.getNonTaxablePriceTobeCanceled()));

        if (cancelManager.allCanceled()) {
            Payments payment = paymentRepository.findFirstByImpUid(order.getImpUid());
            payment.setStatus(PaymentState.CANCELED);
            order.setState(OrderState.CANCELED);

            couponCommandService.unUseCoupon(order.getCouponId(), order.getUserId());
            UserInfo userInfo = userInfoQueryService.findByUserId(order.getUserId());
            userInfo.addPoint(order.getUsedPoint());

            paymentRepository.save(payment);
            userInfoRepository.save(userInfo);
        }
        if (!cancelManager.allCanceled()) {
            // 부분취소일 때만 주문에 가격 설정. 전체 취소할 때 가격 설정하면 전체취소 주문 가격이 0원이 됨
            order.setTotalPrice(cancelManager.getOrderPriceAfterCancellation());
            order.setOriginTotalPrice(cancelManager.getProductAndDeliveryFee());
        }
        setCancelReason(request, cancelManager.getTobeCanceled());

        List<OptionItem> optionItems = addQuantity(cancelManager);
        optionItemRepository.saveAll(optionItems);
        orderProductInfoRepository.saveAll(cancelManager.getAllOrderProducts());
        orderRepository.save(order);

        pgService.cancelPayment(cancelData);
    }

    @NotNull
    private List<OptionItem> addQuantity(CancelManager cancelManager) {
        List<OrderProductInfo> tobeCanceled = cancelManager.getTobeCanceled();
        List<Integer> optionItemIds = tobeCanceled.stream()
                .map(v -> v.getOptionItemId())
                .toList();
        List<OptionItem> optionItems = optionItemRepository.findAllById(optionItemIds);

        for (OrderProductInfo orderProductInfo : tobeCanceled) {
            for (OptionItem optionItem : optionItems) {
                if (optionItem.getId() == orderProductInfo.getOptionItemId()) {
                    optionItem.addQuantity(orderProductInfo.getAmount());
                    break;
                }
            }
        }
        return optionItems;
    }

    private static void setVbankRefundInfo(Orders order, CancelData cancelData) {
        if (order.getVbankRefundInfo() != null) {
            VBankRefundInfo refundInfo = order.getVbankRefundInfo();
            cancelData.setRefund_holder(refundInfo.getBankHolder());
            cancelData.setRefund_bank(refundInfo.getBankCode());
            cancelData.setRefund_account(refundInfo.getBankAccount());
        }
    }

    private void setCancelReason(RequestCancelReq request, List<OrderProductInfo> tobeCanceled) {
        for (OrderProductInfo cancelProduct : tobeCanceled) {
            cancelProduct.setCancelReason(request.getCancelReason());
            cancelProduct.setCancelReasonContent(request.getContent());
        }
    }

    @Nullable
    private String validateRequest(Integer userId, RequestCancelReq data, Orders order) {
        if (userId != order.getUserId()) {
            throw new BusinessException("타인의 주문 내역입니다.");
        }
        if (data.getCancelReason() == null) {
            throw new BusinessException("취소/환불 사유를 선택해주세요.");
        }
        String content = null;
        if (data.getContent() != null) {
            content = utils.validateString(data.getContent(), 1000L, "사유");
        }
        return content;
    }
}
