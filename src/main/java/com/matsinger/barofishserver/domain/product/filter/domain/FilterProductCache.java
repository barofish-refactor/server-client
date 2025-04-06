package com.matsinger.barofishserver.domain.product.filter.domain;

import com.matsinger.barofishserver.domain.filter.domain.Filter;
import com.matsinger.barofishserver.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "filter_product_cache")
public class FilterProductCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "filter_id", nullable = false)
    private Integer filterId;

    @Column(name = "field_ids", nullable = false)
    private String fieldIds;

    @Column(name = "product_ids", nullable = false)
    private String productIds;

    public static FilterProductCache from(int filterId, String fieldsString, String productsString) {
        return FilterProductCache.builder()
                .filterId(filterId)
                .fieldIds(fieldsString)
                .productIds(productsString)
                .build();
    }
}