package com.matsinger.barofishserver.global.config;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * MongoDB 문서에 대한 생성 시간 및 수정 시간 자동 관리를 위한 기본 엔티티
 */
@Getter
public abstract class MongoBaseTimeEntity {

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 엔티티 생성 시 시간 정보 초기화
     */
    public void initialize() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }
    
    /**
     * 수정 시간 업데이트
     */
    public void updateModifiedTime() {
        this.updatedAt = LocalDateTime.now();
    }
} 