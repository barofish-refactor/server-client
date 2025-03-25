package com.matsinger.barofishserver.domain.product.filter.domain;

import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 카테고리에 대한 필터와 필드의 완전한 조합을 저장하는 엔티티
 * 최종 검색/필터링에 사용됨
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "category_filter_field_combinations")
public class CategoryFilterFieldCombination extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "category_id", nullable = false, insertable = false, updatable = false)
    private Integer categoryId;
    
    // 필터 키 (구분='양식,자연산',지역='동해,서해' 형식)
    @Column(name = "filter_keys", nullable = false, length = 500)
    private String filterKeys;
    
    // ID 형식의 필터 키 ('1='2,3',2='4,5'' 형식)
    @Column(name = "filter_ids", nullable = false, length = 500)
    private String filterIds;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;
} 