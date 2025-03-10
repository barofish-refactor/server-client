package com.matsinger.barofishserver.order.domain.service;

import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.option.domain.Option;
import com.matsinger.barofishserver.domain.product.option.application.OptionQueryService;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.domain.review.application.ReviewQueryService;
import com.matsinger.barofishserver.order.application.dto.OrderProductDto;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderProductDtoConverter {
    private final ProductService productService;
    private final StoreService storeService;
    private final OptionQueryService optionQueryService;
    private final ReviewQueryService reviewQueryService;

    public OrderProductDto convertToDto(Integer userId, OrderProductInfo orderProductInfo) {
        Product product = productService.selectProduct(orderProductInfo.getProductId());
        StoreInfo storeInfo = storeService.selectStoreInfo(product.getStoreId());
        OptionItem optionItem = productService.selectOptionItem(orderProductInfo.getOptionItemId());
        Option option = optionItem.getOption();
        boolean isReviewWritten = reviewQueryService.checkReviewWritten(userId, product.getId(), orderProductInfo.getId());

        return OrderProductDto.from(
                orderProductInfo,
                product,
                storeInfo,
                optionItem,
                option,
                isReviewWritten
        );
    }
}