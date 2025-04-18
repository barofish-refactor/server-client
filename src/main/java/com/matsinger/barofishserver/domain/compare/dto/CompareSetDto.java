package com.matsinger.barofishserver.domain.compare.dto;

import com.matsinger.barofishserver.domain.product.dto.ProductListDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompareSetDto {
    Integer compareSetId;
    List<ProductListDto> products;
}
