package com.matsinger.barofishserver.order.domain.model;

import com.matsinger.barofishserver.order.application.dto.OrderDto;
import com.matsinger.barofishserver.order.application.dto.VBankRefundInfo;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders", schema = "barofish_dev", catalog = "")
public class Orders {
    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;
    @Basic
    @Column(name = "user_id", nullable = false)
    private int userId;
    @Basic
    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderState state;
    @Basic
    @Column(name = "payment_way", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderPaymentWay paymentWay;

    @Basic
    @Column(name = "orderer_name")
    private String ordererName;

    @Basic
    @Column(name = "orderer_tel")
    private String ordererTel;
    @Basic
    @Column(name = "origin_total_price")
    private Integer originTotalPrice;
    @Basic
    @Column(name = "total_price", nullable = false)
    private int totalPrice;
    @Basic
    @Column(name = "ordered_at", nullable = false)
    private Timestamp orderedAt;

    @Basic
    @Column(name = "imp_uid", nullable = true)
    private String impUid;

    @Basic
    @Column(name = "coupon_id", nullable = true)
    private Integer couponId;

    @Basic
    @Column(name = "coupon_discount", nullable = true)
    private Integer couponDiscount;

    @Basic
    @Column(name = "use_point", nullable = true)
    private Integer usePoint;
    @Basic
    @Column(name = "bank_holder", nullable = true)
    private String bankHolder;
    @Basic
    @Column(name = "bank_code", nullable = true)
    private String bankCode;
    @Basic
    @Column(name = "bank_account", nullable = true)
    private String bankAccount;
    @Basic
    @Column(name = "bank_name", nullable = true)
    private String bankName;

    @Builder.Default
    @OneToMany(mappedBy = "order")
    List<OrderProductInfo> productInfos = new ArrayList<>();
    @OneToOne(mappedBy = "order")
    OrderDeliverPlace deliverPlace;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public OrderState getState() {
        return state;
    }

    public void setState(OrderState state) {
        this.state = state;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Timestamp getOrderedAt() {
        return orderedAt;
    }

    public void setOrderedAt(Timestamp orderedAt) {
        this.orderedAt = orderedAt;
    }

    public void setVbankRefundInfo(String bankCode, String bankHolder,
                                   String bankName, String bankAccount) {
        this.bankCode = bankCode;
        this.bankHolder = bankHolder;
        this.bankName = bankName;
        this.bankAccount = bankAccount;
    }

    public VBankRefundInfo getVbankRefundInfo() {
        if (this.bankCode == null) {
            return null;
        }
        return VBankRefundInfo.builder()
                .bankCode(this.bankCode)
                .bankHolder(this.bankHolder)
                .bankName(this.bankName)
                .bankAccount(this.bankAccount)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Orders orders = (Orders) o;
        return userId == orders.userId &&
                totalPrice == orders.totalPrice &&
                Objects.equals(id, orders.id) &&
                Objects.equals(state, orders.state) &&
                Objects.equals(orderedAt, orders.orderedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, state, totalPrice, orderedAt);
    }

    public OrderDto toDto() {
        return null;
    }

    public boolean isCouponUsed() {
        return couponId != null;
    }

    public boolean isPointUsed() {
        return this.usePoint != null;
    }

    public Integer getUsedPoint() {
        if (this.usePoint == null) {
            return 0;
        }
        return this.usePoint;
    }
    public Integer getUsedCouponId() {
        return this.couponId;
    }

    public boolean hasProductFromStore(Integer storeId) {
        return this.getProductInfos().stream()
                .anyMatch(product -> product.getStoreId().equals(storeId));
    }
}
