package com.matsinger.barofishserver.order.domain.repository;

import com.matsinger.barofishserver.order.domain.model.OrderDeliverPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderDeliverPlaceRepository extends JpaRepository<OrderDeliverPlace, String> {
    void deleteAllByOrderIdIn(List<String> orderIds);
}
