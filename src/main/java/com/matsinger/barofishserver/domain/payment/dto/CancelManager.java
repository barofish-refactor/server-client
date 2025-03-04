package com.matsinger.barofishserver.domain.payment.dto;

import com.matsinger.barofishserver.order.domain.model.Orders;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class CancelManager {

    private List<OrderProductInfo> tobeCanceled;
    private List<OrderProductInfo> notTobeCanceled;
    private Orders order;

    private int notTobeCanceledProductPrice;
    private int notTobeCanceledDeliveryFee;

    private int nonTaxablePriceTobeCanceled;
    private int taxablePriceTobeCanceled;

    private boolean allCanceled = false;

    public CancelManager(Orders order, List<OrderProductInfo> tobeCanceled, List<OrderProductInfo> notTobeCanceled) {
        this.tobeCanceled = tobeCanceled;
        this.notTobeCanceled = notTobeCanceled;
        this.order = order;

        notTobeCanceledProductPrice = notTobeCanceled.stream()
                .mapToInt(v -> v.getTotalProductPrice()).sum();
        notTobeCanceledDeliveryFee = notTobeCanceled.stream()
                .mapToInt(v -> v.getDeliveryFee()).sum();

        if (notTobeCanceled.isEmpty()) {
            allCanceled = tobeCanceled.stream()
                    .noneMatch(v -> v.getState().equals(OrderProductState.CANCELED));
        }

        // 쿠폰, 포인트는 포트원에 보내는 주문 가격에 포함돼 있지 않기 때문에 비과세 가격에서 쿠폰, 포인트 가격을 빼줌
        int nonTaxablePrice = tobeCanceled.stream()
                .mapToInt(v -> v.getTaxFreeAmount()).sum();
        int taxablePrice = tobeCanceled.stream().mapToInt(
                        v -> v.getTotalProductPrice() + v.getDeliveryFee())
                .sum() - nonTaxablePrice;
        if (allCanceled) {
            int discountPrice = order.getUsePoint() + order.getCouponDiscount();
            if (taxablePrice < discountPrice) {
                int remainingDiscount = discountPrice - taxablePrice;
                taxablePriceTobeCanceled = 0;
                nonTaxablePriceTobeCanceled = nonTaxablePrice - remainingDiscount;
                return;
            }

            nonTaxablePriceTobeCanceled = nonTaxablePrice;
            taxablePriceTobeCanceled = taxablePrice - discountPrice;

        }
        if (!allCanceled) {
            nonTaxablePriceTobeCanceled = nonTaxablePrice;
            taxablePriceTobeCanceled = taxablePrice;
        }
        log.warn("cancelManager.nonTaxablePriceTobeCanceled = {}", nonTaxablePriceTobeCanceled);
        log.warn("cancelManager.taxablePriceTobeCanceled = {}", taxablePriceTobeCanceled);
        log.warn("isAllCanceled = {}", allCanceled);
    }

    public int getProductAndDeliveryFee() {
        return notTobeCanceledProductPrice + notTobeCanceledDeliveryFee;
    }

    public int getOrderPriceAfterCancellation() {
        return notTobeCanceledProductPrice +
                notTobeCanceledDeliveryFee -
                order.getCouponDiscount() -
                order.getUsedPoint();
    }

    public boolean allCanceled() {
        return allCanceled;
    }

    public List<OrderProductInfo> getAllOrderProducts() {
        List<OrderProductInfo> allOrderProducts = new ArrayList<>();
        allOrderProducts.addAll(tobeCanceled);
        allOrderProducts.addAll(notTobeCanceled);
        return allOrderProducts;
    }

    public int getCancelPrice() {
        return nonTaxablePriceTobeCanceled + taxablePriceTobeCanceled;
    }

    public void setCancelProductState(OrderProductState state) {
        validateCancelProductState();
        tobeCanceled.stream()
                .forEach(v -> v.setState(state));
    }

    public void validateCancelProductState() {
        for (OrderProductInfo cancelProduct : tobeCanceled) {
            OrderProductState state = cancelProduct.getState();
            if (state.equals(OrderProductState.CANCELED)) {
                throw new BusinessException("취소 완료된 상태입니다.");
            }
            if (state.equals(OrderProductState.DELIVERY_DONE) ||
                    state.equals(OrderProductState.EXCHANGE_REQUEST) ||
                    state.equals(OrderProductState.EXCHANGE_ACCEPT) ||
                    state.equals(OrderProductState.FINAL_CONFIRM) ||
                    state.equals(OrderProductState.REFUND_REQUEST) ||
                    state.equals(OrderProductState.REFUND_ACCEPT) ||
                    state.equals(OrderProductState.REFUND_DONE)) {
                throw new BusinessException("취소 불가능한 상태입니다.");
            }
            if (state.equals(OrderProductState.CANCEL_REQUEST)) {
                throw new BusinessException("이미 취소 요청된 상태입니다.");
            }
            if (state.equals(OrderProductState.DELIVERY_READY)) {
                throw new BusinessException("상품이 출고되어 취소가 불가능합니다.");
            }
        }
    }

    public Integer getKakaopayTaxFreePrice() {
        // 부가세액 = 과세금액 * 0.1 소수 첫째자리 올림
        Integer surTax = (int) Math.ceil(taxablePriceTobeCanceled * 0.1);
        // 과세금액 + 부가세액 = 주문금액 - 면세금액

        // 면세금액 = 주문금액 - 과세금액 - 부가세액
        // 과세금액 = 주문금액 - 면세금액 - 부가세액
        // 면세금액 = 주문금액 - 과세금액 - 부가세액
        Integer taxFreePrice = taxablePriceTobeCanceled + nonTaxablePriceTobeCanceled - taxablePriceTobeCanceled - surTax;
        log.warn("kakaoPay taxFreePrice = {}", taxFreePrice);
        return taxFreePrice;
    }
}
