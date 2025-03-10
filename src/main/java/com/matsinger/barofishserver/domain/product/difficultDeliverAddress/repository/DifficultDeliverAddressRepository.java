package com.matsinger.barofishserver.domain.product.difficultDeliverAddress.repository;

import com.matsinger.barofishserver.domain.product.difficultDeliverAddress.domain.DifficultDeliverAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface DifficultDeliverAddressRepository extends JpaRepository<DifficultDeliverAddress, Integer> {
    List<DifficultDeliverAddress> findAllByProductId(Integer productId);

    void deleteAllByProductId(Integer productId);

    List<Integer> findProductIdsByProductIdInAndBcodeStartingWith(Set<Integer> productIds, String orderBcode);
}
