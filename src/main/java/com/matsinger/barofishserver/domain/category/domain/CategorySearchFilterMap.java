package com.matsinger.barofishserver.domain.category.domain;

import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 카테고리와 검색 필터 간의 매핑을 나타내는 엔티티
 * 특정 카테고리에 어떤 검색 필터가 적용되는지를 결정합니다.
 */
@Entity
@Table(name = "category_search_filter_map")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySearchFilterMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 카테고리 관계 설정
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * 검색 필터 관계 설정
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_filter_id", nullable = false)
    private SearchFilter searchFilter;

    /**
     * 생성 시간
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 시간
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 카테고리와 검색 필터로 매핑 생성
     */
    public static CategorySearchFilterMap createMapping(Category category, SearchFilter searchFilter) {
        return CategorySearchFilterMap.builder()
                .category(category)
                .searchFilter(searchFilter)
                .build();
    }
} 