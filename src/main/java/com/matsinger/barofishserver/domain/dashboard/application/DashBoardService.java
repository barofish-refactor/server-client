package com.matsinger.barofishserver.domain.dashboard.application;

import com.matsinger.barofishserver.domain.dashboard.dto.DashBoardType;
import com.matsinger.barofishserver.domain.dashboard.dto.ProductRankDto;
import com.matsinger.barofishserver.domain.inquiry.domain.Inquiry;
import com.matsinger.barofishserver.domain.inquiry.dto.InquiryDto;
import com.matsinger.barofishserver.domain.inquiry.repository.InquiryRepository;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.order.domain.repository.OrderProductInfoRepository;
import com.matsinger.barofishserver.order.domain.repository.OrderRepository;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.domain.user.repository.UserRepository;
import com.matsinger.barofishserver.utils.Common;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Predicate;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class DashBoardService {
    private final UserRepository userRepository;
    private final InquiryRepository inquiryRepository;
    private final OrderRepository orderRepository;
    private final OrderProductInfoRepository infoRepository;
    private final ProductService productService;
    private final StoreService storeService;
    private final Common utils;

    public Timestamp getStartAt(DashBoardType type) {
        Calendar calendar = Calendar.getInstance();
        Integer lastDate = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (type.equals(DashBoardType.DAILY)) {
            calendar.set(calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DATE),
                    0,
                    0,
                    0);
        } else {calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1, 0, 0, 0);}
        return Timestamp.from(calendar.getTime().toInstant());
    }

    public Timestamp getEndAt(DashBoardType type) {
        Calendar calendar = Calendar.getInstance();
        int lastDate = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        if (type.equals(DashBoardType.DAILY)) {
            calendar.set(calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DATE),
                    23,
                    59,
                    59);
        } else {calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), lastDate, 23, 59, 59);}
        return Timestamp.from(calendar.getTime().toInstant());
    }

    public Integer selectJoinCount(DashBoardType type) {
        Timestamp startAt = getStartAt(type);
        Timestamp endAt = getEndAt(type);

        return userRepository.countAllByJoinAtBetween(startAt, endAt);
    }

    public List<InquiryDto> selectInquiryList(Integer storeId) {
        Timestamp startAt = getStartAt(DashBoardType.DAILY);
        Timestamp endAt = getEndAt(DashBoardType.DAILY);
        Specification<Inquiry> inquirySpec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (storeId != null) predicates.add(builder.equal(root.get("product").get("storeId"), storeId));
            predicates.add(builder.lessThan(root.get("createdAt"), endAt));
            predicates.add(builder.isNull(root.get("answeredAt")));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        List<Inquiry> inquiries = inquiryRepository.findAll(inquirySpec, Sort.by(Sort.Direction.DESC, "createdAt"));
        return inquiries.stream().map(Inquiry::convert2Dto).toList();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DashBoardOrderAggregation {
        Integer count;
        Integer amount;
    }

    public DashBoardOrderAggregation getOrderCount(DashBoardType type, Integer storeId) {
        Timestamp startAt = getStartAt(type);
        Timestamp endAt = getEndAt(type);
        int totalAmount = 0;
        List<OrderProductInfo> orderProductInfos;
        if (storeId == null) {
            Specification<OrderProductInfo> orderSpec = (root, query, builder) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(builder.greaterThan(root.get("order").get("orderedAt"), startAt));
                predicates.add(builder.lessThan(root.get("order").get("orderedAt"), endAt));
                predicates.add(builder.not(root.get("state").in(List.of(OrderProductState.WAIT_DEPOSIT,
                        OrderProductState.CANCELED))));
                query.distinct(true);
                return builder.and(predicates.toArray(new Predicate[0]));
            };
            orderProductInfos = infoRepository.findAll(orderSpec, Sort.by(Sort.Direction.DESC, "orderId"));
        } else {
            Specification<OrderProductInfo> orderSpec = (root, query, builder) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (storeId != null) predicates.add(builder.equal(root.get("product").get("storeId"), storeId));
                predicates.add(builder.greaterThan(root.get("order").get("orderedAt"), startAt));
                predicates.add(builder.lessThan(root.get("order").get("orderedAt"), endAt));
                predicates.add(builder.not(root.get("state").in(List.of(OrderProductState.WAIT_DEPOSIT,
                        OrderProductState.CANCELED))));
                query.distinct(true);
                return builder.and(predicates.toArray(new Predicate[0]));
            };
            orderProductInfos = infoRepository.findAll(orderSpec, Sort.by(Sort.Direction.DESC, "orderId"));

        }
        Integer count = orderProductInfos.size();
        for (OrderProductInfo info : orderProductInfos) {
            totalAmount += info.getPrice();
        }
        return DashBoardOrderAggregation.builder().count(count).amount(totalAmount).build();
    }

    public List<ProductRankDto> getProductRankList(DashBoardType type, Integer storeId) {
        Timestamp startAt = getStartAt(type);
        Timestamp endAt = getEndAt(type);
        List<Tuple> rankList = new ArrayList<>();
        if (storeId == null) rankList = infoRepository.getProductOrderCountRank(startAt, endAt);
        else rankList = infoRepository.getProductOrderCountRank(storeId, startAt, endAt);
        List<ProductRankDto> rankDtos = new ArrayList<>();
        for (Tuple rank : rankList) {
            Product product = productService.selectProduct(Integer.parseInt(rank.get("productId").toString()));
            StoreInfo storeInfo = storeService.selectStoreInfo(product.getStoreId());
            ProductRankDto
                    rankDto =
                    ProductRankDto.builder().productId(product.getId()).productName(product.getTitle()).storeName(
                            storeInfo.getName()).count(Integer.parseInt(rank.get("count").toString())).rank(Integer.parseInt(
                            rank.get("ranking").toString())).build();
            rankDtos.add(rankDto);
        }
        return rankDtos;
    }
}
