package com.matsinger.barofishserver.domain.settlement.dto;

import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.order.domain.model.OrderPaymentWay;
import com.matsinger.barofishserver.domain.settlement.domain.SettlementState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSettlementExcelDto {

    private int productId;                          // 상품번호
    private String orderId;                         // 주문번호
    private OrderProductState orderProductState;    // 주문상태
    private Timestamp orderAt;                      // 주문일
    private Timestamp finalConfirmDate;               // 구매 확정일
    private String storeName;                       // 스토어 이름
    private String productName;                     // 상품명
    private String optionName;                      // 옵션명
    private boolean needTaxation;                   // 과세 여부
    private int purchasePrice;                      // 매입가 -> 공급가(원)으로 변경
    private int commission;                         // 수수료가 (바로피쉬 판매수수료 수익)
    private int sellingPrice;                       // 판매가
    private int deliveryFee;                        // 배송비
    private int quantity;                           // 수량
    private int orderAmount;                        // 총 금액
    private String couponName;                      // 쿠폰명
    private int couponDiscount;                     // 쿠폰할인
    private int usePoint;                           // 포인트
    private int totalOrderAmount;                   // 최종결제금액 - 총금액 - 쿠폰할인 - 포인트
    private OrderPaymentWay paymentMethod;          // 결제수단
    private double settlementRatio;                 // 정산비율
    private double settlementAmount;                // 정산금액 = 판매가 * 수량 / (1 - 정산비율) + 배송비
    private SettlementState settlementState;        // 정산상태
    private Timestamp settledAt;                    // 정산일시
    private String customerName;                    // 주문인
    private String customerPhoneNumber;             // 연락처
    private String customerEmail;                   // 이메일
    private String customerAddress;                 // 주소
    private String deliveryMessage;                 // 배송메세지
    private String deliveryCompany;                 // 택배사
    private String trackingNumber;                  // 운송장번호
}
