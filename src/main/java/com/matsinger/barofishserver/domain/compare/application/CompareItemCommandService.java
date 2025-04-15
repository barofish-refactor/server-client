package com.matsinger.barofishserver.domain.compare.application;

import com.matsinger.barofishserver.domain.category.filter.repository.CategoryFilterMapRepository;
import com.matsinger.barofishserver.domain.compare.domain.CompareItem;
import com.matsinger.barofishserver.domain.compare.domain.CompareSet;
import com.matsinger.barofishserver.domain.compare.domain.SaveProduct;
import com.matsinger.barofishserver.domain.compare.domain.SaveProductId;
import com.matsinger.barofishserver.domain.compare.dto.CompareProductDto;
import com.matsinger.barofishserver.domain.compare.filter.application.CompareFilterQueryService;
import com.matsinger.barofishserver.domain.compare.filter.dto.CompareFilterDto;
import com.matsinger.barofishserver.domain.compare.repository.CompareItemRepository;
import com.matsinger.barofishserver.domain.compare.repository.CompareSetRepository;
import com.matsinger.barofishserver.domain.compare.repository.SaveProductRepository;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.product.productfilter.application.ProductFilterService;
import com.matsinger.barofishserver.domain.product.productfilter.dto.ProductFilterValueDto;
import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class CompareItemCommandService {
    private final CompareItemRepository compareItemRepository;
    private final CompareSetRepository compareSetRepository;
    private final SaveProductRepository saveProductRepository;
    private final StoreService storeService;
    private final ProductService productService;
    private final ProductFilterService productFilterService;
    private final CategoryFilterMapRepository categoryFilterRepository;
    private final CompareFilterQueryService compareFilterQueryService;

    public CompareSet addCompareSet(Integer userId, List<Integer> productIds) {
        CompareSet compareSet = CompareSet.builder().userId(userId).createdAt(new Timestamp(System.currentTimeMillis()))
                .build();
        compareSetRepository.save(compareSet);
        List<CompareItem> compareItems = new ArrayList<>();
        List<Product> products = new ArrayList<>();
        for (Integer productId : productIds) {
            Product product = productService.findById(productId);
            CompareItem compareItem = CompareItem.builder().compareSetId(compareSet.getId()).productId(product.getId())
                    .build();
            compareItems.add(compareItem);
            compareItemRepository.save(compareItem);
            products.add(product);
        }
        return compareSet;
    }

    @Transactional
    public void deleteCompareSet(Integer id) {
        CompareSet compareSet = compareSetRepository.findById(id).orElseThrow(() -> {
            throw new BusinessException("이미 제거된 비교하기 조합입니다.");
        });
        compareItemRepository.deleteByCompareSetId(compareSet.getId());
        compareSetRepository.deleteById(id);
    }

    public Boolean addSaveProduct(Integer userId, Integer productId) {
        SaveProduct saveProduct = SaveProduct.builder().userId(userId).productId(productId).build();
        saveProductRepository.save(saveProduct);
        return true;
    }

    @Transactional
    public void deleteSaveProduct(List<SaveProduct> saveProducts) {
        saveProductRepository.deleteAll(saveProducts);
    }

    public Boolean checkSaveProduct(Integer userId, Integer productId) {
        return saveProductRepository.existsById(new SaveProductId(userId, productId));
    }

    public CompareProductDto convertProduct2Dto(Product product) {
        StoreInfo storeInfo = storeService.selectStoreInfo(product.getStoreId());
        List<ProductFilterValueDto> filterValues = productFilterService
                .selectProductFilterValueListWithProductId(product.getId());
        List<CompareFilterDto> compareFilterDtos = categoryFilterRepository
                .findAllByCategoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : 0).stream()
                .map(v -> compareFilterQueryService.selectCompareFilter(
                        v.getCompareFilterId()).convert2Dto())
                .toList();
        OptionItem optionItem = productService
                .selectOptionItem(product.getRepresentOptionItemId());
        return CompareProductDto.builder().id(product.getId()).image(product.getImages().substring(1,
                product.getImages().length() -
                        1)
                .split(",")[0]).title(product.getTitle()).originPrice(optionItem.getOriginPrice()).storeName(
                        storeInfo.getName())
                .discountPrice(optionItem.getDiscountPrice()).deliveryFee(product.getDeliverFee()).deliverFeeType(
                        product.getDeliverFeeType())
                .minOrderPrice(product.getMinOrderPrice()).compareFilters(
                        compareFilterDtos)
                .filterValues(filterValues).build();
    }
}
