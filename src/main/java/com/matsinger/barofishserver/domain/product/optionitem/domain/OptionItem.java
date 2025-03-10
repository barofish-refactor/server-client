package com.matsinger.barofishserver.domain.product.optionitem.domain;

import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.optionitem.dto.OptionItemDto;
import com.matsinger.barofishserver.domain.product.domain.OptionItemState;
import com.matsinger.barofishserver.global.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Entity
@Table(name = "option_item", schema = "barofish_dev", catalog = "")
public class OptionItem {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @Basic
    @Column(name = "option_id", nullable = false)
    private int optionId;
    @Basic
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Basic
    @Column(name = "state", nullable = false)
    @Enumerated(EnumType.STRING)
    private OptionItemState state;

    @Basic
    @Column(name = "discount_price", nullable = false)
    private int discountPrice;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Basic
    @Column(name = "purchase_price", nullable = false)
    private Integer purchasePrice;

    @Basic
    @Column(name = "origin_price", nullable = false)
    private Integer originPrice;

    @Basic
    @Column(name = "delivery_fee", nullable = false)
    private Integer deliverFee;

    @Basic
    @Column(name = "deliver_box_per_amount", nullable = true)
    private Integer deliverBoxPerAmount;

    @Basic
    @Column(name = "max_available_amount", nullable = true)
    private Integer maxAvailableAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    public void validateQuantity(int quantity, String productName) {
        if (quantity < 1) {
            throw new BusinessException("수량은 0보다 커야 합니다.");
        }
        if (this.amount != null) {
            int reducedValue = this.amount - quantity;
            if (reducedValue < 0) {
                String errorMessage = String.format("[%s] %s 상품의 재고가 부족합니다." + "\n" +
                                                    "상품 재고 = %s" + "\n" +
                                                    "요청 수량 = %s",
                        productName, this.name, this.amount, quantity);
                throw new BusinessException(errorMessage);
            }
        }
    }

    public void reduceQuantity(int quantity) {
        int reducedValue = this.amount - quantity;
        if (reducedValue < 0) {
            String errorMessage = String.format("[%s] %s 상품의 재고가 부족합니다." + "\n" +
                                                "상품 재고 = %s" + "\n" +
                                                "요청 수량 = %s",
                                                this.name, this.amount, quantity);
            throw new BusinessException(errorMessage);
        }
        this.amount = reducedValue;
    }

    public OptionItemDto convert2Dto(Product product) {
        return OptionItemDto.builder()
                .id(this.id)
                .optionId(this.getOptionId())
                .name(this.name)
                .discountPrice(this.discountPrice)
                .amount(this.amount)
                .purchasePrice(this.purchasePrice)
                .originPrice(this.originPrice)
                .deliveryFee(this.deliverFee)
                .deliverBoxPerAmount(this.deliverBoxPerAmount)
                .maxAvailableAmount(this.maxAvailableAmount)
                .minOrderPrice(product.getMinOrderPrice())
                .build();
    }

    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptionItem that = (OptionItem) o;
        return id == that.id && optionId == that.getOptionId() && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, getOptionId(), name);
    }

    public void addQuantity(int quantity) {
        if (quantity < 0) {
            throw new BusinessException("주문 상품 개수는 0보다 커야합니다.");
        }
        this.amount += quantity;
    }
}
