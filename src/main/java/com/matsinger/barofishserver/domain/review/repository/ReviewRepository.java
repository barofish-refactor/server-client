package com.matsinger.barofishserver.domain.review.repository;

import com.matsinger.barofishserver.domain.review.domain.Review;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer>, JpaSpecificationExecutor<Review> {

    Page<Review> findAllByUserId(Integer userId, Pageable pageable);

    Integer countAllByUserId(Integer userId);

    Integer countAllByProductId(Integer productId);
    Integer countByIsDeletedFalseAndStoreId(Integer storeId);

    Page<Review> findAllByStoreId(Integer storeId, Pageable pageable);

    Page<Review> findAllByStoreIdOrderByCreatedAtDesc(Integer storeId, Pageable pageable);

    Page<Review> findAllByProductIdAndIsDeletedFalseOrderByCreatedAtDesc(Integer productId, Pageable pageable);

    @Query(value = "SELECT r.*\n" +
            "FROM review r\n" +
            "         LEFT JOIN review_like rl ON r.id = rl.review_id\n" +
            "WHERE store_id = :storeId\n" +
            "GROUP BY r.id\n" +
            "ORDER BY COUNT( rl.user_id ) DESC\n", countQuery = "SELECT COUNT(*)\n" +
            "FROM review r\n" +
            "         JOIN review_like rl ON r.id = rl.review_id\n" +
            "WHERE store_id = :storeId\n", nativeQuery = true)
    Page<Review> selectReviewOrderedBestWithStoreId(@Param(value = "storeId") Integer storeId, Pageable pageable);

    @Query(value = "SELECT r.*\n" +
            "FROM review r\n" +
            "        LEFT OUTER JOIN review_like rl ON r.id = rl.review_id\n" +
            "WHERE r.product_id = :productId\n" +
            "GROUP BY r.id\n" +
            "ORDER BY COUNT( rl.user_id ) DESC\n", countQuery = "SELECT COUNT(*)\n" +
            "FROM review r\n" +
            "        LEFT OUTER JOIN review_like rl ON r.id = rl.review_id\n" +
            "WHERE product_id = :productId\n", nativeQuery = true)
    Page<Review> selectReviewOrderedBestWithProductId(@Param(value = "productId") Integer productId, Pageable pageable);

    @Query(value = "SELECT re.evaluation, COUNT( re.evaluation ) AS count\n" +
            "FROM review r\n" +
            "         JOIN review_evaluation re ON r.id = re.review_id\n" +
            "WHERE r.product_id = :productId\n" +
            "GROUP BY re.evaluation;", nativeQuery = true)
    List<Tuple> selectReviewStatisticsWithProductId(Integer productId);


    @Query(value = "SELECT re.evaluation, COUNT( re.evaluation ) AS count\n" +
            "FROM review r\n" +
            "         JOIN review_evaluation re ON r.id = re.review_id\n" +
            "WHERE r.store_id = :storeId\n" +
            "GROUP BY re.evaluation;", nativeQuery = true)
    List<Tuple> selectReviewStatisticsWithStoreId(Integer storeId);

    @Query(value = "SELECT COUNT(*) as count FROM review_like WHERE review_id = :reviewId", nativeQuery = true)
    Integer selectReviewLikeCountWithReviewId(Integer reviewId);

    void deleteAllByUserId(Integer userId);

    Boolean existsByUserIdAndProductIdAndOrderProductInfoId(Integer userId,
                                                            Integer productId,
                                                            Integer orderProductInfoId);

    void deleteAllByUserIdIn(List<Integer> userIds);

    Integer countAllByProductIdAndIsDeleted(Integer productId, boolean isDeleted);

    boolean existsByUserIdAndOrderProductInfoId(Integer userId, int orderProductInfoId);

    @Query("SELECT r.product.id, COUNT(r) FROM Review r WHERE r.product.id IN :productIds AND r.isDeleted = :deleted GROUP BY r.product.id")
    List<Object[]> countReviewsGroupByProductId(@Param("productIds") List<Integer> productIds, @Param("deleted") Boolean deleted);
}
