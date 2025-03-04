package com.matsinger.barofishserver.order.domain.repository;

import com.matsinger.barofishserver.order.domain.model.OrderState;
import com.matsinger.barofishserver.order.domain.model.Orders;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.util.List;

public interface OrderRepository extends JpaRepository<Orders, String>, JpaSpecificationExecutor<Orders> {
    @Query(value = "select concat(date_format(current_timestamp()+interval 9 hour,'%y%m%d%H%i%s'),lpad(nextval" +
            "(order_seq),4,'0')) id", nativeQuery = true)
    Tuple selectOrderId();

    Page<Orders> findAllByStateNotInAndUserId(List<OrderState> orderStates, Integer userId, PageRequest pageRequest);

    Page<Orders> findAllByUserId(Integer userId, Pageable pageable);

    List<Orders> findAllByStateInAndUserId(List<OrderState> orderStates, Integer userId);

    Integer countAllByOrderedAtBetween(Timestamp startAt, Timestamp endAt);

    List<Orders> findAllByUserIdIn(List<Integer> userIds);

    void deleteAllByIdIn(List<String> ids);
}
