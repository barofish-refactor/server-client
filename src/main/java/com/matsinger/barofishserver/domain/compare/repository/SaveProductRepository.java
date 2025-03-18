package com.matsinger.barofishserver.domain.compare.repository;

import com.matsinger.barofishserver.domain.compare.domain.SaveProduct;
import com.matsinger.barofishserver.domain.compare.domain.SaveProductId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaveProductRepository extends JpaRepository<SaveProduct, SaveProductId> {
    public List<SaveProduct> findAllByUserId(Integer userId);

    Integer countAllByUserId(Integer userId);

    void deleteAllByUserIdIn(List<Integer> userIds);

    List<SaveProduct> findAllByUserIdAndProductIdIn(Integer userId, List<Integer> productIds);
}
