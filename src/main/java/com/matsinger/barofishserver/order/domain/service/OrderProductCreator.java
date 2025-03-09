package com.matsinger.barofishserver.order.domain.service;

import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.order.application.dto.OrderProductReq;
import com.matsinger.barofishserver.order.application.dto.OrderReq;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderProductCreator {

    public List<OrderProductInfo> createOrderProducts(OrderReq request, String orderId, Map<Integer, Product> productMap) {
        return request.getProducts().stream()
                .map(orderProductReq -> createOrderProduct(orderId, orderProductReq, productMap))
                .collect(Collectors.toList());
    }

    private OrderProductInfo createOrderProduct(String orderId, OrderProductReq orderProductReq, Map<Integer, Product> productMap) {
        Product product = findProduct(orderProductReq.getProductId(), productMap);
        OptionItem optionItem = findAndValidateOption(product, orderProductReq.getOptionId());
        optionItem.validateQuantity(orderProductReq.getAmount(), product.getTitle());

        int totalProductPrice = calculateTotalPrice(optionItem, orderProductReq.getAmount());

        return OrderProductInfo.builder()
                .orderId(orderId)
                .productId(product.getId())
                .storeId(product.getStoreId())
                .optionItemId(orderProductReq.getOptionId())
                .settlePrice(optionItem.getPurchasePrice())
                .originPrice(optionItem.getDiscountPrice())
                .price(totalProductPrice)
                .amount(orderProductReq.getAmount())
                .deliveryFeeType(product.getDeliverFeeType())
                .isSettled(false)
                .isTaxFree(!product.getNeedTaxation())
                .taxFreeAmount(!product.getNeedTaxation() ? totalProductPrice : 0)
                .state(OrderProductState.WAIT_DEPOSIT)
                .build();
    }

    private Product findProduct(Integer productId, Map<Integer, Product> productMap) {
        Product product = productMap.get(productId);
        if (product == null) {
            throw new BusinessException("상품을 찾을 수 없습니다.");
        }
        validateProductState(product);
        return product;
    }

    private OptionItem findAndValidateOption(Product product, Integer optionId) {
        return product.getOptionItems().stream()
                .filter(item -> item.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("옵션 아이템 정보를 찾을 수 없습니다."));
    }

    private int calculateTotalPrice(OptionItem optionItem, int amount) {
        return optionItem.getDiscountPrice() * amount;
    }

    private void validateProductState(Product product) {
        if (product.isPromotionEnd()) {
            throw new BusinessException("프로모션 기간이 아닌 상품이 포함되어 있습니다.");
        }
        product.validateState();
    }
} 