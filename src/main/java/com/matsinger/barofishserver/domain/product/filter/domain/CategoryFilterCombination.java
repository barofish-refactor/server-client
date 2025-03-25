package com.matsinger.barofishserver.domain.product.filter.domain;

import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 카테고리에 적용 가능한 모든 필터 조합을 저장하는 엔티티
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "category_filter_combinations")
public class CategoryFilterCombination extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "category_id", nullable = false, insertable = false, updatable = false)
    private Integer categoryId;
    
    @Column(name = "filter_ids", nullable = false, length = 500)
    private String filterIds; // 오름차순 정렬된 필터 ID (예: "1,3,5")
    
    @Column(name = "filter_names", nullable = false, length = 500)
    private String filterNames; // 필터 이름들 (예: "구분,지역,크기")
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;
} 