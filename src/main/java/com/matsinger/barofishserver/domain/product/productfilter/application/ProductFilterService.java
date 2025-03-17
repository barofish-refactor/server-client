package com.matsinger.barofishserver.domain.product.productfilter.application;

import com.matsinger.barofishserver.domain.compare.filter.application.CompareFilterQueryService;
import com.matsinger.barofishserver.domain.compare.filter.domain.CompareFilter;
import com.matsinger.barofishserver.domain.product.productfilter.domain.ProductFilterValue;
import com.matsinger.barofishserver.domain.product.productfilter.domain.ProductFilterValueId;
import com.matsinger.barofishserver.domain.product.productfilter.repository.ProductFilterRepository;
import com.matsinger.barofishserver.domain.product.productfilter.dto.ProductFilterValueDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductFilterService {
    private final ProductFilterRepository productFilterRepository;
    private final CompareFilterQueryService compareFilterQueryService;

    public void addAllProductFilter(List<ProductFilterValue> values) {
        productFilterRepository.saveAll(values);
    }

    public Optional<ProductFilterValue> selectProductFilterValue(ProductFilterValueId valueId) {
        return productFilterRepository.findById(valueId);
    }

    @Transactional
    public void deleteAllFilterValueWithProductId(Integer productId) {
        productFilterRepository.deleteAllByProductId(productId);
    }

    public List<ProductFilterValueDto> selectProductFilterValueListWithProductId(Integer productId) {
        return productFilterRepository.findAllByProductId(productId).stream().map(this::convert2Dto).toList();
    }

    public Map<Integer, List<ProductFilterValueDto>> selectProductFilterValuesByProductIds(List<Integer> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        List<ProductFilterValue> allFilterValues = productFilterRepository.findAllByProductIdIn(productIds);

        Map<Integer, List<ProductFilterValue>> filterValuesByProduct = allFilterValues.stream()
                .collect(Collectors.groupingBy(ProductFilterValue::getProductId));

        Map<Integer, List<ProductFilterValueDto>> result = new HashMap<>();
        for (Integer productId : productIds) {
            List<ProductFilterValue> filterValues = filterValuesByProduct.getOrDefault(productId, Collections.emptyList());
            List<ProductFilterValueDto> filterValueDtos = filterValues.stream()
                    .map(this::convert2Dto)
                    .collect(Collectors.toList());
            result.put(productId, filterValueDtos);
        }

        return result;
    }

    public ProductFilterValueDto convert2Dto(ProductFilterValue value) {

        CompareFilter filter = compareFilterQueryService.selectCompareFilter(value.getCompareFilterId());
        return ProductFilterValueDto.builder().compareFilterId(filter.getId()).compareFilterName(filter.getName())
                .value(
                        value.getValue())
                .build();
    }
}
