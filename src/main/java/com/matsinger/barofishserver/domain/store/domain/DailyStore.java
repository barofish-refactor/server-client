package com.matsinger.barofishserver.domain.store.domain;

import com.matsinger.barofishserver.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyStore extends BaseTimeEntity {
    @Id
    @Column(name = "store_id", nullable = false)
    private int storeId;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "store_id", updatable = false, insertable = false)
    private StoreInfo storeInfo;

    @Column(name = "deleted")
    private boolean deleted;

    public boolean isCreatedToday() {
        return getCreatedAt().toLocalDateTime().toLocalDate()
            .isEqual(LocalDateTime.now().toLocalDate());
    }

    public void markAsDeleted() {
        this.deleted = true;
    }

    public static DailyStore createFrom(StoreInfo storeInfo) {
        return DailyStore.builder()
            .storeId(storeInfo.getStoreId())
            .deleted(false)
            .storeInfo(storeInfo)
            .build();
    }
    
    public static DailyStore createFrom(Store store) {
        return DailyStore.builder()
            .storeId(store.getId())
            .deleted(false)
            .build();
    }
}
