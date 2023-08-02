package com.matsinger.barofishserver.store.domain;

import lombok.*;

import java.io.Serializable;

@Data
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreScrapId implements Serializable {
    private Integer storeId;
    private Integer userId;
}
