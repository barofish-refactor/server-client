package com.matsinger.barofishserver.domain.product.index.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Entity
@Table(name = "product_recommend_cache")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRecommendCache {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private Integer categoryId;

    @Column
    private Integer subCategoryId;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String productIds;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static ProductRecommendCache from(Integer categoryId, Integer subCategoryId, String productIds) {
        return ProductRecommendCache.builder()
                .categoryId(categoryId)
                .subCategoryId(subCategoryId)
                .productIds(productIds)
                .build();

    }

    @PrePersist
    @PreUpdate
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }
} 