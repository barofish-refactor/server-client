package com.matsinger.barofishserver.domain.product.api;

import com.matsinger.barofishserver.domain.address.application.AddressQueryService;
import com.matsinger.barofishserver.domain.address.domain.Address;
import com.matsinger.barofishserver.domain.admin.application.AdminQueryService;
import com.matsinger.barofishserver.domain.admin.domain.Admin;
import com.matsinger.barofishserver.domain.admin.domain.AdminAuthority;
import com.matsinger.barofishserver.domain.admin.log.application.AdminLogCommandService;
import com.matsinger.barofishserver.domain.admin.log.application.AdminLogQueryService;
import com.matsinger.barofishserver.domain.admin.log.domain.AdminLog;
import com.matsinger.barofishserver.domain.admin.log.domain.AdminLogType;
import com.matsinger.barofishserver.domain.category.application.CategoryQueryService;
import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.compare.filter.application.CompareFilterQueryService;
import com.matsinger.barofishserver.domain.data.curation.application.CurationCommandService;
import com.matsinger.barofishserver.domain.product.LikePostType;
import com.matsinger.barofishserver.domain.product.application.ProductQueryService;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.difficultDeliverAddress.application.DifficultDeliverAddressCommandService;
import com.matsinger.barofishserver.domain.product.difficultDeliverAddress.domain.DifficultDeliverAddress;
import com.matsinger.barofishserver.domain.product.domain.*;
import com.matsinger.barofishserver.domain.product.dto.*;
import com.matsinger.barofishserver.domain.product.option.domain.Option;
import com.matsinger.barofishserver.domain.product.option.dto.OptionDto;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.product.productfilter.application.ProductFilterService;
import com.matsinger.barofishserver.domain.product.productfilter.domain.ProductFilterValue;
import com.matsinger.barofishserver.domain.search.application.SearchKeywordQueryService;
import com.matsinger.barofishserver.domain.searchFilter.application.SearchFilterQueryService;
import com.matsinger.barofishserver.domain.searchFilter.domain.ProductSearchFilterMap;
import com.matsinger.barofishserver.domain.store.application.StoreInfoQueryService;
import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.domain.store.domain.Store;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.domain.tastingNote.application.TastingNoteQueryService;
import com.matsinger.barofishserver.domain.tastingNote.basketTastingNote.application.BasketTastingNoteQueryService;
import com.matsinger.barofishserver.domain.tastingNote.dto.ProductTastingNoteResponse;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.jwt.JwtService;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.CustomResponse;
import com.matsinger.barofishserver.utils.S3.Uploader;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/product")
public class ProductController {

    private final ProductService productService;
    private final StoreService storeService;
    private final CategoryQueryService categoryQueryService;
    private final CurationCommandService curationCommandService;
    private final CompareFilterQueryService compareFilterQueryService;
    private final ProductFilterService productFilterService;
    private final SearchFilterQueryService searchFilterQueryService;
    private final ProductQueryService productQueryService;
    private final SearchKeywordQueryService searchKeywordQueryService;
    private final AddressQueryService addressQueryService;
    private final DifficultDeliverAddressCommandService difficultDeliverAddressCommandService;
    private final AdminLogQueryService adminLogQueryService;
    private final AdminLogCommandService adminLogCommandService;
    private final AdminQueryService adminQueryService;
    private final TastingNoteQueryService tastingNoteQueryService;
    private final JwtService jwt;

    private final Common utils;

    private final Uploader s3;
    private final BasketTastingNoteQueryService basketTastingNoteQueryService;
    private final StoreInfoQueryService storeInfoQueryService;

    @GetMapping("/recent-view")
    public ResponseEntity<CustomResponse<List<ProductListDto>>> selectRecentViewList(@RequestParam(value = "ids") String ids) {
        CustomResponse<List<ProductListDto>> res = new CustomResponse<>();

        List<Integer> idList = utils.str2IntList(ids);

        List<ProductListDto> productListDtos = new ArrayList<>();
        for (Integer id : idList) {
            Optional<Product> product = productService.selectOptioanlProduct(id);
            product.ifPresent(value -> {
                if (value.getState().equals(ProductState.ACTIVE))
                    productListDtos.add(productService.convert2ListDto(value));
            });
        }
        res.setData(Optional.of(productListDtos));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/list/ids")
    public ResponseEntity<CustomResponse<List<ProductListDto>>> selectProductListWithIds(@RequestParam(value = "ids") String ids) {
        CustomResponse<List<ProductListDto>> res = new CustomResponse<>();

        List<Integer> idList = utils.str2IntList(ids);
        List<Product> products = productService.selectProductListWithIds(idList);
        res.setData(Optional.of(products.stream().map(productService::convert2ListDto).toList()));

        return ResponseEntity.ok(res);
    }

    @GetMapping("/")
    public ResponseEntity<CustomResponse<Page<SimpleProductDto>>> selectProductList(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                                    @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                                    @RequestParam(value = "take", required = false, defaultValue = "10") Integer take,
                                                                                    @RequestParam(value = "orderby", defaultValue = "createdAt") ProductOrderBy orderBy,
                                                                                    @RequestParam(value = "orderType", defaultValue = "DESC") Sort.Direction sort,
                                                                                    @RequestParam(value = "partnerName", required = false) String partnerName,
                                                                                    @RequestParam(value = "title", required = false) String title,
                                                                                    @RequestParam(value = "state", required = false) String state,
                                                                                    @RequestParam(value = "category", required = false) String category,
                                                                                    @RequestParam(value = "partnerId", required = false) String partnerId,
                                                                                    @RequestParam(value = "categoryId", required = false) String categoryId,
                                                                                    @RequestParam(value = "createdAtS", required = false) Timestamp createdAtS,
                                                                                    @RequestParam(value = "createdAtE", required = false) Timestamp createdAtE) {
        CustomResponse<Page<SimpleProductDto>> res = new CustomResponse<>();

        Integer userId = null;
        TokenInfo tokenInfo = new TokenInfo();
        if (auth.isEmpty()) {
            tokenInfo.setType(null);
            tokenInfo.setId(null);
        }
        if (auth.isPresent()) {
            tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN, TokenAuthType.PARTNER, TokenAuthType.USER, TokenAuthType.ALLOW), auth);
        }

