package com.matsinger.barofishserver.domain.basketProduct.application;

import com.matsinger.barofishserver.domain.basketProduct.domain.BasketProductInfo;
import com.matsinger.barofishserver.domain.basketProduct.domain.BasketProductOption;
import com.matsinger.barofishserver.domain.basketProduct.dto.BasketProductDto;
import com.matsinger.barofishserver.domain.basketProduct.dto.BasketProductDtoV2;
import com.matsinger.barofishserver.domain.basketProduct.dto.BasketProductInfoDto;
import com.matsinger.barofishserver.domain.basketProduct.repository.BasketProductInfoRepository;
import com.matsinger.barofishserver.domain.basketProduct.repository.BasketProductOptionRepository;
import com.matsinger.barofishserver.domain.basketProduct.repository.BasketQueryRepository;
import com.matsinger.barofishserver.domain.product.application.ProductQueryService;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.dto.ProductListDto;
import com.matsinger.barofishserver.domain.product.option.application.OptionQueryService;
import com.matsinger.barofishserver.domain.product.option.domain.Option;
import com.matsinger.barofishserver.domain.product.optionitem.application.OptionItemQueryService;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.product.optionitem.dto.OptionItemDto;
import com.matsinger.barofishserver.domain.product.optionitem.repository.OptionItemRepository;
import com.matsinger.barofishserver.domain.product.productfilter.application.ProductFilterService;
import com.matsinger.barofishserver.domain.review.repository.ReviewRepository;
import com.matsinger.barofishserver.domain.store.application.StoreQueryService;
import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.domain.store.domain.Store;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.domain.store.dto.SimpleStore;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class BasketQueryService {
    private final BasketProductInfoRepository basketProductInfoRepository;
    private final BasketProductOptionRepository basketProductOptionRepository;
    private final OptionItemRepository optionItemRepository;
    private final ReviewRepository reviewRepository;
    private final StoreService storeService;
    private final ProductFilterService productFilterService;
    private final ProductService productService;
    private final ProductQueryService productQueryService;
    private final StoreQueryService storeQueryService;
    private final OptionQueryService optionQueryService;
    private final OptionItemQueryService optionItemQueryService;
    private BasketQueryRepository basketQueryRepository;

    public BasketProductInfo selectBasket(Integer id) {
        return basketProductInfoRepository.findById(id).orElseThrow(() -> {
            throw new BusinessException("장바구니 상품 정보를 찾을 수 없습니다.");
        });

    }

    public Integer countBasketList(Integer userId) {
        List<BasketProductInfo> infos = basketProductInfoRepository.findAllByUserId(userId);
        return infos.size();
    }

    public List<BasketProductDto> selectBasketList(Integer userId) {
        List<BasketProductInfo> infos = basketProductInfoRepository.findAllByUserId(userId);
        List<BasketProductDto> productDtos = new ArrayList<>();
        for (BasketProductInfo info : infos) {

            Product product = productService.selectProduct(info.getProductId());
            StoreInfo storeInfo = storeService.selectStoreInfo(product.getStoreId());
            SimpleStore store = storeService.convert2SimpleDto(storeInfo, userId);

            List<BasketProductOption> options = basketProductOptionRepository.findAllByOrderProductId(info.getId());
            BasketProductOption option = options.size() == 0 ? null : options.get(0);

            OptionItemDto optionDto;
            if (option == null) {
                optionDto = null;
            } else {
                OptionItem
                        optionItem =
                        optionItemRepository.findById(option.getOptionId()).orElseThrow(() -> new BusinessException(
                                "옵션 아이템 정보를 찾을 수 없습니다."));
                optionDto = optionItem.convert2Dto(product);
                optionDto.setDeliverBoxPerAmount(product.getDeliverBoxPerAmount());
                optionDto.setPointRate(product.getPointRate());
            }

            ProductListDto productListDto = createProductListDto(product);
            BasketProductDto
                    basketProductDto =
                    BasketProductDto.builder()
                            .id(info.getId())
                            .product(productListDto)
                            .amount(info.getAmount())
                            .deliveryFee(store.getDeliveryFee())
                            .deliverFeeType(product.getDeliverFeeType())
                            .minOrderPrice(product.getMinOrderPrice())
                            .minStorePrice(store.getMinStorePrice())
                            .isConditional(storeInfo.getIsConditional())
                            .store(store)
                            .option(optionDto)
                            .build();
            productDtos.add(basketProductDto);
        }
        return productDtos;
    }

    private ProductListDto createProductListDto(Product product) {
        StoreInfo storeInfo = storeService.selectStoreInfo(product.getStoreId());
        Integer reviewCount = reviewRepository.countAllByProductId(product.getId());
        OptionItem
                optionItem =
                optionItemRepository.findById(product.getRepresentOptionItemId()).orElseThrow(() -> new Error(
                        "옵션 아이템 정보를 찾을 수 없습니다."));

        return ProductListDto.builder().id(product.getId()).state(product.getState()).image(product.getImages().substring(
                1,
                product.getImages().length() - 1).split(",")[0]).originPrice(optionItem.getOriginPrice()).discountPrice(
                optionItem.getDiscountPrice()).title(product.getTitle()).reviewCount(reviewCount).storeId(storeInfo.getStoreId()).storeName(
                storeInfo.getName()).parentCategoryId(product.getCategory() !=
                null ? product.getCategory().getCategoryId() : null).filterValues(productFilterService.selectProductFilterValueListWithProductId(
                product.getId())).build();
    }

    public List<BasketProductDtoV2> selectBasketListV2(Integer userId) {
        List<BasketProductInfo> basketProductInfos = basketProductInfoRepository.findAllByUserId(userId);
        List<BasketProductDtoV2> response = new ArrayList<>();
        for (BasketProductInfo basketProductInfo : basketProductInfos) {
            Product product = productQueryService.findById(basketProductInfo.getProductId());
            Store store = storeQueryService.findById(basketProductInfo.getStoreId());
            StoreInfo storeInfo = store.getStoreInfo();
            Option option = optionQueryService.findById(basketProductInfo.getOptionId());
            OptionItem optionItem = optionItemQueryService.findById(basketProductInfo.getOptionItemId());

            BasketProductInfoDto basketProductInfoDto = getBasketProductInfoDto(basketProductInfo, product, optionItem, storeInfo);
            OptionItemDto optionItemDto = getOptionItemDto(basketProductInfo, optionItem, product);

            response.add(
                    BasketProductDtoV2.builder()
                    .id(basketProductInfo.getId())
                    .store(storeInfo.toBasketStoreDto())
                    .product(basketProductInfoDto)
                    .amount(basketProductInfo.getAmount())
                    .inventoryQuantity(optionItem.getAmount())
                    .deliverFeeType(product.getDeliverFeeType())
                    .minOrderPrice(product.getMinOrderPrice())
                    .deliveryFee(product.getDeliverFee())
                    .isConditional(storeInfo.getIsConditional())
                    .minStorePrice(storeInfo.getMinStorePrice())
                    .option(optionItemDto)
                    .build()
            );
        }
        return response;
    }

    private static OptionItemDto getOptionItemDto(BasketProductInfo productInfo, OptionItem optionItem, Product product) {
        OptionItemDto optionItemDto = OptionItemDto.builder()
                .id(optionItem.getId())
                .optionId(optionItem.getOption().getId())
                .name(optionItem.getName())
                .discountPrice(optionItem.getDiscountPrice())
                .amount(productInfo.getAmount())
                .inventoryQuantity(optionItem.getAmount())
                .purchasePrice(optionItem.getPurchasePrice())
                .originPrice(optionItem.getOriginPrice())
                .deliveryFee(product.getDeliverFee())
                .deliverBoxPerAmount(optionItem.getDeliverBoxPerAmount())
                .pointRate(product.getPointRate())
                .minOrderPrice(product.getMinOrderPrice())
                .build();
        return optionItemDto;
    }

    @NotNull
    private static BasketProductInfoDto getBasketProductInfoDto(BasketProductInfo productInfo, Product product, OptionItem optionItem, StoreInfo storeInfo) {
        BasketProductInfoDto basketProductInfoDto = BasketProductInfoDto.builder()
                .id(product.getId())
                .productId(product.getId())
                .state(product.getState())
                .image(product.getImages())
                .title(product.getTitle())
                .isNeedTaxation(product.getNeedTaxation())
                .discountPrice(optionItem.getDiscountPrice())
                .originPrice(optionItem.getOriginPrice())
                .storeId(storeInfo.getStoreId())
                .minOrderPrice(product.getMinOrderPrice())
                .minStorePrice(storeInfo.getMinStorePrice())
                .deliverFeeType(product.getDeliverFeeType())
                .build();
        basketProductInfoDto.convertImageUrlsToFirstUrl();
        return basketProductInfoDto;
    }

    public Optional<BasketProductInfo> findByUserIdAndOptionItemId(Integer userId, Integer optionItemReqId) {
        return basketProductInfoRepository.findByUserIdAndOptionItemId(userId, optionItemReqId);
    }

    public List<BasketProductInfo> findAllByUserIdAndProductId(Integer userId, int productId) {
        return basketProductInfoRepository.findAllByUserIdAndProductId(userId, productId);
    }
}
