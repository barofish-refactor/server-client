package com.matsinger.barofishserver.domain.product.domain;

import com.matsinger.barofishserver.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "product_summary")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSummary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "product_id", insertable = false, updatable = false)
    private Integer productId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column
    private Integer reviewCnt;

    @Column
    private Integer tasteCnt;

    @Column
    private Integer freshCnt;

    @Column
    private Integer packagingCnt;

    @Column
    private Integer sizeCnt;

    @Column
    private Integer priceCnt;

    @Column(nullable = false)
    private Boolean deleted = false;
} 