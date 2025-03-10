package com.matsinger.barofishserver.order.domain.repository;

import com.matsinger.barofishserver.order.domain.model.OrderProductOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductOptionRepository extends JpaRepository<OrderProductOption, Integer> {
    OrderProductOption findFirstByOrderProductId(Integer orderProductId);
}
