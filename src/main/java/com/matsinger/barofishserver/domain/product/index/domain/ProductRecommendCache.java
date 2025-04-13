package com.matsinger.barofishserver.domain.product.index.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_recommend_cache")
@Getter
@NoArgsConstructor
public class ProductRecommendCache {
    @EmbeddedId
    private ProductRecommendCacheId id;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String productIds;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }
} 