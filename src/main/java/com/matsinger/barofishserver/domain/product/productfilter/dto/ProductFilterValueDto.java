package com.matsinger.barofishserver.domain.product.productfilter.dto;

import com.matsinger.barofishserver.domain.product.productfilter.domain.ProductFilterValue;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterValueDto {
    Integer compareFilterId;
    String compareFilterName;
    String value;

    public static ProductFilterValueDto from(ProductFilterValue filterValue) {
        return ProductFilterValueDto.builder()
                .compareFilterId(filterValue.getCompareFilterId())
                .compareFilterName(filterValue.getCompareFilter().getName())
                .value(filterValue.getValue())
                .build();
    }

    public static List<ProductFilterValueDto> listFrom(List<ProductFilterValue> filterValues) {
        return filterValues.stream()
                .map(filterValue -> from(filterValue))
                .collect(Collectors.toList());
    }
}
