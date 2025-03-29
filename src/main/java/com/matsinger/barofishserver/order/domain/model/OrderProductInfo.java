package com.matsinger.barofishserver.order.domain.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import com.matsinger.barofishserver.domain.product.domain.Product;

import com.matsinger.barofishserver.domain.product.domain.ProductDeliverFeeType;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "order_product_info", schema = "barofish_dev", catalog = "")
public class OrderProductInfo {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private int id;
    @Basic
    @Column(name = "order_id", nullable = false, length = 20)
    private String orderId;
    @Basic
    @Column(name = "product_id", nullable = false)
    private int productId;
    @Column(name = "store_id")
    private Integer storeId;
    @Basic
    @Column(name = "option_item_id", nullable = false)
    private int optionItemId;
    @Basic
    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderProductState state;
    @Basic
    @Column(name = "settle_price")
    private Integer settlePrice;                            // 정산가격 = 공급가(매입가)
    @Basic
    @Column(name = "origin_price")
    private Integer originPrice;                            // 상품가격
    @Basic
    @Column(name = "price", nullable = false)
    private int price;                                      // 상품가격 * 수량
    @Basic
    @Column(name = "amount", nullable = false)
    private int amount;
    @Basic
    @Column(name = "delivery_fee", nullable = false)
    private int deliveryFee;
    @Basic @Column(name = "delivery_fee_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductDeliverFeeType deliveryFeeType;

    @Basic
    @Column(name = "cancel_reason", nullable = true)
    @Enumerated(EnumType.STRING)
    private OrderCancelReason cancelReason;

    @Basic
    @Column(name = "cancel_reason_content", nullable = true)
    private String cancelReasonContent;

    @Basic
    @Column(name = "deliver_company_code", nullable = true)
    private String deliverCompanyCode;

    @Basic
    @Column(name = "invoice_code", nullable = true)
    private String invoiceCode;

    @Basic
    @Column(name = "isSettled", nullable = false)
    private Boolean isSettled;

    @Basic
    @Column(name = "settledAt", nullable = true)
    private Timestamp settledAt;
    @Basic
    @Column(name = "delivery_done_at")
    private Timestamp deliveryDoneAt;
    @Basic
    @Column(name = "final_confirmed_at", nullable = true)
    private Timestamp finalConfirmedAt;
    @Basic
    @Column(name = "tax_free_amount", nullable = true)
    private Integer taxFreeAmount;
    @Basic
    @Column(name = "is_tax_free", nullable = false)
    private Boolean isTaxFree;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonBackReference
    @JoinColumn(name = "order_id", updatable = false, insertable = false)
    Orders order;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonBackReference
    @JoinColumn(name = "product_id", updatable = false, insertable = false)
    Product product;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public OrderProductState getState() {
        return state;
    }

    public void setState(OrderProductState state) {
        this.state = state;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(int deliveryFee) {
        this.deliveryFee = deliveryFee;
    }
    public boolean isIfOver() {
        return deliveryFeeType.equals(ProductDeliverFeeType.FIX);
    }
    public boolean isFree() {
        return deliveryFeeType.equals(ProductDeliverFeeType.FIX);
    }

    public Integer getTotalProductPrice() {
        return originPrice * amount;
    }

    public Integer getTotalPriceMinusDeliveryFee() {
        return getTotalProductPrice() - deliveryFee;
    }

    public int getTotalPriceContainsDeliveryFee() {
        return getTotalProductPrice() + deliveryFee;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderProductInfo that = (OrderProductInfo) o;
        return id == that.id &&
                productId == that.productId &&
                price == that.price &&
                amount == that.amount &&
                deliveryFee == that.deliveryFee &&
                Objects.equals(orderId, that.orderId) &&
                Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderId, productId, state, price, amount, deliveryFee);
    }

    public boolean equalToProductId(int productId) {
        return this.productId == productId;
    }

    public boolean equalToOptionItemId(int optionItemId) {
        return this.optionItemId == optionItemId;
    }

    public boolean isCancelableState() {
        return this.state == OrderProductState.WAIT_DEPOSIT ||
                this.state == OrderProductState.PAYMENT_DONE;
    }

    public StoreInfo getStoreInfo() {
        return product.getStoreInfo();
    }
}
