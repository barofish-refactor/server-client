package com.matsinger.barofishserver.domain.product.filter.domain;

import com.matsinger.barofishserver.global.config.MongoBaseTimeEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class FilterProductCache extends MongoBaseTimeEntity {

    @Id
    private String id;

    private Integer categoryId;
    private Integer subCategoryId;
    private Integer filterId;
    private List<Integer> fieldIds;
    private List<Integer> productIds;
    
    private Integer productCount;

    @Builder.Default
    private LocalDateTime lastAccessed = LocalDateTime.now();

    @Override
    public void initialize() {
        super.initialize();
        if (this.productIds != null) {
            this.productCount = this.productIds.size();
        } else {
            this.productCount = 0;
        }
    }

    public void updateLastAccessed() {
        this.lastAccessed = LocalDateTime.now();
        updateModifiedTime();
    }
} 