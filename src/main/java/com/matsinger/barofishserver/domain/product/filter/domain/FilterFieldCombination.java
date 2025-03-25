package com.matsinger.barofishserver.domain.product.filter.domain;

import com.matsinger.barofishserver.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "filter_field_combinations")
public class FilterFieldCombination extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "filter_id", nullable = false)
    private Integer filterId;
    
    @Column(name = "filter_name", nullable = false)
    private String filterName;
    
    @Column(name = "field_ids", nullable = false, length = 500)
    private String fieldIds; // 오름차순 정렬된 필드 ID (예: "2,3,5")
    
    @Column(name = "field_names", nullable = false, length = 500)
    private String fieldNames; // 해당 필드 이름들 (예: "양식,자연산,냉동")
}