package com.matsinger.barofishserver.domain.product.index.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_recommend")
@Getter
@NoArgsConstructor
public class ProductRecommend {
    @EmbeddedId
    private ProductRecommendId id;

    @Column(nullable = false)
    private Integer weight;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }
} 