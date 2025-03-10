package com.matsinger.barofishserver.order.application.dto;

import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.option.domain.Option;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.order.domain.model.OrderCancelReason;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.domain.product.domain.ProductDeliverFeeType;
import com.matsinger.barofishserver.domain.product.optionitem.dto.OptionItemDto;
import com.matsinger.barofishserver.domain.product.dto.ProductListDto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.sql.Timestamp;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderProductDto {
    private Integer id;
    private Integer storeId;
    private String storeName;
    private String storeProfile;
    private Integer deliverFee;
    private ProductDeliverFeeType deliverFeeType;
    private Integer minOrderPrice;
    private Integer minStorePrice;
    private ProductListDto product;
    private OrderProductState state;
    private String optionName;
    private boolean isNeeded;
    private OptionItemDto optionItem;
    private Integer originPrice;
    private Integer price;
    private Integer amount;
    private String deliverCompany;
    private String invoiceCode;
    private String deliverCompanyCode;
    private Timestamp finalConfirmedAt;
    private Boolean needTaxation;
    // private Integer deliveryFee;
    private OrderCancelReason cancelReason;
    private String cancelReasonContent;
    private Boolean isReviewWritten;

    public static OrderProductDto from(
            OrderProductInfo orderProductInfo,
            Product product,
            StoreInfo storeInfo,
            OptionItem optionItem,
            Option option,
            boolean isReviewWritten
    ) {
        return OrderProductDto.builder()
                .id(orderProductInfo.getId())
                .storeId(storeInfo.getStoreId())
                .storeName(storeInfo.getName())
                .storeProfile(storeInfo.getProfileImage())
                .optionItem(OptionItemDto.from(optionItem, product))
                .optionName(optionItem.getName())
                .isNeeded(option.getIsNeeded())
                .amount(orderProductInfo.getAmount())
                .state(orderProductInfo.getState())
                .price(orderProductInfo.getPrice())
                .originPrice(orderProductInfo.getOriginPrice())
                .deliverFee(orderProductInfo.getDeliveryFee())
                .deliverFeeType(product.getDeliverFeeType())
                .minOrderPrice(product.getMinOrderPrice())
                .minStorePrice(storeInfo.getMinStorePrice())
                .isReviewWritten(isReviewWritten)
                .needTaxation(product.getNeedTaxation())
                .build();
    }
}
