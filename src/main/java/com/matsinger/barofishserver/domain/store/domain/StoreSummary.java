package com.matsinger.barofishserver.domain.store.domain;

import com.matsinger.barofishserver.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_summary")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSummary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private Integer storeId;

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

    @Column
    private Integer productCnt;

    @Column(nullable = false)
    private Boolean deleted = false;
}
