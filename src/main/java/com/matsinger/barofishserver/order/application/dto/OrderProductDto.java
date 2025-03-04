package com.matsinger.barofishserver.order.application.dto;

import com.matsinger.barofishserver.order.domain.model.OrderCancelReason;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.domain.product.domain.ProductDeliverFeeType;
import com.matsinger.barofishserver.domain.product.optionitem.dto.OptionItemDto;
import com.matsinger.barofishserver.domain.product.dto.ProductListDto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Builder
public class OrderProductDto {
    private Integer id;
    private Integer storeId;
    private String storeProfile;
    private String storeName;
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
}
