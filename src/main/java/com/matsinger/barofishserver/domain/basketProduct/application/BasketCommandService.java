package com.matsinger.barofishserver.domain.basketProduct.application;

import com.matsinger.barofishserver.domain.basketProduct.domain.BasketProductInfo;
import com.matsinger.barofishserver.domain.basketProduct.domain.BasketProductInfos;
import com.matsinger.barofishserver.domain.basketProduct.domain.BasketProductOption;
import com.matsinger.barofishserver.domain.basketProduct.dto.AddBasketOptionReq;
import com.matsinger.barofishserver.domain.basketProduct.dto.AddBasketReq;
import com.matsinger.barofishserver.domain.basketProduct.repository.BasketProductInfoRepository;
import com.matsinger.barofishserver.domain.basketProduct.repository.BasketProductOptionRepository;
import com.matsinger.barofishserver.order.domain.model.Orders;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.domain.product.application.ProductQueryService;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.domain.ProductDeliverFeeType;
import com.matsinger.barofishserver.domain.product.option.application.OptionQueryService;
import com.matsinger.barofishserver.domain.product.option.domain.Option;
import com.matsinger.barofishserver.domain.product.optionitem.application.OptionItemQueryService;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.product.optionitem.repository.OptionItemRepository;
import com.matsinger.barofishserver.domain.store.application.StoreInfoQueryService;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class BasketCommandService {
    private final BasketProductInfoRepository basketProductInfoRepository;
    private final BasketProductOptionRepository basketProductOptionRepository;
    private final ProductService productService;
    private final OptionItemRepository optionItemRepository;
    private final OptionQueryService optionQueryService;
    private final ProductQueryService productQueryService;
    private final BasketQueryService basketQueryService;
    private final OptionItemQueryService optionItemQueryService;
    private final StoreInfoQueryService storeInfoQueryService;

    public void addProductToBasket(Integer userId,
                                   Integer productId,
                                   Integer optionId,
                                   Integer amount,
                                   Integer deliveryFee) {

        BasketProductInfo
                productInfo =
                BasketProductInfo.builder().userId(userId).productId(productId).amount(amount).deliveryFee(deliveryFee).build();
        productInfo = basketProductInfoRepository.save(productInfo);

        if (optionId != null) {
            BasketProductOption
                    option =
                    BasketProductOption.builder().orderProductId(productInfo.getId()).optionId(optionId).build();
            basketProductOptionRepository.save(option);
        }
    }

    public void updateAmountBasket(Integer basketId, Integer amount) {
        BasketProductInfo info = basketProductInfoRepository.findById(basketId).orElseThrow(() -> {
            throw new BusinessException("장바구니 상품 정보를 찾을 수 없습니다.");
        });
        info.setAmount(amount);
        basketProductInfoRepository.save(info);
    }

    @Transactional
    public void deleteBasketV2(Integer userId, List<Integer> basketProductInfoIds) {
        List<BasketProductInfo> allProductsToBeDeleted = basketProductInfoRepository.findAllById(basketProductInfoIds);
        List<BasketProductInfo> allExistingProducts = basketProductInfoRepository.findAllByUserId(userId);

        int[] uniqueProductIdsToBeDeleted = allProductsToBeDeleted.stream()
                .mapToInt(v -> v.getProductId()).distinct().toArray();

        for (int productIdToBeDeleted : uniqueProductIdsToBeDeleted) {
            ArrayList<BasketProductInfo> basketProducts = new ArrayList<>(
                    allExistingProducts.stream()
                    .filter(v -> v.getProductId() == productIdToBeDeleted)
                    .toList()
            );
            List<BasketProductInfo> toBeDeleted =
                    allProductsToBeDeleted.stream()
                    .filter(v -> v.getProductId() == productIdToBeDeleted)
                    .toList();

            if (necessaryOptionDoesntExistsDeleteAll(basketProducts, allExistingProducts, toBeDeleted)) {
                continue;
            }

            remove(basketProducts, toBeDeleted);

            if (!basketProducts.isEmpty()) {
                validateIfNecessaryOptionExists(basketProducts);
            }
        }
        basketProductInfoRepository.deleteAllByIdIn(basketProductInfoIds);
    }

    private static void validateIfNecessaryOptionExists(ArrayList<BasketProductInfo> basketProducts) {
        boolean productNecessaryOptionExists = false;
        for (BasketProductInfo existingProduct : basketProducts) {
            if (productNecessaryOptionExists == false) {
                productNecessaryOptionExists = existingProduct.isNeeded();
            }
            if (productNecessaryOptionExists) {
                break;
            }
        }
        if (!productNecessaryOptionExists) {
            throw new BusinessException("선택옵션을 먼저 제거한 후" + "\n" + "필수옵션을 제거해주세요.");
        }
    }

    private static void remove(ArrayList<BasketProductInfo> basketProducts, List<BasketProductInfo> toBeDeleted) {
        List<BasketProductInfo> tobeDeletedBasketProducts = new ArrayList<>();
        for (BasketProductInfo existingProduct : basketProducts) {
            for (BasketProductInfo toBeDeletedProduct : toBeDeleted) {
                if (existingProduct.equalOptionItem(toBeDeletedProduct.getOptionItemId())) {
                    tobeDeletedBasketProducts.add(existingProduct);
                }
            }
        }
        basketProducts.removeAll(tobeDeletedBasketProducts);
    }

    private boolean necessaryOptionDoesntExistsDeleteAll(ArrayList<BasketProductInfo> basketProducts, List<BasketProductInfo> allExistingProducts, List<BasketProductInfo> toBeDeleted) {
        // 장바구니에 필수옵션이 없으면 모든 옵션아이템을 지운다.
        boolean necessaryOptionDoesntExists = basketProducts.stream()
                .noneMatch(v -> v.isNeeded() == true);
        if (necessaryOptionDoesntExists) {
            allExistingProducts.removeAll(toBeDeleted);
            return true;
        }
        return false;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBasket(List<Integer> basketIds) {
        basketProductOptionRepository.deleteAllByOrderProductIdIn(basketIds);
        basketProductInfoRepository.deleteAllByIdIn(basketIds);
    }

    @Transactional
    public void deleteBasket(Integer productId, Integer userId) {
        List<BasketProductInfo> infos = basketProductInfoRepository.findByUserIdAndProductId(userId, productId);
        List<Integer> basketIds = infos.stream().map(BasketProductInfo::getId).toList();
        basketProductOptionRepository.deleteAllByOrderProductIdIn(basketIds);
        basketProductInfoRepository.deleteAllByIdIn(basketIds);
    }

    public void processBasketProductAdd(Integer userId,
                                        Integer productId,
                                        Integer optionItemId,
                                        Integer amount) {
        List<BasketProductInfo> infos = basketProductInfoRepository.findByUserIdAndProductId(userId, productId);
        boolean isExist = false;
        for (BasketProductInfo info : infos) {
            List<BasketProductOption> optionItems = basketProductOptionRepository.findAllByOrderProductId(info.getId());
            for (BasketProductOption optionItem : optionItems) {
                if (optionItem.getOptionId() == optionItemId) {
                    isExist = true;
                    OptionItem findedOptionItem = productService.selectOptionItem(optionItemId);
                    Option option = optionQueryService.findById(findedOptionItem.getOptionId());

                    info.setAmount(info.getAmount() + amount);
                    basketProductInfoRepository.save(info);
                }
            }
        }
        if (!isExist) {
            OptionItem
                    optionItem =
                    optionItemRepository.findById(optionItemId).orElseThrow(() -> new BusinessException(
                            "옵션 아이템 정보를 찾을 수 없습니다."));
            Product product = productService.selectProduct(productId);
            int deliveryFee = 0;
            if (product.getDeliverFeeType().equals(ProductDeliverFeeType.FREE)) deliveryFee = 0;
            else if (product.getDeliverFeeType().equals(ProductDeliverFeeType.FIX))
                deliveryFee = product.getDeliverFee();
            else if (product.getDeliverFeeType().equals(ProductDeliverFeeType.FREE_IF_OVER))
                deliveryFee =
                        product.getMinOrderPrice() == null ||
                                optionItem.getDiscountPrice() * amount >
                                        product.getMinOrderPrice() ? 0 : (product.getDeliverFee() ==
                                null ? 0 : product.getDeliverFee());

            BasketProductInfo
                    info =
                    BasketProductInfo.builder().userId(userId).productId(productId).amount(amount).deliveryFee(
                            deliveryFee).build();
            info = basketProductInfoRepository.save(info);
            basketProductOptionRepository.save(BasketProductOption.builder().orderProductId(info.getId()).optionId(optionItemId).build());
        }
    }

    @Transactional
    public void deleteBasketAfterOrder(Orders order, List<OrderProductInfo> orderProductInfos) {
        List<Integer> deleteBasketIds = new ArrayList<>();
        orderProductInfos.forEach(v -> {
            List<BasketProductInfo>
                    infos =
                    basketProductInfoRepository.findByUserIdAndProductId(order.getUserId(), v.getProductId());
            for (BasketProductInfo info : infos) {
                List<BasketProductOption> options = basketProductOptionRepository.findAllByOrderProductId(info.getId());
                for (BasketProductOption option : options) {
                    if (option.getOptionId() == v.getOptionItemId()) {
                        deleteBasketIds.add(info.getId());
                    }
                }
            }
        });
        deleteBasket(deleteBasketIds);
    }

    @Transactional
    public void processBasketProductAddV2(AddBasketReq request, Integer userId) {
        Product product = productQueryService.findById(request.getProductId());

        boolean necessaryOptionExists = false;
        for (AddBasketOptionReq optionReq : request.getOptions()) {
            List<BasketProductInfo> exisingBasketProductInfos = basketQueryService.findAllByUserIdAndProductId(userId, product.getId());
            BasketProductInfos basketProductInfos = new BasketProductInfos(exisingBasketProductInfos);

            OptionItem optionItem = optionItemQueryService.findById(optionReq.getOptionId());
            Option option = optionQueryService.findById(optionItem.getOptionId());
            // 장바구니에 같은 상품이 있으면
            if (!basketProductInfos.isEmpty()) {
                // 같은 옵션 아이템에 수량 추가
                BasketProductInfo sameOptionItem = basketProductInfos.getSameOptionItem(optionReq.getOptionId());
                if (sameOptionItem != null) {
                    optionItem.validateQuantity(
                            optionReq.getAmount() + sameOptionItem.getAmount(),
                            product.getTitle());
                    basketProductInfos.addQuantity(optionReq.getOptionId(), optionReq.getAmount());
                }
                // 같은 옵션 아이템이 없으면 새로운 옵션아이템으로 추가
                if (sameOptionItem == null) {
                    optionItem.validateQuantity(
                            optionReq.getAmount(),
                            product.getTitle());
                    addBasketProduct(userId, optionReq, option, optionItem, product);
                }
                necessaryOptionExists = true;
            }

            // 장바구니에 같은 상품이 없으면 새로운 상품으로 추가하면서 필수옵션인지 체크
            if (basketProductInfos.isEmpty()) {
                optionItem.validateQuantity(optionReq.getAmount(), product.getTitle());
                addBasketProduct(userId, optionReq, option, optionItem, product);
                if (!necessaryOptionExists) {
                    necessaryOptionExists = option.getIsNeeded();
                }
            }
        }
        // 새로 추가하려는 상품에 필수옵션이 없으면 예외처리
        if (!necessaryOptionExists) {
            throw new BusinessException("필수 옵션을 선택해주세요.");
        }
    }

    private void addBasketProduct(Integer userId,
                                  AddBasketOptionReq optionReq,
                                  Option option,
                                  OptionItem optionItem,
                                  Product product) {
        basketProductInfoRepository.save(
                BasketProductInfo.builder()
                .userId(userId)
                .storeId(product.getStoreId())
                .productId(product.getId())
                .optionId(option.getId())
                .isNeeded(option.getIsNeeded())
                .optionItemId(optionItem.getId())
                .amount(optionReq.getAmount())
                .deliveryFee(0)
                .build()
        );
    }

    public void addAmount(Integer userId, Integer basketProductInfoId, Integer amount) {
        BasketProductInfo basketProductInfo = basketQueryService.selectBasket(basketProductInfoId);
        if (userId != basketProductInfo.getUserId()) {
            throw new BusinessException("타인의 장바구니 정보입니다.");
        }
        Product product = productQueryService.findById(basketProductInfo.getProductId());
        OptionItem optionItem = optionItemQueryService.findById(basketProductInfo.getOptionItemId());
        optionItem.validateQuantity(amount, product.getTitle());
        basketProductInfo.setAmount(amount);
        basketProductInfoRepository.save(basketProductInfo);
    }
}
