package com.matsinger.barofishserver.compare.dto;

import com.matsinger.barofishserver.product.dto.ProductListDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendCompareProduct {
    ProductListDto mainProduct;
    List<ProductListDto> recommendProducts;
}
