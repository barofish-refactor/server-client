package com.matsinger.barofishserver.order.application;

import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.order.application.dto.OrderDto;
import com.matsinger.barofishserver.order.application.dto.OrderProductDto;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.Orders;
import com.matsinger.barofishserver.order.domain.service.OrderProductDtoConverter;
import com.matsinger.barofishserver.order.domain.service.OrderReader;
import com.matsinger.barofishserver.order.domain.service.OrderValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryUseCase {
    
    private final OrderProductDtoConverter orderProductDtoConverter;
    private final OrderValidator orderValidator;
    private final OrderReader orderReader;

    public OrderDto getOrder(String orderId, TokenInfo tokenInfo) {
        Orders order = orderReader.getOrder(orderId);
        orderValidator.validateOrderAccess(order, tokenInfo);
        
        return createOrderDto(order, tokenInfo);
    }

    private OrderDto createOrderDto(Orders order, TokenInfo tokenInfo) {
        List<OrderProductInfo> filteredProducts = tokenInfo.getType().equals(TokenAuthType.PARTNER)
            ? order.getProductInfos().stream()
                .filter(product -> product.getStoreId().equals(tokenInfo.getId()))
                .toList()
            : order.getProductInfos();
        
        List<OrderProductDto> productDtos = filteredProducts.stream()
            .map(product -> orderProductDtoConverter.convertToDto(order.getUserId(), product))
            .toList();

        return OrderDto.from(order, productDtos);
    }
} 