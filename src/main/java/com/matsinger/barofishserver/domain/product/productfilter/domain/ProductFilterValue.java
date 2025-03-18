package com.matsinger.barofishserver.domain.product.productfilter.domain;

import com.matsinger.barofishserver.domain.compare.filter.domain.CompareFilter;
import com.matsinger.barofishserver.domain.product.domain.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ProductFilterValueId.class)
@Table(name = "product_filter_value", schema = "barofish_dev", catalog = "")
public class ProductFilterValue {
    @Id
    @Column(name = "compare_filter_id", nullable = false)
    private int compareFilterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compare_filter_id", insertable = false, updatable = false)
    private CompareFilter compareFilter;

    @Id
    @Column(name = "product_id", nullable = false)
    private int productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @Basic
    @Column(name = "value", nullable = false)
    private String value;
}
