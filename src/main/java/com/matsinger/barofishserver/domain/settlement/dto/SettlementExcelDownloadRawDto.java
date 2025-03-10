package com.matsinger.barofishserver.domain.settlement.dto;

import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Getter
@Builder @AllArgsConstructor @NoArgsConstructor
public class SettlementExcelDownloadRawDto {

    private int productId;                                  // 상품번호
    private String orderId;                                 // 주문번호
    private OrderProductState orderProductInfoState;        // 주문상태
    private Timestamp orderedAt;                            // 주문일
    private Timestamp finalConfirmedAt;                       // 구매 확정일
    private String partnerName;                             // 파트너
    private String productName;                             // 상품명
    private String optionItemName;                          // 옵션명
    private boolean isTaxFree;                              // 과세여부
    private int purchasePrice;                              // 공급가(원)
    private int commissionPrice;                            // 수수료가(원) | 공급가 - 판매가
    private int sellingPrice;                               // 판매가 | 공급가 / (1 - 정산비율)
    private int deliveryFee;                                // 배송비                                  개별 | 파트너끝
    private int quantity;                                   // 수량
    private int totalPrice;                                 // 총 금액(원) (판매가 * 수량) - 배송비         개별 | 파트너끝
    private int totalOrderPrice;                            // 총 주문금액
    private String couponName;                              // 쿠폰명                                               | 주문끝
    private int couponDiscount;                             // 쿠폰할인                                              | 주문끝
    private int usePoint;                                   // 포인트                                               | 주문끝
    private int finalPaymentPrice;                           // 최종결제금액(원) 총 금액 - 쿠폰할인 - 포인트    개별 | 파트너끝 | 주문끝
    private String PaymentWay;                     // 결제수단
    private Float settlementRate;                           // 정산비율
    private int settlementPrice;                            // 정산금액(원)
    private boolean settlementState;                        // 정산상태
    private Timestamp settledAt;                            // 정산일시
    private String customerName;                            // 주문인
    private String phoneNumber;                             // 연락처
    private String email;                                   // 이메일
    private String address;                                 // 주소
    private String deliverMessage;                          // 배송메세지
    private String deliveryCompany;                         // 택배사
    private String invoiceCode;                             // 운송장번호

    @Override
    public String toString() {
        return "SettlementExcelDownloadRawDto{" +
                "productId=" + productId +
                ", orderId='" + orderId + '\'' +
                ", orderProductInfoState='" + orderProductInfoState + '\'' +
                ", orderedAt=" + orderedAt +
                ", finalConfirmedAt=" + finalConfirmedAt +
                ", partnerName='" + partnerName + '\'' +
                ", productName='" + productName + '\'' +
                ", optionItemName='" + optionItemName + '\'' +
                ", isTaxFree=" + isTaxFree +
                ", purchasePrice=" + purchasePrice +
                ", commissionPrice=" + commissionPrice +
                ", sellingPrice=" + sellingPrice +
                ", deliveryFee=" + deliveryFee +
                ", quantity=" + quantity +
                ", totalPrice=" + totalPrice +
                ", totalOrderPrice=" + totalOrderPrice +
                ", couponName='" + couponName + '\'' +
                ", couponDiscount=" + couponDiscount +
                ", usePoint=" + usePoint +
                ", finalPaymentPrice=" + finalPaymentPrice +
                ", PaymentWay='" + PaymentWay + '\'' +
                ", settlementRate=" + settlementRate +
                ", settlementPrice=" + settlementPrice +
                ", settlementState='" + settlementState + '\'' +
                ", settledAt=" + settledAt +
                ", customerName='" + customerName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", deliverMessage='" + deliverMessage + '\'' +
                ", deliveryCompany='" + deliveryCompany + '\'' +
                ", invoiceCode='" + invoiceCode + '\'' +
                '}';
    }
}