        TokenInfo finalTokenInfo = tokenInfo;
        Specification<Product> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (partnerName != null) predicates.add(builder.like(root.get("store").get("storeInfo").get("name"),
                    "%" + partnerName + "%"));
            if (title != null) predicates.add(builder.like(root.get("title"), "%" + title + "%"));
            if (category != null)
                predicates.add(builder.like(root.get("category").get("name"), "%" + category + "%"));
            if (partnerId != null)
                predicates.add(builder.and(root.get("storeId").in(Arrays.stream(partnerId.split(",")).map(
                        Integer::valueOf).toList())));
            if (state != null)
                predicates.add(builder.and(root.get("state").in(Arrays.stream(state.split(",")).map(ProductState::valueOf).toList())));
            if (categoryId != null)
                predicates.add(builder.and(root.get("category").get("id").in(Arrays.stream(categoryId.split(",")).map(
                        Integer::valueOf).toList())));
            if (createdAtS != null) predicates.add(builder.greaterThan(root.get("createdAt"), createdAtS));
            if (createdAtE != null) predicates.add(builder.lessThan(root.get("createdAt"), createdAtE));
            if (finalTokenInfo.getType().equals(TokenAuthType.PARTNER))
                predicates.add(builder.equal(root.get("storeId"), finalTokenInfo.getId()));
            predicates.add(builder.notEqual(root.get("state"), ProductState.DELETED));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        Pageable pageRequest = PageRequest.of(page, take, Sort.by(sort, orderBy.label));
        Page<Product> products;
        products = productService.selectProductByAdmin(pageRequest, spec);

        Page<SimpleProductDto>
                productDtos =
                products.map(product -> productService.convert2SimpleDto(product, null));

