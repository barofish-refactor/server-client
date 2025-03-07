package com.matsinger.barofishserver.domain.product.optionitem.dto;

import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import lombok.*;

@Getter
@Setter
@Builder @AllArgsConstructor @NoArgsConstructor
public class OptionItemDto {
    private Integer id;
    private Integer optionId;
    private String name;
    private Integer discountPrice;
    private Integer amount;
    private Integer inventoryQuantity;
    private Integer purchasePrice;
    private Integer originPrice;
    private Integer deliveryFee;
    private Integer deliverBoxPerAmount;
    private Integer maxAvailableAmount;
    private Float pointRate;
    private Integer minOrderPrice;

    public static OptionItemDto from(OptionItem optionItem, Product product) {
        return OptionItemDto.builder()
                .id(optionItem.getId())
                .optionId(optionItem.getOptionId())
                .name(optionItem.getName())
                .discountPrice(optionItem.getDiscountPrice())
                .amount(optionItem.getAmount())
                .inventoryQuantity(optionItem.getAmount())
                .purchasePrice(optionItem.getPurchasePrice())
                .originPrice(optionItem.getOriginPrice())
                .deliveryFee(optionItem.getDeliverFee())
                .deliverBoxPerAmount(product.getDeliverBoxPerAmount())
                .maxAvailableAmount(optionItem.getMaxAvailableAmount())
                .minOrderPrice(product.getMinOrderPrice())
                .build();
    }
}
