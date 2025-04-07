package com.matsinger.barofishserver.domain.product.filter.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "category_filter_products")
public class CategoryFilterProducts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "sub_category_id")
    private Integer subCategoryId;

    @Column(name = "filter_id", nullable = false)
    private Integer filterId;

    @Column(name = "field_ids", nullable = false)
    private String fieldIds;

    @Column(name = "product_ids", nullable = false)
    private String productIds;

    public static CategoryFilterProducts from(Integer pCategoryId, Integer cCategoryId, int filterId, String fieldsString, String productsString) {
        return CategoryFilterProducts.builder()
                .categoryId(pCategoryId)
                .subCategoryId(cCategoryId)
                .filterId(filterId)
                .fieldIds(fieldsString)
                .productIds(productsString)
                .build();
    }
}