        res.setData(Optional.of(productDtos));
        return ResponseEntity.ok(res);
    }


    @GetMapping("/list/count")
    public ResponseEntity<CustomResponse<Long>> selectProductCountByUser(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                                         @RequestParam(value = "take", defaultValue = "10") Integer take,
                                                                         @RequestParam(value = "sortby", defaultValue = "RECOMMEND", required = false) ProductSortBy sortBy,
                                                                         @RequestParam(value = "categoryIds", required = false) String categoryIds,
                                                                         @RequestParam(value = "filterFieldIds", required = false) String filterFieldIds,
                                                                         @RequestParam(value = "typeIds", required = false) String typeIds,
                                                                         @RequestParam(value = "locationIds", required = false) String locationIds,
                                                                         @RequestParam(value = "processIds", required = false) String processIds,
                                                                         @RequestParam(value = "usageIds", required = false) String usageIds,
                                                                         @RequestParam(value = "storageIds", required = false) String storageIds,
                                                                         @RequestParam(value = "curationId", required = false) Integer curationId,
                                                                         @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                                                         @RequestParam(value = "storeId", required = false) Integer storeId) {
        CustomResponse<Long> res = new CustomResponse<>();

        Page<ProductListDto>
                result =
                productService.selectProductListWithPagination(page - 1,
                        take,
                        sortBy,
                        utils.str2IntList(categoryIds),
                        utils.str2IntList(filterFieldIds),
                        curationId,
                        keyword,
                        storeId,
                        null);
        res.setData(Optional.of(result.getTotalElements()));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/list")
    public ResponseEntity<CustomResponse<Page<ProductListDto>>> selectProductListByUser(@RequestHeader(value = "Authorization", required = false) Optional<String> auth,
                                                                                        @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                                                        @RequestParam(value = "take", defaultValue = "10") Integer take,
                                                                                        @RequestParam(value = "sortby", defaultValue = "RECOMMEND", required = false) ProductSortBy sortBy,
                                                                                        @RequestParam(value = "categoryIds", required = false) String categoryIds,
                                                                                        @RequestParam(value = "filterFieldIds", required = false) String filterFieldIds,
                                                                                        @RequestParam(value = "typeIds", required = false) String typeIds,
                                                                                        @RequestParam(value = "locationIds", required = false) String locationIds,
                                                                                        @RequestParam(value = "processIds", required = false) String processIds,
                                                                                        @RequestParam(value = "usageIds", required = false) String usageIds,
                                                                                        @RequestParam(value = "storageIds", required = false) String storageIds,
                                                                                        @RequestParam(value = "curationId", required = false) Integer curationId,
                                                                                        @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                                                                        @RequestParam(value = "storeId", required = false) Integer storeId) {
        CustomResponse<Page<ProductListDto>> res = new CustomResponse<>();

        Integer userId = null;
        
        jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ALLOW), auth);

        Page<ProductListDto>
                result =
                productService.selectProductListWithPagination(page - 1,
                        take,
                        sortBy,
                        utils.str2IntList(categoryIds),
                        utils.str2IntList(filterFieldIds),
                        curationId,
                        keyword,
                        storeId,
                        userId);
        if (keyword != null && keyword.length() != 0) searchKeywordQueryService.searchKeyword(keyword);
        res.setData(Optional.ofNullable(result));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/excel-list")
    public ResponseEntity<CustomResponse<List<List<ExcelProductDto2>>>> selectProductListForExcel(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                                                  @RequestParam(value = "ids", required = false) String idsStr) {
        CustomResponse<List<List<ExcelProductDto2>>> res = new CustomResponse<>();

        Integer userId = null;
        
        jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        List<Integer> ids = null;
        if (idsStr != null) ids = utils.str2IntList(idsStr);
        List<Product> products = new ArrayList<>();
        if (ids != null) products = productService.selectProductListWithIds(ids);
        else products = productService.selectProductListNotDelete();
        List<List<ExcelProductDto2>>
                productDtos =
                products.stream().map(productService::convert2ExcelProductDto2).toList();
        res.setData(Optional.of(productDtos));
        return ResponseEntity.ok(res);
    }


    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<SimpleProductDto>> selectProduct(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                          @PathVariable("id") Integer id) {
        CustomResponse<SimpleProductDto> res = new CustomResponse<>();

        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(
                Set.of(TokenAuthType.ALLOW, TokenAuthType.USER, TokenAuthType.ADMIN, TokenAuthType.PARTNER),
                auth
        );

        Product product = productService.findById(id);

        TokenAuthType type = null;
        Integer idFromToken = null;
        if (tokenInfo == null) {
            idFromToken = null;
        }
        if (tokenInfo != null && tokenInfo.getType().equals(TokenAuthType.USER)) {
            idFromToken = tokenInfo.getId();
        }
        SimpleProductDto productDto = productService.convert2SimpleDto(
                product,
                idFromToken);

        boolean isSaved = false;
        if (tokenInfo.getType().equals(TokenAuthType.USER)) {
            isSaved = basketTastingNoteQueryService.isSaved(tokenInfo.getId(), id);
        }

        productDto.setIsLike(isSaved);

//        List<ProductTastingNoteResponse> tastingNoteResponses = new ArrayList<>();
//        // 주석 해제시 관리자 페이지에서 상품 비활성화 했을 경우 비활성화된 상품입니다 오류메시지 던지면서 데이터가 안보임
////        if (tokenInfo.getType() == TokenAuthType.USER || tokenInfo.getType() == TokenAuthType.ALLOW) {
//            ProductTastingNoteResponse tastingNoteResponse = tastingNoteQueryService.getTastingNoteInfo(productDto.getId());
//            if (tastingNoteResponse != null) {
//                tastingNoteResponses.add(tastingNoteResponse);
//            }
//            productDto.setTastingNoteInfo(tastingNoteResponses);
//        }

        res.setData(Optional.ofNullable(productDto));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}/option")
    public ResponseEntity<CustomResponse<List<OptionDto>>> selectProductOptionList(@PathVariable("id") Integer id) {
        CustomResponse<List<OptionDto>> res = new CustomResponse<>();

        List<OptionDto> optionDtos = productService.selectProductOption(id);
        res.setData(Optional.ofNullable(optionDtos));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/add")
    public ResponseEntity<CustomResponse<SimpleProductDto>> addProduct(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                       @RequestPart(value = "data") ProductAddReq data,
                                                                       @RequestPart(value = "images") List<MultipartFile> images) throws Exception {
        CustomResponse<SimpleProductDto> res = new CustomResponse<>();

        
        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.PARTNER, TokenAuthType.ADMIN), auth);

        Integer adminId = null;
        if (tokenInfo.getType().equals(TokenAuthType.ADMIN)) adminId = tokenInfo.getId();
        if (tokenInfo.getType().equals(TokenAuthType.ADMIN) && data.getStoreId() == null)
            throw new BusinessException("상점 아이디를 입력해주세요.");
        else if (tokenInfo.getType().equals(TokenAuthType.PARTNER)) data.setStoreId(tokenInfo.getId());
        Optional<Store> store = storeService.selectStoreOptional(data.getStoreId());
        if (store.isEmpty()) throw new BusinessException("가게 정보를 찾을 수 없습니다.");
        Category category = categoryQueryService.findById(data.getCategoryId());
        if (images.size() == 0) throw new BusinessException("이미지를 입력해주세요.");
        for (MultipartFile image : images) {
            if (!s3.validateImageType(image)) throw new BusinessException("허용되지 않는 확장자입니다.");
        }
        if (data.getDescriptionContent() == null) throw new BusinessException("상품 설명을 입력해주세요.");
        String title = utils.validateString(data.getTitle(), 100L, "상품");
        data.getSearchFilterFieldIds().forEach(searchFilterQueryService::selectSearchFilterField);
        String
                deliveryInfo =
                data.getDeliveryInfo() != null ? utils.validateString(data.getDeliveryInfo(), 500L, "배송안내") : null;
        boolean existRepresent = false;
        if (data.getDeliverFeeType() == null) throw new BusinessException("배송비 유형을 입력해주세요.");

        if (data.getDeliverFeeType().equals(ProductDeliverFeeType.FREE)) {
            data.setDeliveryFee(0);
            data.setMinOrderPrice(null);
        }
        if (data.getDeliverFeeType().equals(ProductDeliverFeeType.FREE_IF_OVER)) {
            if (data.getDeliveryFee() == null) throw new BusinessException("배송비를 입력해주세요.");
            if (data.getMinOrderPrice() == null)
                throw new BusinessException("무료 배송 최소 금액을 입력해주세요.");
        }
        if (data.getDeliverFeeType().equals(ProductDeliverFeeType.S_CONDITIONAL)) {
            StoreInfo storeInfo = store.get().getStoreInfo();
            data.setDeliveryFee(storeInfo.getDeliveryFee());
            data.setMinOrderPrice(storeInfo.getMinStorePrice());
        }
        if (data.getDeliveryFee() == null) throw new BusinessException("배송비를 입력해주세요.");

        if (data.getOptions() == null || data.getOptions().size() == 0)
            throw new BusinessException("옵션은 최소 1개 이상 필수입니다.");
        if (data.getOptions().stream().noneMatch(OptionAddReq::getIsNeeded))
            throw new BusinessException("필수 옵션은 최소 1개 이상입니다.");
        for (OptionAddReq optionData : data.getOptions()) {
            if (optionData.getIsNeeded() == null) throw new BusinessException("필수 여부를 체크해주세요.");
            if (optionData.getItems() == null || optionData.getItems().size() == 0)
                throw new BusinessException("옵션 아이템을 입력해주세요.");
            for (OptionItemAddReq itemData : optionData.getItems()) {
                String name = utils.validateString(itemData.getName(), 100L, "옵션 이름");
                itemData.setName(name);
                if (itemData.getIsRepresent() != null && itemData.getIsRepresent()) existRepresent = true;
                if (itemData.getDiscountPrice() == null) itemData.setDiscountPrice(0);
                if (itemData.getOriginPrice() == null) itemData.setOriginPrice(0);
                // throw new BusinessException()("할인(판매) 가격을 입력해주세요);
                if (itemData.getAmount() == null) itemData.setAmount(null);
                // throw new BusinessException()("개수를 입력해주세요);
                if (itemData.getPurchasePrice() == null) itemData.setPurchasePrice(0);
                // throw new BusinessException()("매입가를 입력해주세요);
                if (!optionData.getIsNeeded() && itemData.getOriginPrice() == null) itemData.setOriginPrice(0);
                if (itemData.getDeliveryFee() == null) itemData.setDeliveryFee(0);
                // throw new BusinessException()("배송비를 입력해주세요);
            }
        }
        if (!existRepresent) throw new BusinessException("대표 옵션 아이템을 선택해주세요.");
        List<ProductFilterValue>
                filterValues =
                data.getFilterValues() != null ? data.getFilterValues().stream().map(v -> {
                    try {
                        String valueName = utils.validateString(v.getValue(), 50L, "필터 값");
                        searchFilterQueryService.selectSearchFilterField(v.getCompareFilterId());
                        return ProductFilterValue.builder().compareFilterId(v.getCompareFilterId()).value(valueName).build();
                    } catch (Exception e) {
                        System.out.println(e.getStackTrace());
                        throw new RuntimeException(e);
                    }
                }).toList() : null;
        List<Address> addresses = null;
        if (data.getDifficultDeliverAddressIds() != null) {
            addresses = addressQueryService.selectAddressListWithIds(data.getDifficultDeliverAddressIds());
        }
        // Setter
        Product product = new Product();
        product.setItemCode("19");
        product.setDiscountRate(0);
        product.setTitle(title);
        product.setOriginPrice(0);
        product.setCategory(category);
        product.setStoreId(data.getStoreId());
        product.setExpectedDeliverDay(data.getExpectedDeliverDay() != null ? data.getExpectedDeliverDay() : 0);
        product.setDeliveryInfo(deliveryInfo != null ? deliveryInfo : "");
        product.setImages("");
        product.setDescriptionImages("");
        product.setPointRate(data.getPointRate() != null ? data.getPointRate() : 0.0F);
        product.setState(adminId == null ? ProductState.INACTIVE_PARTNER : ProductState.ACTIVE);
        product.setRepresentOptionItemId(null);
        product.setNeedTaxation(data.getNeedTaxation() != null ? data.getNeedTaxation() : true);
        product.setDeliverBoxPerAmount(data.getDeliverBoxPerAmount());
        product.setPromotionStartAt(data.getPromotionStartAt());
        product.setPromotionEndAt(data.getPromotionEndAt());
        product.setDeliverFee(data.getDeliveryFee());
        product.setDeliverFeeType(data.getDeliverFeeType());
        product.setMinOrderPrice(data.getMinOrderPrice());
        product.setForwardingTime(data.getForwardingTime());
        product.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        Product result = productService.addProduct(product);
        OptionItem representOptionItem = null;
        if (data.getOptions() != null) {
            for (OptionAddReq od : data.getOptions()) {
                Option
                        option =
                        Option.builder().product(result).state(OptionState.ACTIVE).isNeeded(od.getIsNeeded()).description(
                                "").build();
                option = productService.addOption(option);
                for (OptionItemAddReq itemData : od.getItems()) {
                    OptionItem
                            item =
                            OptionItem.builder().option(option).name(itemData.getName()).discountPrice(
                                    itemData.getDiscountPrice()).amount(itemData.getAmount()).purchasePrice(itemData.getPurchasePrice()).originPrice(
                                    itemData.getOriginPrice()).state(OptionItemState.ACTIVE).deliverFee(itemData.getDeliveryFee()).deliverBoxPerAmount(
                                    itemData.getDeliverBoxPerAmount()).maxAvailableAmount(itemData.getMaxAvailableAmount()).build();
                    item = productService.addOptionItem(item);
                    if (itemData.getIsRepresent() != null && itemData.getIsRepresent()) representOptionItem = item;
                }
            }
        }
        if (addresses != null) {
            List<DifficultDeliverAddress> difficultDeliverAddresses = addresses.stream().map(v -> {
                return DifficultDeliverAddress.builder().productId(product.getId()).bcode(v.getBcode()).build();
            }).toList();
            difficultDeliverAddressCommandService.addDifficultDeliverAddressList(difficultDeliverAddresses);
        }
        if (filterValues != null)
            productFilterService.addAllProductFilter(filterValues.stream().peek(v -> v.setProductId(product.getId())).toList());
        List<String>
                imagesUrl =
                s3.uploadFiles(images, new ArrayList<>(Arrays.asList("product", String.valueOf(result.getId()))));
        String
                descriptionContent =
                s3.uploadEditorStringToS3(data.getDescriptionContent(),
                        new ArrayList<>(Arrays.asList("product", String.valueOf(result.getId()))));
        productService.addProductSearchFilters(data.getSearchFilterFieldIds().stream().map(v -> ProductSearchFilterMap.builder().productId(
                product.getId()).fieldId(v).build()).toList());
        result.setImages(imagesUrl.toString());
        result.setDescriptionImages(descriptionContent);
        result.setRepresentOptionItemId(representOptionItem.getId());
        Product finalResult = productService.update(result.getId(), result);
        SimpleProductDto dto = productService.convert2SimpleDto(finalResult, null);
        dto.setReviews(null);
        if (adminId != null) {
            String content = "상품을 등록하였습니다.";
            AdminLog
                    adminLog =
                    AdminLog.builder().id(adminLogQueryService.getAdminLogId()).adminId(adminId).type(AdminLogType.PRODUCT).targetId(
                            String.valueOf(result.getId())).content(content).createdAt(utils.now()).build();
            adminLogCommandService.saveAdminLog(adminLog);
        }
        res.setData(Optional.of(dto));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<CustomResponse<SimpleProductDto>> updateProduct(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                          @PathVariable(value = "id") Integer id,
                                                                          @RequestPart(value = "data") ProductUpdateReq data,
                                                                          @RequestPart(value = "existingImages", required = false) List<String> existingImages,
                                                                          @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages) {
        CustomResponse<SimpleProductDto> res = new CustomResponse<>();

        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN, TokenAuthType.PARTNER), auth);

        Integer adminId = null;
        if (tokenInfo.getType().equals(TokenAuthType.ADMIN)) adminId = tokenInfo.getId();
        if (tokenInfo.getType().equals(TokenAuthType.ADMIN) && data.getStoreId() == null)
            throw new BusinessException("상점 아이디를 입력해주세요.");
        else if (tokenInfo.getType().equals(TokenAuthType.PARTNER)) data.setStoreId(tokenInfo.getId());
        Product product = productService.findById(id);
        if (tokenInfo.getType().equals(TokenAuthType.PARTNER) &&
                product.getStoreId() != tokenInfo.getId())
            throw new BusinessException("타지점의 상품입니다.");

        // 파트너 바꿈
        if (product.getStoreId() != data.getStoreId()) {
            product.setStoreId(data.getStoreId());
        }

        if (data.getCategoryId() != null) {
            Category category = categoryQueryService.findById(data.getCategoryId());
            product.setCategory(category);
        }
        if (newImages != null) {
            for (MultipartFile image : newImages) {
                if (image != null && !s3.validateImageType(image))
                    throw new BusinessException("허용되지 않는 확장자입니다.");
            }
        }
        if (data.getTitle() != null) {
            String title = utils.validateString(data.getTitle(), 100L, "제목");
            product.setTitle(title);
        }
        if (data.getIsActive() != null) {
            product.setState(data.getIsActive() ? ProductState.ACTIVE : ProductState.INACTIVE);
        }
        if (data.getNeedTaxation() != null) {
            product.setNeedTaxation(data.getNeedTaxation());
        }
        if (data.getDeliverBoxPerAmount() != null) {
            product.setDeliverBoxPerAmount(data.getDeliverBoxPerAmount());
        }
        if (data.getPointRate() != null) {
            product.setPointRate(data.getPointRate());
        }
        if (data.getDeliveryInfo() != null) {
            String deliveryInfo = utils.validateString(data.getDeliveryInfo(), 500L, "배송 안내");
            product.setDeliveryInfo(deliveryInfo);
        }
        if (data.getDeliverFeeType() == null) {
            if (data.getDeliveryFee() != null) product.setDeliverFee(data.getDeliveryFee());
            if (data.getMinOrderPrice() != null) product.setMinOrderPrice(data.getMinOrderPrice());
        }
        if (data.getDeliverFeeType().equals(ProductDeliverFeeType.FREE)) {
            product.setDeliverFeeType(ProductDeliverFeeType.FREE);
            product.setDeliverFee(0);
            product.setMinOrderPrice(0);
        }
        if (data.getDeliverFeeType().equals(ProductDeliverFeeType.FIX)) {
            if (product.getDeliverFee() == null && data.getDeliveryFee() == null)
                throw new BusinessException("배송비를 입력해주세요.");
            product.setDeliverFeeType(ProductDeliverFeeType.FIX);
            product.setDeliverFee(data.getDeliveryFee());
            product.setMinOrderPrice(null);
        }
        if (data.getDeliverFeeType().equals(ProductDeliverFeeType.S_CONDITIONAL)) {
            StoreInfo storeInfo = storeInfoQueryService.findByStoreId(product.getStoreId());
            product.setDeliverFeeType(ProductDeliverFeeType.S_CONDITIONAL);
            product.setDeliverFee(storeInfo.getDeliveryFee());
            product.setMinOrderPrice(storeInfo.getMinStorePrice());
        }
        if (data.getDeliverFeeType().equals(ProductDeliverFeeType.FREE_IF_OVER)) {
            if (data.getMinOrderPrice() == null && product.getMinOrderPrice() == null)
                throw new BusinessException("무료 배송 최소 금액을 입력해주세요.");
            if (product.getDeliverFee() == null && data.getDeliveryFee() == null)
                throw new BusinessException("배송비를 입력해주세요.");
            product.setDeliverFeeType(ProductDeliverFeeType.FREE_IF_OVER);
            if (data.getDeliveryFee() != null) product.setDeliverFee(data.getDeliveryFee());
            if (data.getMinOrderPrice() != null) product.setMinOrderPrice(data.getMinOrderPrice());
        }
        if (data.getDeliveryFee() != null) {
            if (data.getDeliveryFee() < 0) throw new BusinessException("배달료를 확인해주세요.");
//                product.setDeliveryFee(data.getDeliveryFee());
        }
        if (data.getExpectedDeliverDay() != null) {
            if (data.getExpectedDeliverDay() < 0) throw new BusinessException("예상 도착일을 입력해주세요.");
            product.setExpectedDeliverDay(data.getExpectedDeliverDay());
        }
        product.setForwardingTime(data.getForwardingTime());
        if (data.getPromotionStartAt() != null) {
            product.setPromotionStartAt(data.getPromotionStartAt());
        }
        if (data.getPromotionEndAt() != null) {
            product.setPromotionEndAt(data.getPromotionEndAt());
        }
        if (data.getSearchFilterFieldIds() != null)
            data.getSearchFilterFieldIds().forEach(searchFilterQueryService::selectSearchFilterField);
        if (data.getDescriptionContent() != null) {
            String
                    url =
                    s3.uploadEditorStringToS3(data.getDescriptionContent(),
                            new ArrayList<>(Arrays.asList("product", id.toString())));
            product.setDescriptionImages(url);
        }
        if (existingImages != null || newImages != null) {
            List<String> imgUrls = existingImages;
            if (newImages != null) existingImages.addAll(s3.uploadFiles(newImages,
                    new ArrayList<>(Arrays.asList("product", String.valueOf(id)))));

            product.setImages(imgUrls.toString());
        }
        List<ProductFilterValue>
                filterValues =
                data.getFilterValues() != null ? data.getFilterValues().stream().map(v -> {
                    try {
                        String valueName = utils.validateString(v.getValue(), 50L, "필터 값");
                        compareFilterQueryService.selectCompareFilter(v.getCompareFilterId());
                        return ProductFilterValue.builder().productId(product.getId()).compareFilterId(v.getCompareFilterId()).value(
                                valueName).build();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toList() : null;
        OptionItem representOptionItem = null;
        if (data.getOptions() != null) {
            for (Common.CudInput<OptionUpdateReq, Integer> optionData : data.getOptions()) {
                if (optionData.getType().equals(Common.CudType.CREATE)) {
                    if (optionData.getData() == null)
                        throw new BusinessException("CREATE의 경우 DATA는 필수 입니다.");
                    if (optionData.getData().getIsNeeded() == null)
                        throw new BusinessException("필수 여부를 입력해주세요.");
                    for (OptionItemUpdateReq itemData : optionData.getData().getItems().stream().map(Common.CudInput::getData).toList()) {
                        String name = utils.validateString(itemData.getName(), 100L, "옵션 이름");
                        itemData.setName(name);
                        if (itemData.getDiscountPrice() == null) itemData.setDiscountPrice(0);
                        // throw new BusinessException()("할인(판매) 가격을 입력해주세요);
                        if (itemData.getAmount() == null)
                            throw new BusinessException("개수를 입력해주세요.");
                        if (itemData.getPurchasePrice() == null) itemData.setPurchasePrice(0);
                        // throw new BusinessException()("매입가를 입력해주세요);
                        if (itemData.getOriginPrice() == null) itemData.setOriginPrice(0);
                        if (itemData.getDeliveryFee() == null) itemData.setDeliveryFee(0);
                    }
                } else if (optionData.getType().equals(Common.CudType.UPDATE)) {
                    if (optionData.getId() == null || optionData.getData() == null)
                        throw new BusinessException("UPDATE인 경우 ID, DATA는 필수 입니다.");
                    Option option = productService.selectOption(optionData.getId());
                    for (Common.CudInput<OptionItemUpdateReq, Integer> itemData : optionData.getData().getItems()) {
                        if (itemData.getType().equals(Common.CudType.CREATE)) {
                            String name = utils.validateString(itemData.getData().getName(), 100L, "옵션 이름");
                            itemData.getData().setName(name);
                            if (itemData.getData().getDiscountPrice() == null)
                                itemData.getData().setDiscountPrice(0);
//                                    throw new BusinessException()("할인(판매) 가격을 입력해주세요);
                            if (itemData.getData().getAmount() == null)
                                throw new BusinessException("개수를 입력해주세요.");
                            if (itemData.getData().getPurchasePrice() == null)
                                throw new BusinessException("매입가를 입력해주세요.");
                            if (itemData.getData().getOriginPrice() == null) itemData.getData().setOriginPrice(0);
                            if (itemData.getData().getDeliveryFee() == null) itemData.getData().setDeliveryFee(0);
                        } else if (itemData.getType().equals(Common.CudType.UPDATE)) {
                            if (itemData.getId() == null)
                                throw new BusinessException("옵션 아이템 UPDATE 시 ID를 필수로 입력해주세요.");
                            OptionItem optionItem = productService.selectOptionItem(itemData.getId());
                        }
                    }
                } else {
                    if (optionData.getId() == null)
                        throw new BusinessException("DELETE인 경우 ID는 필수 입니다.");
                    Option option = productService.selectOption(optionData.getId());
                }
            }
        }
        if (adminId == null) product.setState(ProductState.INACTIVE_PARTNER);
        else product.setState(ProductState.INACTIVE);
        Product result = productService.update(id, product);

        if (data.getOptions() != null) {
            for (Common.CudInput<OptionUpdateReq, Integer> optionData : data.getOptions()) {
                if (optionData.getType().equals(Common.CudType.CREATE)) {
                    Option
                            option =
                            productService.addOption(Option.builder().product(product).description("").state(
                                    OptionState.ACTIVE).isNeeded(optionData.getData().getIsNeeded()).build());
                    for (OptionItemUpdateReq itemData : optionData.getData().getItems().stream().map(Common.CudInput::getData).toList()) {
                        OptionItem
                                optionItem =
                                productService.addOptionItem(OptionItem.builder().option(option).name(
                                        itemData.getName()).discountPrice(itemData.getDiscountPrice()).amount(
                                        itemData.getAmount()).purchasePrice(itemData.getPurchasePrice()).originPrice(
                                        itemData.getOriginPrice()).state(OptionItemState.ACTIVE).deliverFee(itemData.getDeliveryFee()).deliverBoxPerAmount(
                                        itemData.getDeliverBoxPerAmount()).maxAvailableAmount(itemData.getMaxAvailableAmount()).build());
                        if (itemData.getIsRepresent() != null && itemData.getIsRepresent())
                            representOptionItem = optionItem;
                    }
                } else if (optionData.getType().equals(Common.CudType.UPDATE)) {
                    Option option = productService.selectOption(optionData.getId());
                    if (optionData.getData().getIsNeeded() != null)
                        option.setIsNeeded(optionData.getData().getIsNeeded());
                    for (Common.CudInput<OptionItemUpdateReq, Integer> itemData : optionData.getData().getItems()) {
                        OptionItemUpdateReq d = itemData.getData();
                        if (itemData.getType().equals(Common.CudType.CREATE)) {
                            OptionItem
                                    optionItem =
                                    productService.addOptionItem(OptionItem.builder().option(option).name(
                                            d.getName()).discountPrice(d.getDiscountPrice()).state(OptionItemState.ACTIVE).amount(
                                            d.getAmount()).purchasePrice(d.getPurchasePrice()).originPrice(d.getOriginPrice()).deliverFee(
                                            d.getDeliveryFee()).deliverBoxPerAmount(d.getDeliverBoxPerAmount()).maxAvailableAmount(
                                            d.getMaxAvailableAmount()).build());
                            if (d.getIsRepresent() != null && d.getIsRepresent()) representOptionItem = optionItem;
                        } else if (itemData.getType().equals(Common.CudType.UPDATE)) {
                            OptionItem oi = productService.selectOptionItem(itemData.getId());
                            OptionItem
                                    optionItem =
                                    OptionItem.builder().id(itemData.getId()).option(option).name(itemData.getData().getName() !=
                                            null ? itemData.getData().getName() : oi.getName()).discountPrice(
                                            itemData.getData().getDiscountPrice() !=
                                                    null ? itemData.getData().getDiscountPrice() : oi.getDiscountPrice()).amount(
                                            itemData.getData().getAmount() !=
                                                    null ? itemData.getData().getAmount() : oi.getAmount()).state(oi.getState()).purchasePrice(
                                            itemData.getData().getPurchasePrice() !=
                                                    null ? itemData.getData().getPurchasePrice() : oi.getPurchasePrice()).originPrice(
                                            itemData.getData().getOriginPrice() !=
                                                    null ? itemData.getData().getOriginPrice() : oi.getOriginPrice()).deliverFee(
                                            itemData.getData().getDeliveryFee() !=
                                                    null ? itemData.getData().getDeliveryFee() : oi.getDeliverFee()).deliverBoxPerAmount(
                                            itemData.getData().getDeliverBoxPerAmount() !=
                                                    null ? itemData.getData().getDeliverBoxPerAmount() : oi.getDeliverBoxPerAmount()).maxAvailableAmount(
                                            itemData.getData().getMaxAvailableAmount() !=
                                                    null ? itemData.getData().getMaxAvailableAmount() : oi.getMaxAvailableAmount()).build();
                            optionItem = productService.addOptionItem(optionItem);

                            if (d.getIsRepresent() != null && d.getIsRepresent()) representOptionItem = optionItem;
                        } else {
                            productService.deleteOptionItem(itemData.getId());
                        }
                    }
                } else {
                    productService.deleteOption(optionData.getId());
                }
            }
        }

        if (data.getDifficultDeliverAddressIds() != null) {
            List<Address>
                    addresses =
                    addressQueryService.selectAddressListWithIds(data.getDifficultDeliverAddressIds());
            List<DifficultDeliverAddress> difficultDeliverAddresses = addresses.stream().map(v -> {
                return DifficultDeliverAddress.builder().productId(product.getId()).bcode(v.getBcode()).build();
            }).toList();
            difficultDeliverAddressCommandService.deleteDifficultDeliverAddressWithProductId(product.getId());
            difficultDeliverAddressCommandService.addDifficultDeliverAddressList(difficultDeliverAddresses);

        }
        if (data.getSearchFilterFieldIds() != null) {
            productService.deleteProductSearchFilters(product.getId());
            productService.addProductSearchFilters(data.getSearchFilterFieldIds().stream().map(v -> ProductSearchFilterMap.builder().productId(
                    product.getId()).fieldId(v).build()).toList());
        }
        if (representOptionItem != null) {
            product.setRepresentOptionItemId(representOptionItem.getId());
            productService.update(product.getId(), product);
        }
        if (filterValues != null) {
            productFilterService.deleteAllFilterValueWithProductId(product.getId());
            productFilterService.addAllProductFilter(filterValues);
        }
        if (adminId == null) adminId = 1;
        Admin admin = adminQueryService.selectAdmin(adminId);
        String
                content =
                String.format("상품 정보를 수정하였습니다.[%s]",
                        tokenInfo.getType().equals(TokenAuthType.PARTNER) ? "파트너" : admin.getName());
        AdminLog
                adminLog =
                AdminLog.builder().id(adminLogQueryService.getAdminLogId()).adminId(adminId).type(AdminLogType.PRODUCT).targetId(
                        String.valueOf(result.getId())).content(content).createdAt(utils.now()).build();
        if (data.getIsActive() != null) {
            String
                    contentState =
                    String.format("%s -> %s 상태 변경하였습니다.[%s]",
                            data.getIsActive() ? "미노출" : "노출",
                            data.getIsActive() ? "노출" : "미노출",
                            admin.getName());
            AdminLog
                    stateAdminLog =
                    AdminLog.builder().id(adminLogQueryService.getAdminLogId()).adminId(adminId).type(AdminLogType.PRODUCT).targetId(
                            String.valueOf(result.getId())).createdAt(utils.now()).content(contentState).build();
            adminLogCommandService.saveAdminLog(stateAdminLog);
        }
        adminLogCommandService.saveAdminLog(adminLog);
        res.setData(Optional.ofNullable(productService.convert2SimpleDto(result, null)));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/update/state")
    public ResponseEntity<CustomResponse<Boolean>> updateStateProducts(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                       @RequestPart(value = "data") UpdateStateProductsReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();

        
        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Integer adminId = tokenInfo.getId();
        if (data.getProductIds() == null || data.getProductIds().size() == 0)
            throw new BusinessException("상품 아이디를 입력해주세요");
        if (data.getIsActive() == null) throw new BusinessException("노출 여부를 입력해주세요.");
        List<Product> products = productService.selectProductListWithIds(data.getProductIds());
        Admin admin = adminQueryService.selectAdmin(adminId);
        products.forEach(v -> {
            String
                    content =
                    String.format("%s -> %s 상태 변경하였습니다.[%s]",
                            v.getState().equals(ProductState.ACTIVE) ? "노출" : "미노출",
                            data.getIsActive() ? "노출" : "미노출",
                            admin.getAuthority().equals(AdminAuthority.MASTER) ? "관리자" : "서브관리자");
            v.setState(data.getIsActive() ? ProductState.ACTIVE : ProductState.INACTIVE);
            AdminLog
                    adminLog =
                    AdminLog.builder().id(adminLogQueryService.getAdminLogId()).adminId(adminId).type(AdminLogType.PRODUCT).targetId(
                            String.valueOf(v.getId())).createdAt(utils.now()).content(content).build();
            adminLogCommandService.saveAdminLog(adminLog);
        });
        productService.updateProducts(products);
        res.setData(Optional.of(true));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/like")
    public ResponseEntity<CustomResponse<Boolean>> likeProductByUser(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                     @RequestParam(value = "productId") Integer productId,
                                                                     @RequestParam(value = "type") LikePostType type) {
        CustomResponse<Boolean> res = new CustomResponse<>();

        
        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.USER, TokenAuthType.USER), auth);

        Integer check = productService.checkLikeProduct(productId, tokenInfo.getId());
        if (check == 0 && type.equals(LikePostType.LIKE)) {
            productService.likeProduct(productId, tokenInfo.getId());
            res.setData(Optional.of(true));
        } else if (check == 1 && type.equals(LikePostType.UNLIKE)) {
            productService.unlikeProduct(productId, tokenInfo.getId());
            res.setData(Optional.of(true));
        }
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CustomResponse<Boolean>> deleteProduct(@PathVariable("id") Integer id,
                                                                 @RequestHeader(value = "Authorization") Optional<String> auth) {
        CustomResponse<Boolean> res = new CustomResponse<>();

        
        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN, TokenAuthType.PARTNER), auth);


        Integer adminId = null;
        if (tokenInfo.getType().equals(TokenAuthType.ADMIN)) adminId = tokenInfo.getId();
        Product product = productService.findById(id);
        if (tokenInfo.getType().equals(TokenAuthType.PARTNER) &&
                product.getStoreId() != tokenInfo.getId())
            throw new BusinessException("삭제 권한이 없습니다.");
        curationCommandService.deleteWithProductId(product.getId());
        product.setState(ProductState.DELETED);
        product = productService.update(product.getId(), product);
        if (adminId != null) {
            String content = "삭제 처리 되었습니다.";
            AdminLog
                    adminLog =
                    AdminLog.builder().id(adminLogQueryService.getAdminLogId()).adminId(adminId).type(AdminLogType.PRODUCT).targetId(
                            String.valueOf(product.getId())).content(content).createdAt(utils.now()).build();
            adminLogCommandService.saveAdminLog(adminLog);
        }
        res.setData(Optional.of(true));
        return ResponseEntity.ok(res);
    }
}
