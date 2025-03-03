package com.matsinger.barofishserver.domain.product.api;

import com.matsinger.barofishserver.domain.address.application.AddressQueryService;
import com.matsinger.barofishserver.domain.admin.application.AdminQueryService;
import com.matsinger.barofishserver.domain.admin.log.application.AdminLogCommandService;
import com.matsinger.barofishserver.domain.admin.log.application.AdminLogQueryService;
import com.matsinger.barofishserver.domain.category.application.CategoryQueryService;
import com.matsinger.barofishserver.domain.compare.filter.application.CompareFilterQueryService;
import com.matsinger.barofishserver.domain.data.curation.application.CurationCommandService;
import com.matsinger.barofishserver.domain.product.application.ProductQueryService;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.difficultDeliverAddress.application.DifficultDeliverAddressCommandService;
import com.matsinger.barofishserver.domain.product.domain.ProductSortBy;
import com.matsinger.barofishserver.domain.product.dto.ExpectedArrivalDateResponse;
import com.matsinger.barofishserver.domain.product.dto.ProductListDto;
import com.matsinger.barofishserver.domain.product.dto.ProductPhotoReviewDto;
import com.matsinger.barofishserver.domain.product.productfilter.application.ProductFilterService;
import com.matsinger.barofishserver.domain.search.application.SearchKeywordQueryService;
import com.matsinger.barofishserver.domain.searchFilter.application.SearchFilterQueryService;
import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.jwt.JwtService;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.CustomResponse;
import com.matsinger.barofishserver.utils.S3.S3Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/product")
public class ProductControllerV2 {

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
    private final JwtService jwt;

    private final Common utils;

    private final S3Uploader s3;

    @GetMapping("/list")
    public ResponseEntity<CustomResponse<Page<ProductListDto>>> selectProductListByUserV2(@RequestHeader(value = "Authorization", required = false) Optional<String> auth,
                                                                                          @RequestParam(value = "page", defaultValue = "1") Integer page,
                                                                                          @RequestParam(value = "take", defaultValue = "10") Integer take,
                                                                                          @RequestParam(value = "sortby", defaultValue = "RECOMMEND", required = false) ProductSortBy sortBy,
                                                                                          @RequestParam(value = "categoryIds", required = false) String categoryIds,
                                                                                          @RequestParam(value = "filterFieldIds", required = false) String filterFieldIds,
                                                                                          @RequestParam(value = "curationId", required = false) Integer curationId,
                                                                                          @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                                                                          @RequestParam(value = "productIds", required = false) String productIds,
                                                                                          @RequestParam(value = "storeId", required = false) Integer storeId) {

        CustomResponse<Page<ProductListDto>> res = new CustomResponse<>();
        
        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ALLOW, TokenAuthType.USER), auth);

        Integer userId = tokenInfo != null ? tokenInfo.getId() : null;

        List<Integer> integerProductIds = productIds != null
                ? Arrays.stream(productIds.split(",")).map(v -> Integer.valueOf(v)).toList()
                : null;

        PageRequest pageRequest = PageRequest.of(page - 1, take);
        Page<ProductListDto> result = productQueryService.getPagedProducts(
                pageRequest,
                sortBy,
                utils.str2IntList(categoryIds),
                utils.str2IntList(filterFieldIds),
                curationId,
                keyword,
                integerProductIds,
                storeId,
                userId);

        res.setData(Optional.ofNullable(result));
        return ResponseEntity.ok(res);
    }

//    @GetMapping("/list/count")
//    public ResponseEntity<CustomResponse<Integer>> selectProductCountByUserV2(@RequestHeader(value = "Authorization", required = false) Optional<String> auth,
//                                                                                        @RequestParam(value = "categoryIds", required = false) String categoryIds,
//                                                                                        @RequestParam(value = "filterFieldIds", required = false) String filterFieldIds,
//                                                                                        @RequestParam(value = "curationId", required = false) Integer curationId,
//                                                                                        @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
//                                                                                        @RequestParam(value = "storeId", required = false) Integer storeId) {
//        CustomResponse<Integer> response = new CustomResponse<>();
//        jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ALLOW), auth);
//
//        int count = productQueryService.countProducts(
//                utils.str2IntList(categoryIds),
//                utils.str2IntList(filterFieldIds),
//                curationId,
//                keyword,
//                productIds,
//                storeId);
//        response.setIsSuccess(true);
//        response.setData(Optional.of(count));
//
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/arrival-date/{id}")
    public ResponseEntity<CustomResponse<Object>> getExpectedArrivalDate(@PathVariable(value = "id") Integer productId,
                                                                         @RequestParam(value = "Authorization") Optional<String> auth) {

        CustomResponse<Object> res = new CustomResponse<>();

        LocalDateTime now = LocalDateTime.now();

        ExpectedArrivalDateResponse expectedArrivalDate = productQueryService.getExpectedArrivalDate(now, productId);

        res.setData(Optional.of(expectedArrivalDate));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}/review-pictures")
    public ResponseEntity<CustomResponse<List<ProductPhotoReviewDto>>> getProductReviewPhotos(@PathVariable(value = "id") Integer productId) {
        CustomResponse<List<ProductPhotoReviewDto>> response = new CustomResponse<>();

        List<ProductPhotoReviewDto> productPhotoReviewDtos = productQueryService.getProductPictures(productId);
        response.setIsSuccess(true);
        response.setData(Optional.of(productPhotoReviewDtos));

        return ResponseEntity.ok(response);
    }
}
