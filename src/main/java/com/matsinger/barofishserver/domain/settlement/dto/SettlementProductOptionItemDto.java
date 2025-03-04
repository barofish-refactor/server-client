package com.matsinger.barofishserver.domain.settlement.dto;

import com.matsinger.barofishserver.order.domain.model.OrderPaymentWay;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * DB에서 조회 후 따로 계산해서 set해줘야함
 * commissionPrice
 * totalPrice
 * settlementPrice
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SettlementProductOptionItemDto {
    private int productId;                                  // 상품 아이디
    private String productName;                             // 상품명
    private String optionItemName;                          // 옵션명
    private OrderProductState orderProductInfoState;        // 주문상태
    private Timestamp orderedAt;                            // 주문일
    private Timestamp finalConfirmedAt;                       // 구매 확정일
    private boolean isTaxFree;                              // 과세여부
    private int purchasePrice;                              // 공급가(원)
    private int commissionPrice;                            // 수수료가(원) | 공급가 - 판매가
    private int sellingPrice;                               // 판매가 | 공급가 / (1 - 정산비율)
    private int deliveryFee;                                // 배송비                                  개별 | 파트너끝
    private int quantity;                                   // 수량
    private int totalPrice;                                 // 총 금액(원) (판매가 * 수량) - 배송비         개별 | 파트너끝
//    private int finalPaymentPrice;                           // 최종결제금액(원) 총 금액 - 쿠폰할인 - 포인트    개별 | 파트너끝 | 주문끝
    private OrderPaymentWay paymentWay;                     // 결제수단
    private int settlementPrice;                            // 정산금액(원)
    private boolean settlementState;                        // 정산상태
    private Timestamp settledAt;                            // 정산일시
    private String customerName;                            // 주문인
    private String phoneNumber;                             // 연락처
    private String email;                                   // 이메일
    private String address;                                 // 주소
    private String addressDetail;                           // 주소 상세
    private String deliverMessage;                          // 배송메세지
    private String deliveryCompany;                         // 택배사
    private String invoiceCode;                             // 운송장번호

    public void setCommissionPrice(int commissionPrice) {
        this.commissionPrice = commissionPrice;
    }
    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }
    public void setSettlementPrice(int settlementPrice) {
        this.settlementPrice = settlementPrice;
    }

    @Override
    public String toString() {
        return "SettlementProductOptionItemDto{" +
                "productName='" + productName + '\'' +
                ", optionItemName='" + optionItemName + '\'' +
                ", orderProductInfoState=" + orderProductInfoState +
                ", orderedAt=" + orderedAt +
                ", finalConfirmedAt=" + finalConfirmedAt +
                ", isTaxFree=" + isTaxFree +
                ", purchasePrice=" + purchasePrice +
                ", commissionPrice=" + commissionPrice +
                ", sellingPrice=" + sellingPrice +
                ", deliveryFee=" + deliveryFee +
                ", quantity=" + quantity +
                ", totalPrice=" + totalPrice +
                ", paymentWay=" + paymentWay +
                ", settlementPrice=" + settlementPrice +
                ", settlementState=" + settlementState +
                ", settledAt=" + settledAt +
                ", customerName='" + customerName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", addressDetail='" + addressDetail + '\'' +
                ", deliverMessage='" + deliverMessage + '\'' +
                ", deliveryCompany='" + deliveryCompany + '\'' +
                ", invoiceCode='" + invoiceCode + '\'' +
                '}';
    }
}
