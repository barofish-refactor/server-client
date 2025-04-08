package com.matsinger.barofishserver.domain.product.application;

import com.matsinger.barofishserver.domain.address.application.AddressQueryService;
import com.matsinger.barofishserver.domain.address.domain.Address;
import com.matsinger.barofishserver.domain.basketProduct.repository.BasketProductOptionRepository;
import com.matsinger.barofishserver.domain.category.dto.CategoryDto;
import com.matsinger.barofishserver.domain.compare.domain.SaveProduct;
import com.matsinger.barofishserver.domain.compare.domain.SaveProductId;
import com.matsinger.barofishserver.domain.compare.filter.domain.CompareFilter;
import com.matsinger.barofishserver.domain.compare.filter.repository.CompareFilterRepository;
import com.matsinger.barofishserver.domain.compare.repository.SaveProductRepository;
import com.matsinger.barofishserver.domain.inquiry.application.InquiryQueryService;
import com.matsinger.barofishserver.domain.inquiry.domain.Inquiry;
import com.matsinger.barofishserver.domain.product.difficultDeliverAddress.application.DifficultDeliverAddressQueryService;
import com.matsinger.barofishserver.domain.product.domain.*;
import com.matsinger.barofishserver.domain.product.dto.*;
import com.matsinger.barofishserver.domain.product.option.domain.Option;
import com.matsinger.barofishserver.domain.product.option.dto.OptionDto;
import com.matsinger.barofishserver.domain.product.option.repository.OptionRepository;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.product.optionitem.dto.OptionItemDto;
import com.matsinger.barofishserver.domain.product.optionitem.repository.OptionItemRepository;
import com.matsinger.barofishserver.domain.product.productfilter.application.ProductFilterService;
import com.matsinger.barofishserver.domain.product.productfilter.domain.ProductFilterValue;
import com.matsinger.barofishserver.domain.product.productfilter.dto.ProductFilterValueDto;
import com.matsinger.barofishserver.domain.product.productfilter.repository.ProductFilterRepository;
import com.matsinger.barofishserver.domain.product.repository.ProductRepository;
import com.matsinger.barofishserver.domain.review.application.ReviewQueryService;
import com.matsinger.barofishserver.domain.review.domain.Review;
import com.matsinger.barofishserver.domain.review.dto.ReviewDto;
import com.matsinger.barofishserver.domain.review.dto.ReviewTotalStatistic;
import com.matsinger.barofishserver.domain.review.repository.ReviewLikeRepository;
import com.matsinger.barofishserver.domain.review.repository.ReviewRepository;
import com.matsinger.barofishserver.domain.searchFilter.application.SearchFilterQueryService;
import com.matsinger.barofishserver.domain.searchFilter.domain.ProductSearchFilterMap;
import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilterField;
import com.matsinger.barofishserver.domain.searchFilter.dto.SearchFilterFieldDto;
import com.matsinger.barofishserver.domain.searchFilter.repository.ProductSearchFilterMapRepository;
import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.domain.store.domain.Store;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.domain.store.repository.StoreInfoRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.utils.Common;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;
    private final OptionItemRepository optionItemRepository;
    private final BasketProductOptionRepository basketProductOptionRepository;
    private final SaveProductRepository saveProductRepository;
    private final ProductSearchFilterMapRepository productSearchFilterMapRepository;

    private final ProductFilterService productFilterService;
    private final StoreService storeService;
    private final InquiryQueryService inquiryQueryService;
    private final ReviewQueryService reviewQueryService;
    private final SearchFilterQueryService searchFilterQueryService;
    private final ReviewRepository reviewRepository;
    private final ProductFilterRepository productFilterRepository;
    private final CompareFilterRepository compareFilterRepository;
    private final StoreInfoRepository storeInfoRepository;
    private final DifficultDeliverAddressQueryService difficultDeliverAddressQueryService;
    private final AddressQueryService addressQueryService;
    private final Common utils;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ProductSummaryService productSummaryService;

    public List<Product> selectProductListWithIds(List<Integer> ids) {
        return productRepository.findAllByIdIn(ids);
    }

    @Transactional
    public void deleteOption(Integer optionId) {
        Option option = selectOption(optionId);
        List<OptionItem> optionItems = optionItemRepository.findAllByOptionIdAndState(optionId, OptionItemState.ACTIVE);
        basketProductOptionRepository.deleteAllByOptionIdIn(optionItems.stream().map(oi -> oi.getOption().getId()).toList());
        optionItems.forEach(v -> {
            v.setState(OptionItemState.DELETED);
        });
        optionItemRepository.saveAll(optionItems);
        option.setState(OptionState.DELETED);
        optionRepository.save(option);
    }

    public void deleteOptionItems(Integer optionId) {
        optionItemRepository.deleteAllByOptionId(optionId);
    }

    @Transactional
    public void deleteOptionItem(Integer optionItemId) {
        basketProductOptionRepository.deleteAllByOptionId(optionItemId);
        OptionItem optionItem = selectOptionItem(optionItemId);
        optionItem.setState(OptionItemState.DELETED);
        optionItemRepository.save(optionItem);
    }

    public OptionItem selectOptionItem(Integer optionItemId) {
        return optionItemRepository.findById(optionItemId).orElseThrow(() -> {
            throw new BusinessException("옵션 아이템 정보를 찾을 수 없습니다.");
        });
    }

    public List<Product> selectProductOtherCustomerBuy(List<Integer> productIds) {
        return productRepository.selectProductOtherCustomerBuy(productIds);
    }

    public Page<ProductListDto> selectProductListWithPagination(Integer page,
                                                                Integer take,
                                                                ProductSortBy sortBy,
                                                                List<Integer> categoryIds,
                                                                List<Integer> filterFieldIds,
                                                                Integer curationId,
                                                                String keyword,
                                                                Integer storeId,
                                                                Integer userId) {
        Page<Product> products;
        List<Integer> filterIds = null;
        if (filterFieldIds != null) {
            List<SearchFilterField>
                    searchFilterFields =
                    searchFilterQueryService.selectSearchFilterListWithIds(filterFieldIds);
            filterIds = searchFilterFields.stream().map(SearchFilterField::getSearchFilterId).toList();
        }
        switch (sortBy) {
            case REVIEW:
                products =
                        productRepository.findWithPaginationSortByReview(Pageable.ofSize(take).withPage(page),
                                categoryIds,
                                filterFieldIds,
                                filterIds,
                                curationId,
                                keyword,
                                storeId);
                break;
            case NEW:
                products =
                        productRepository.findWithPaginationSortByNewer(Pageable.ofSize(take).withPage(page),
                                categoryIds,
                                filterFieldIds,
                                filterIds,
                                curationId,
                                keyword,
                                storeId);
                break;
            case LIKE:
                products =
                        productRepository.findWithPaginationSortByLike(Pageable.ofSize(take).withPage(page),
                                categoryIds,
                                filterFieldIds,
                                filterIds,
                                curationId,
                                keyword,
                                storeId);
                break;
            case SALES:
                products =
                        productRepository.findWithPaginationSortByOrder(Pageable.ofSize(take).withPage(page),
                                categoryIds,
                                filterFieldIds,
                                filterIds,
                                curationId,
                                keyword,
                                storeId);
                break;
            case RECOMMEND:
                products =
                        productRepository.findWithPaginationSortByRecommend(Pageable.ofSize(take).withPage(page),
                                categoryIds,
                                filterFieldIds,
                                filterIds,
                                curationId,
                                keyword,
                                storeId);
                break;
            case LOW_PRICE:
                products =
                        productRepository.findWithPaginationSortByLowPrice(Pageable.ofSize(take).withPage(page),
                                categoryIds,
                                filterFieldIds,
                                filterIds,
                                curationId,
                                keyword,
                                storeId);
                break;
            default:
                products =
                        productRepository.findWithPaginationSortByHighPrice(Pageable.ofSize(take).withPage(page),
                                categoryIds,
                                filterFieldIds,
                                filterIds,
                                curationId,
                                keyword,
                                storeId);
                break;
        }

        return products.map(v -> convert2ListDto(v, userId));
    }

    public Option addOption(Option option) {
        return optionRepository.save(option);
    }

    public OptionItem addOptionItem(OptionItem item) {
        return optionItemRepository.save(item);
    }

    public Page<Product> selectProductByAdmin(Pageable pageRequest, Specification<Product> spec) {
        return productRepository.findAll(spec, pageRequest);
    }

    @NotNull
    private List<ProductFilterValueDto> getProductFilterValueDtos(List<ProductFilterValue> productFilters) {
        List<ProductFilterValueDto> filterValueDtos = new ArrayList<>();
        for (ProductFilterValue productFilterValue : productFilters) {
            CompareFilter
                    findCompareFilter =
                    compareFilterRepository.findById(productFilterValue.getCompareFilterId()).orElseThrow(() -> new Error(
                            "비교하기 항목 정보를 찾을 수 없습니다."));

            ProductFilterValueDto
                    filterValueDto =
                    ProductFilterValueDto.builder().compareFilterId(findCompareFilter.getId()).compareFilterName(
                            findCompareFilter.getName()).value(productFilterValue.getValue()).build();
            filterValueDtos.add(filterValueDto);
        }
        return filterValueDtos;
    }

    public Product selectProduct(Integer id) {
        return productRepository.findById(id).orElseThrow(() -> {
            throw new BusinessException("상품 정보를 찾을 수 없습니다.");
        });
    }

    public Optional<Product> selectOptioanlProduct(Integer id) {
        return productRepository.findById(id);
    }

    public Option selectOption(Integer id) {
        return optionRepository.findById(id).orElseThrow(() -> {
            throw new BusinessException("상품 옵션 정보를 찾을 수 없습니다.");
        });
    }

    public Product addProduct(Product product) {
        return productRepository.save(product);
    }

    public Product update(Integer id, Product data) {
        findById(id);
        return productRepository.save(data);
    }

    public Product findById(Integer id) {
        return productRepository.findById(id).orElseThrow(() -> {
            throw new BusinessException("상품 정보를 찾을 수 없습니다.");
        });
    }

    public List<Product> searchProduct(String keyword) {
        return productRepository.findByTitleContainsAndStateEquals(keyword, ProductState.ACTIVE);
    }

    public List<Product> selectProductListWithStoreIdAndStateActive(Integer storeId) {
        return productRepository.findByStoreIdAndStateEquals(storeId, ProductState.ACTIVE);
    }

    public List<Product> selectComparedProductList(Integer productId) {
        return productRepository.selectComparedProductList(productId);
    }

    public ProductListDto convert2ListDto(Product product, Integer userId) {
        StoreInfo storeInfo = storeService.selectStoreInfo(product.getStoreId());
        Integer reviewCount = reviewQueryService.countReviewWithoutDeleted(product.getId(), false);
        OptionItem optionItem = selectOptionItem(product.getRepresentOptionItemId());
        Boolean
                isLike =
                userId != null
                        ? saveProductRepository.existsById(SaveProductId.builder().userId(userId).productId(product.getId()).build())
                        : null;
        return ProductListDto.builder().id(product.getId()).state(product.getState()).image(product.getImages().substring(
                1,
                product.getImages().length() -
                        1).split(",")[0]).originPrice(optionItem.getOriginPrice()).isNeedTaxation(product.getNeedTaxation()).discountPrice(
                optionItem.getDiscountPrice()).title(product.getTitle()).reviewCount(reviewCount).storeId(storeInfo.getStoreId()).storeName(
                storeInfo.getName()).parentCategoryId(product.getCategory() !=
                null ? product.getCategory().getCategoryId() : null).filterValues(productFilterService.selectProductFilterValueListWithProductId(
                product.getId())).minOrderPrice(product.getMinOrderPrice()).deliverFeeType(product.getDeliverFeeType()).storeImage(
                storeInfo.getProfileImage()).isLike(isLike).build();
    }

    public ProductListDto convert2ListDto(Product product) {
        StoreInfo storeInfo = storeService.selectStoreInfo(product.getStoreId());
        Integer reviewCount = reviewQueryService.countReviewWithoutDeleted(product.getId(), false);
        OptionItem optionItem = selectOptionItem(product.getRepresentOptionItemId());

        List<String> difficultDeliveryBcodes = difficultDeliverAddressQueryService.getDifficultDeliveryBcodes(product.getId());
        return ProductListDto.builder()
                .id(product.getId())
                .state(product.getState())
                .image(product.getImages().substring(1, product.getImages().length() - 1).split(",")[0])
                .originPrice(optionItem.getOriginPrice())
                .isNeedTaxation(product.getNeedTaxation())
                .discountPrice(optionItem.getDiscountPrice())
                .title(product.getTitle())
                .bcodes(difficultDeliveryBcodes)
                .reviewCount(reviewCount)
                .storeId(storeInfo.getStoreId())
                .storeName(storeInfo.getName())
                .parentCategoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .filterValues(productFilterService.selectProductFilterValueListWithProductId(product.getId()))
                .deliverFeeType(product.getDeliverFeeType())
                .minOrderPrice(product.getMinOrderPrice())
                .productDeliveryFee(product.getDeliverFee())
                .isConditional(storeInfo.isConditional())
                .minStorePrice(storeInfo.getMinStorePrice())
                .storeDeliverFee(storeInfo.getDeliveryFee())
                .storeImage(storeInfo.getProfileImage())
                .build();
    }

    public SimpleProductDto convert2SimpleDto(Product product, Integer userId) {
        List<Inquiry> inquiries = inquiryQueryService.selectInquiryListWithProductId(product.getId());
        List<Review>
                reviews =
                reviewQueryService.selectReviewListByProduct(product.getId(), PageRequest.of(0, 50)).getContent();

        StoreInfo store = storeService.selectStoreInfo(product.getStoreId());
        SimpleProductDto productDto = product.convert2SimpleDto();
        List<Product> comparedProducts = selectComparedProductList(product.getId());
        ReviewTotalStatistic
                reviewStatistics =
                reviewQueryService.selectReviewTotalStatisticWithProductId(product.getId());
        productDto.setReviewStatistics(reviewStatistics);
        CategoryDto category = product.getCategory() != null ? product.getCategory().convert2Dto() : null;
        List<SearchFilterFieldDto>
                searchFilterFields =
                productSearchFilterMapRepository.findAllByProductId(product.getId()).stream().map(ProductSearchFilterMap::getFieldId).map(
                        v -> searchFilterQueryService.selectSearchFilterField(v).convert2Dto()).toList();
        List<Address>
                addresses =
                difficultDeliverAddressQueryService.selectDifficultDeliverAddressWithProductId(product.getId()).stream().map(
                        v -> {
                            return addressQueryService.selectAddressWithBcode(v.getBcode());
                        }).toList();
        OptionItem optionItem = selectOptionItem(product.getRepresentOptionItemId());

        List<ReviewDto> reviewDtos = new ArrayList<>();
        for (Review review : reviews) {
            ReviewDto reviewDto = review.convert2Dto();
            if (reviewLikeRepository.findByReviewIdAndUserId(reviewDto.getId(), userId).isEmpty()) {
                reviewDto.setIsLike(false);
                reviewDtos.add(reviewDto);
                break;
            }
            reviewDto.setIsLike(true);
            reviewDtos.add(reviewDto);
        }

        productDto.setIsLike(userId != null &&
                saveProductRepository.existsById(new SaveProductId(userId, product.getId())));
        productDto.setDeliverFeeType(product.getDeliverFeeType());
        productDto.setMinOrderPrice(product.getMinOrderPrice());
        productDto.setDeliveryFee(product.getDeliverFee());
        productDto.setDifficultDeliverAddresses(addresses);
        productDto.setSearchFilterFields(searchFilterFields);
        productDto.setOriginPrice(optionItem.getOriginPrice());
        productDto.setDiscountPrice(optionItem.getDiscountPrice());
        productDto.setCategory(category);
        productDto.setComparedProduct(comparedProducts.stream().map(this::convert2ListDto).toList());
        productDto.setStore(storeService.convert2SimpleDto(store, userId));
        productDto.setInquiries(inquiries.stream().map(Inquiry::convert2Dto).toList());
        productDto.setReviews(reviewDtos);
        productDto.setFilterValues(productFilterService.selectProductFilterValueListWithProductId(product.getId()));
        productDto.setReviewCount(reviews.size());
        productDto.setNeedTaxation(product.getNeedTaxation());
        productDto.setMinStorePrice(store.getMinStorePrice());
        return productDto;

    }

    public List<OptionDto> selectProductOption(Integer productId) {
        List<Option> options = optionRepository.findAllByProductIdAndState(productId, OptionState.ACTIVE);

        return options.stream().map(this::convert2OptionDto).toList();
    }

    public Option selectProductNeededOption(Integer productId) {
        return optionRepository.findFirstByProductIdAndIsNeededTrue(productId);
    }

    public Optional<OptionItem> selectProductWithName(String name, Integer optionId) {
        return optionItemRepository.findFirstByNameAndOptionId(name, optionId);
    }

    @Transactional
    public List<OptionItem> upsertOptionItemList(List<OptionItem> optionItems) {
        return optionItemRepository.saveAll(optionItems);
    }

    public Product upsertProduct(Product product) {
        return productRepository.save(product);
    }

    public Option upsertOption(Option option) {
        return optionRepository.save(option);
    }

    public Page<Product> selectNewerProductList(Integer page,
                                                Integer take,
                                                List<Integer> categoryIds,
                                                List<Integer> filterFieldIds) {
        List<Integer> filterIds = null;
        if (filterFieldIds != null) {
            List<SearchFilterField>
                    searchFilterFields =
                    searchFilterQueryService.selectSearchFilterListWithIds(filterFieldIds);
            filterIds = searchFilterFields.stream().map(SearchFilterField::getSearchFilterId).toList();
        }
        return productRepository.findNewerWithPagination(Pageable.ofSize(take).withPage(page),
                categoryIds,
                filterFieldIds,
                filterIds,
                null);
    }

    public Page<Product> selectDiscountProductList(Integer page,
                                                   Integer take,
                                                   List<Integer> categoryIds,
                                                   List<Integer> filterFieldIds) {
        List<Integer> filterIds = null;
        if (filterFieldIds != null) {
            List<SearchFilterField>
                    searchFilterFields =
                    searchFilterQueryService.selectSearchFilterListWithIds(filterFieldIds);
            filterIds = searchFilterFields.stream().map(SearchFilterField::getSearchFilterId).toList();
        }
        return productRepository.findDiscountWithPagination(Pageable.ofSize(take).withPage(page),
                categoryIds,
                filterFieldIds,
                filterIds,
                null);
    }

    public Page<Product> selectPopularProductList(Integer page,
                                                  Integer take,
                                                  List<Integer> categoryIds,
                                                  List<Integer> filterFieldIds) {
        List<Integer> filterIds = null;
        if (filterFieldIds != null) {
            List<SearchFilterField>
                    searchFilterFields =
                    searchFilterQueryService.selectSearchFilterListWithIds(filterFieldIds);
            filterIds = searchFilterFields.stream().map(SearchFilterField::getSearchFilterId).toList();
        }
        return productRepository.findWithPaginationSortByOrder(Pageable.ofSize(take).withPage(page),
                categoryIds,
                filterFieldIds,
                filterIds,
                null,
                null,
                null);
    }

    public void likeProduct(Integer productId, Integer userId) {
        productRepository.likeProduct(productId, userId);
    }

    public void unlikeProduct(Integer productId, Integer userId) {
        productRepository.unlikeProduct(productId, userId);
    }

    public Integer checkLikeProduct(Integer productId, Integer userId) {
        if (userId == null) return 0;
        return productRepository.checkLikeProduct(productId, userId);
    }

    public void addProductSearchFilters(List<ProductSearchFilterMap> filterMaps) {
        productSearchFilterMapRepository.saveAll(filterMaps);
    }

    public void updateProducts(List<Product> products) {
        productRepository.saveAll(products);
    }

    @Transactional
    public void deleteProductSearchFilters(Integer productId) {
        productSearchFilterMapRepository.deleteAllByProductId(productId);
    }

    public OptionDto convert2OptionDto(Option option) {
        OptionDto optionDto = option.convert2Dto();
        Product product = option.getProduct();
        List<OptionItem>
                optionItems =
                optionItemRepository.findAllByOptionIdAndState(option.getId(), OptionItemState.ACTIVE);
        List<OptionItemDto> itemDtos = optionItems.stream().map(v -> {
            OptionItemDto optionItemDto = v.convert2Dto(product);
            optionItemDto.setDeliverBoxPerAmount(product.getDeliverBoxPerAmount());
            optionItemDto.setPointRate(product.getPointRate());
            return optionItemDto;
        }).toList();
        optionDto.setOptionItems(itemDtos);
        return optionDto;
    }

    public Optional<Product> findOptionalProductWithTitleAndStoreId(String title, Integer storeId) {
        return productRepository.findByTitleAndStoreId(title, storeId);
    }

    public List<Product> selectProductListNotDelete() {
        return productRepository.findAllByStateNot(ProductState.DELETED);
    }

    public ExcelProductDto convert2ExcelProductDto(Product product) {
        Store store = storeService.selectStore(product.getStoreId());
        StoreInfo storeInfo = store.getStoreInfo();

        Option option = optionRepository.findFirstByProductIdAndIsNeededTrue(product.getId());
        List<OptionItem>
                optionItems =
                optionItemRepository.findAllByOptionIdAndState(option.getId(), OptionItemState.ACTIVE);
        Integer representativeOptionNo = 1;
        for (int i = 0; i < optionItems.size(); i++) {
            if (optionItems.get(i).getId() == product.getRepresentOptionItemId()) representativeOptionNo = i + 1;
        }
        return ExcelProductDto.builder().storeLoginId(store.getLoginId()).storeName(storeInfo.getName()).firstCategoryName(
                product.getCategory() !=
                        null ? product.getCategory().getParentCategory().getName() : null).secondCategoryName(product.getCategory() !=
                null ? product.getCategory().getName() : null).productName(product.getTitle()).expectedDeliverDay(
                product.getExpectedDeliverDay()).deliveryInfo(product.getDeliveryInfo()).deliveryFee(product.getDeliverFee()).deliverBoxPerAmount(
                product.getDeliverBoxPerAmount()).isActive(product.getState().equals(ProductState.ACTIVE) ? "노출" : "미노출").needTaxation(
                product.getNeedTaxation() ? "과세" : "비과세").hasOption("있음").purchasePrices(optionItems.stream().map(
                OptionItem::getPurchasePrice).toList()).representativeOptionNo(representativeOptionNo).optionNames(
                optionItems.stream().map(OptionItem::getName).toList()).optionOriginPrices(optionItems.stream().map(
                OptionItem::getOriginPrice).toList()).optionDiscountPrices(optionItems.stream().map(OptionItem::getDiscountPrice).toList()).optionMaxOrderAmount(
                optionItems.stream().map(OptionItem::getMaxAvailableAmount).toList()).optionAmounts(optionItems.stream().map(
                OptionItem::getAmount).toList()).pointRate(product.getPointRate()).build();
    }

    public List<ExcelProductDto2> convert2ExcelProductDto2(Product product) {
        Store store = storeService.selectStore(product.getStoreId());
        StoreInfo storeInfo = store.getStoreInfo();

        Option option = optionRepository.findFirstByProductIdAndIsNeededTrue(product.getId());
        List<OptionItem>
                optionItems =
                optionItemRepository.findAllByOptionIdAndState(option.getId(), OptionItemState.ACTIVE);
        Integer representativeOptionNo = 1;
        for (int i = 0; i < optionItems.size(); i++) {
            if (optionItems.get(i).getId() == product.getRepresentOptionItemId()) representativeOptionNo = i + 1;
        }

        List<ExcelProductDto2> excelProductContents = new ArrayList<>();
        for (OptionItem optionItem : optionItems) {
            ExcelProductDto2
                    excelProductDto =
                    ExcelProductDto2.builder().storeLoginId(store.getLoginId()).storeName(storeInfo.getName()).firstCategoryName(
                            product.getCategory() !=
                                    null ? product.getCategory().getParentCategory().getName() : null).secondCategoryName(
                            product.getCategory() !=
                                    null ? product.getCategory().getName() : null).productName(product.getTitle()).expectedDeliverDay(
                            product.getExpectedDeliverDay()).deliveryInfo(product.getDeliveryInfo()).deliveryFee(product.getDeliverFee()).deliverBoxPerAmount(
                            product.getDeliverBoxPerAmount()).isActive(product.getState().equals(ProductState.ACTIVE) ? "노출" : "미노출").needTaxation(
                            product.getNeedTaxation() ? "과세" : "비과세").hasOption("있음").purchasePrices(optionItem.getPurchasePrice()).representativeOptionNo(
                            representativeOptionNo).optionName(optionItem.getName()).optionOriginPrice(optionItem.getOriginPrice()).optionDiscountPrice(
                            optionItem.getDiscountPrice()).optionMaxOrderAmount(optionItem.getMaxAvailableAmount()).optionAmount(
                            optionItem.getAmount()).pointRate(product.getPointRate()).build();
            excelProductContents.add(excelProductDto);
        }
        return excelProductContents;
    }

    public List<Product> selectProductWithCategoryId(Integer categoryId) {
        return productRepository.findAllByCategory_Id(categoryId);
    }

    public void saveAllProduct(List<Product> products) {
        productRepository.saveAll(products);
    }

    public void updatePassedPromotionProductInactive() {
        List<Product> products = productRepository.findAllByPromotionEndAtBefore(utils.now());
        products.forEach(v -> v.setState(ProductState.INACTIVE));
        productRepository.saveAll(products);
    }

    public void updateProductStateActiveSupposedToStartPromotion() {
        List<Product> products = productRepository.findAllByPromotionStartAtBeforeAndPromotionEndAtAfter(utils.now(), utils.now());
        products.forEach(v -> v.setState(ProductState.ACTIVE));
        productRepository.saveAll(products);
    }

    public Map<Integer, ProductListDto> convertProductsToListDtosMap(List<Product> products, Integer userId) {
        if (products.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, ProductSummary> productSummaryMap = collectReviewCounts(products);
        Map<Integer, Boolean> likeMap = collectLikeInfo(products, userId);

        return products.stream().collect(Collectors.toMap(
            Product::getId,
            product -> buildProductListDto(
                product,
                productSummaryMap.get(product.getId()),
                likeMap.getOrDefault(product.getId(), false)
            )
        ));
    }

    private Map<Integer, ProductSummary> collectReviewCounts(List<Product> products) {
        List<Integer> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        return productSummaryService.countReviewsWithoutDeletedByProductIds(productIds);
    }

    private Map<Integer, Boolean> collectLikeInfo(List<Product> products, Integer userId) {
        Map<Integer, Boolean> likeMap = new HashMap<>();
        
        if (userId != null) {
            List<Integer> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
            List<SaveProduct> savedProducts = saveProductRepository.findAllByUserIdAndProductIdIn(userId, productIds);
            for (SaveProduct savedProduct : savedProducts) {
                likeMap.put(savedProduct.getProductId(), true);
            }
        }
        return likeMap;
    }

    private ProductListDto buildProductListDto(
            Product product,
            ProductSummary productSummary,
            Boolean isLike) {

        Integer reviewCount = productSummary != null ? productSummary.getReviewCnt() : 0;

        return ProductListDto.builder()
                .id(product.getId())
                .state(product.getState())
                .image(product.getFirstImageUrl())
                .originPrice(product.getRepresentOptionItemOriginPrice())
                .isNeedTaxation(product.getNeedTaxation())
                .discountPrice(product.getRepresentOptionItemDiscountPrice())
                .title(product.getTitle())
                .reviewCount(reviewCount)
                .storeId(product.getStoreId())
                .storeName(product.getStoreName())
                .parentCategoryId(product.getCategoryId())
                .filterValues(ProductFilterValueDto.listFrom(product.getFilterValues()))
                .minOrderPrice(product.getMinOrderPrice())
                .deliverFeeType(product.getDeliverFeeType())
                .storeImage(product.getStoreImageUrl())
                .isLike(isLike)
                .build();
    }

    public List<Integer> findIdsByCategoryIdsInAndFieldId(List<Integer> categoryIds, Integer fieldId) {
        return productRepository.findIdsByCategoryIdsInAndFieldId(categoryIds, fieldId);
    }
}
