package com.matsinger.barofishserver.order.domain.service;

import com.matsinger.barofishserver.domain.product.application.ProductQueryService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.store.application.StoreInfoQueryService;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.domain.user.deliverplace.DeliverPlace;
import com.matsinger.barofishserver.domain.user.deliverplace.application.DeliverPlaceQueryService;
import com.matsinger.barofishserver.domain.userinfo.application.UserInfoQueryService;
import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import com.matsinger.barofishserver.order.application.dto.OrderProductReq;
import com.matsinger.barofishserver.order.application.dto.OrderReq;
import com.matsinger.barofishserver.order.application.service.OrderQueryService;
import com.matsinger.barofishserver.order.application.service.OrderCreationValidator;
import com.matsinger.barofishserver.order.domain.model.*;
import com.matsinger.barofishserver.order.domain.repository.OrderDeliverPlaceRepository;
import com.matsinger.barofishserver.order.domain.repository.OrderProductInfoRepository;
import com.matsinger.barofishserver.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderCreationHandler {
    private final OrderCreationService orderCreationService;
    private final OrderCreationValidator orderCreationValidator;
    private final OrderRepository orderRepository;
    private final OrderProductInfoRepository orderProductInfoRepository;
    private final OrderDeliverPlaceRepository orderDeliverPlaceRepository;
    private final UserInfoQueryService userInfoQueryService;
    private final ProductQueryService productQueryService;
    private final StoreInfoQueryService storeInfoQueryService;
    private final DeliverPlaceQueryService deliverPlaceQueryService;
    private final OrderQueryService orderQueryService;

    @Transactional
    public OrderCreationResult createOrder(OrderReq request, Integer userId) {
        // 초기 요청 검증
        orderCreationValidator.validateInitialRequest(request);

        // 주문 준비 데이터 수집
        OrderPreparationData prepData = prepareOrderData(request, userId);

        // 주문 생성
        OrderCreationResult result = orderCreationService.createOrder(
            request, 
            prepData.getUserInfo(), 
            prepData.getOrderId(), 
            prepData.getDeliverPlace(), 
            prepData.getProducts(), 
            prepData.getStores()
        );

        // 주문 검증
        orderCreationValidator.validateOrderCreation(request, result, prepData.getUserInfo());

        // 주문 정보 저장
        saveOrder(result);

        return result;
    }

    private void saveOrder(OrderCreationResult result) {
        orderRepository.save(result.getOrder());
        orderProductInfoRepository.saveAll(result.getOrder().getProductInfos());
        orderDeliverPlaceRepository.save(result.getOrderDeliverPlace());
    }

    private OrderPreparationData prepareOrderData(OrderReq request, Integer userId) {
        UserInfo userInfo = userInfoQueryService.findByUserId(userId);
        String orderId = orderQueryService.getOrderId();
        DeliverPlace deliverPlace = deliverPlaceQueryService.findById(request.getDeliverPlaceId());

        List<Product> products = findProducts(request);
        List<StoreInfo> stores = findStores(products);

        return OrderPreparationData.builder()
                .userInfo(userInfo)
                .orderId(orderId)
                .deliverPlace(deliverPlace)
                .products(products)
                .stores(stores)
                .build();
    }

    private List<Product> findProducts(OrderReq request) {
        List<Integer> productIds = request.getProducts().stream()
                .map(OrderProductReq::getProductId)
                .collect(Collectors.toList());
        return productQueryService.findByIds(productIds);
    }

    private List<StoreInfo> findStores(List<Product> products) {
        List<Integer> storeIds = products.stream()
                .map(Product::getStoreId)
                .distinct()
                .collect(Collectors.toList());
        return storeInfoQueryService.findByStoreIds(storeIds);
    }
} 