package com.matsinger.barofishserver.order.domain.repository;

import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface OrderProductInfoRepository extends JpaRepository<OrderProductInfo, Integer>,
        JpaSpecificationExecutor<OrderProductInfo>, OrderProductInfoRepositoryCustom {
    List<OrderProductInfo> findAllByOrderId(String orderId);

    List<OrderProductInfo> findAllByStateIn(List<OrderProductState> state);

    List<OrderProductInfo> findAllByIdIn(List<Integer> ids);

    List<OrderProductInfo> findAllByDeliveryDoneAtBefore(Timestamp timestamp);

    List<OrderProductInfo> findAllByProduct_StoreIdAndIsSettled(Integer storeId, Boolean isSettled);

    @Query(value = "SELECT opi.product_id as productId," +
            " COUNT( * ) as count," +
            " DENSE_RANK( ) OVER (ORDER BY COUNT( * ) DESC ) as ranking\n" +
            "FROM order_product_info opi\n" +
            " JOIN orders o ON opi.order_id = o.id\n" +
            "WHERE opi.state not in  ('WAIT_DEPOSIT', 'CANCELED', 'CANCEL_REQUEST', 'REFUND_REQUEST')\n" +
            "AND o.ordered_at BETWEEN :startAt AND :endAt\n" +
            "GROUP BY opi.product_id\n" +
            "LIMIT 10;", nativeQuery = true)
    List<Tuple> getProductOrderCountRank(@Param(value = "startAt") Timestamp startAt,
                                         @Param(value = "endAt") Timestamp endAt);

    @Query(value =
            "SELECT opi.product_id AS productId, COUNT( * ) AS count, DENSE_RANK( ) OVER (ORDER BY COUNT( * ) DESC ) AS ranking\n" +
                    "FROM order_product_info opi\n" +
                    "         JOIN product p ON p.id = opi.product_id\n" +
                    " JOIN orders o ON opi.order_id = o.id\n" +
                    "WHERE opi.state not in  ('WAIT_DEPOSIT', 'CANCELED', 'CANCEL_REQUEST', 'REFUND_REQUEST')\n" +
                    "  AND p.store_id = :storeId\n" +
                    "AND o.ordered_at BETWEEN :startAt AND :endAt\n" +
                    "GROUP BY opi.product_id\n" +
                    "LIMIT 10;", nativeQuery = true)
    List<Tuple> getProductOrderCountRank(@Param(value = "storeId") Integer storeId,
                                         @Param(value = "startAt") Timestamp startAt,
                                         @Param(value = "endAt") Timestamp endAt);

    @Query(value = "SELECT COUNT( DISTINCT o.id ) AS count\n" +
            "FROM order_product_info opi\n" +
            "         JOIN orders o ON o.id = opi.order_id\n" +
            "WHERE opi.state = 'FINAL_CONFIRM'\n" +
            "  AND o.user_id = :userId", nativeQuery = true)
    Tuple countFinalConfirmedOrderWithUserId(Integer userId);

    void deleteAllByOrderIdIn(List<String> orderIds);
}
