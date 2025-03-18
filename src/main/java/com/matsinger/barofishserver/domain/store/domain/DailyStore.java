package com.matsinger.barofishserver.domain.store.domain;

import com.matsinger.barofishserver.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Timestamp;

@Entity
@Table(name = "daily_store")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyStore extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "store_id", nullable = false)
    private int storeId;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "store_id", updatable = false, insertable = false)
    private StoreInfo storeInfo;

    @Column(name = "deleted")
    private boolean deleted;

    public void markAsDeleted() {
        this.deleted = true;
    }
    
    public static DailyStore createFrom(Store store) {
        return DailyStore.builder()
            .storeId(store.getId())
            .deleted(false)
            .build();
    }
    
    public boolean isToday(LocalDateTime today) {
        LocalDate todayDate = today.toLocalDate();
        LocalDate createdDate = getCreatedAt().toLocalDateTime().toLocalDate();
        return todayDate.equals(createdDate);
    }
}
