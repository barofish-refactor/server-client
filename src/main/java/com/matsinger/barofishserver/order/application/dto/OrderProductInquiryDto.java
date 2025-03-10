package com.matsinger.barofishserver.order.application.dto;

import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.domain.product.domain.ProductState;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.Getter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
public class OrderProductInquiryDto {

    private Integer productId;
    private OrderProductState state;
    private Timestamp promotionStartAt;
    private Timestamp promotionEndAt;

    private Integer maxAvailableAmount;

    public boolean isPromotionEnd() {
        if (promotionStartAt.after(Timestamp.valueOf(LocalDateTime.now())) ||
                promotionEndAt.before(Timestamp.valueOf(LocalDateTime.now()))) {
            return true;
        }
        return false;
    }

    public void validateState() {
        if (!state.equals(ProductState.ACTIVE)) {
            if (state.equals(ProductState.SOLD_OUT))
                throw new BusinessException("품절된 상품입니다.");
            if (state.equals(ProductState.DELETED))
                throw new BusinessException("주문 불가능한 상품입니다.");
            if (state.equals(ProductState.INACTIVE))
                throw new BusinessException("주문 불가능한 상품입니다.");
        }
    }

    public void validateQuantity(int quantity) {
        if (maxAvailableAmount < quantity) {
            throw new BusinessException("최대 주문 수량을 초과하였습니다.");
        }
    }
}
