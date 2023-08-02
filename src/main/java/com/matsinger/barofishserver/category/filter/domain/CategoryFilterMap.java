package com.matsinger.barofishserver.category.filter.domain;

import com.matsinger.barofishserver.category.filter.domain.CategoryFilterId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@IdClass(CategoryFilterId.class)
@Table(name = "category_filter_map", schema = "barofish_dev", catalog = "")
public class CategoryFilterMap {
    @Id
    @Column(name = "compare_filter_id")
    private int compareFilterId;

    @Id
    @Column(name = "category_id")
    private int categoryId;
}
