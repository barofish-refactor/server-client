package com.matsinger.barofishserver.domain.product.application;

import com.matsinger.barofishserver.domain.category.application.CategoryQueryService;
import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.domain.ProductSortBy;
import com.matsinger.barofishserver.domain.product.dto.ExpectedArrivalDateResponse;
import com.matsinger.barofishserver.domain.product.dto.ProductListDto;
import com.matsinger.barofishserver.domain.product.dto.ProductPhotoReviewDto;
import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterProducts;
import com.matsinger.barofishserver.domain.product.filter.repository.CategoryFilterProductsQueryRepository;
import com.matsinger.barofishserver.domain.product.repository.ProductQueryRepository;
import com.matsinger.barofishserver.domain.product.repository.ProductRepository;
import com.matsinger.barofishserver.domain.product.weeksdate.domain.WeeksDate;
import com.matsinger.barofishserver.domain.product.weeksdate.repository.WeeksDateRepository;
import com.matsinger.barofishserver.domain.review.application.ReviewQueryService;
import com.matsinger.barofishserver.domain.review.dto.ProductReviewPictureInquiryDto;
import com.matsinger.barofishserver.domain.review.repository.ReviewQueryRepository;
import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilterField;
import com.matsinger.barofishserver.domain.searchFilter.repository.SearchFilterFieldRepository;
import com.matsinger.barofishserver.domain.tastingNote.basketTastingNote.repository.BasketTastingNoteRepository;
import com.matsinger.barofishserver.domain.user.application.UserQueryService;
import com.matsinger.barofishserver.domain.user.domain.User;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import java.util.Arrays;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final WeeksDateRepository weeksDateRepository;
    private final ProductQueryRepository productQueryRepository;
    private final UserQueryService userQueryService;
    private final BasketTastingNoteRepository basketTastingNoteRepository;
    private final ReviewQueryRepository reviewQueryRepository;
    private final ReviewQueryService reviewQueryService;
    private final SearchFilterFieldRepository searchFilterFieldRepository;
    private final CategoryFilterProductsQueryRepository categoryFilterProductsQueryRepository;
    private final CategoryQueryService categoryQueryService;

    public Product findById(int productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("상품 정보를 찾을 수 없습니다."));
    }

    public Page<ProductListDto> getPagedProducts(
            PageRequest pageRequest,
            ProductSortBy sortBy,
            List<Integer> categoryIds,
            List<Integer> filterFieldIds,
            Integer curationId,
            String keyword,
            List<Integer> productIds,
            Integer storeId,
            Integer userId) {

        Map<Integer, List<Integer>> filterFieldsMap = createFilterFieldsMap(filterFieldIds);
        int count = countProducts(categoryIds, filterFieldsMap);

        Page<ProductListDto> pagedProductDtos = productQueryRepository.getProducts(
                pageRequest,
                sortBy,
                categoryIds,
                filterFieldsMap,
                curationId,
                keyword,
                productIds,
                storeId,
                count);

        List<Integer> userBasketProductIds = new ArrayList<>();
        if (userId != null) {
            User findedUser = userQueryService.findById(userId);
            userBasketProductIds = basketTastingNoteRepository.findAllByUserId(findedUser.getId())
                    .stream().map(v -> v.getProductId()).toList();
        }

        for (ProductListDto productDto : pagedProductDtos) {
            if (userBasketProductIds.contains(productDto.getProductId())) {
                productDto.setIsLike(true);
            }
            productDto.convertImageUrlsToFirstUrl();
            productDto.setReviewCount(reviewQueryService.countReviewWithoutDeleted(productDto.getId(), false));
        }

        // productIds의 순서에 따라 제품을 정렬
//        List<ProductListDto> sortedProducts = productIds.stream()
//                .map(id -> pagedProductDtos.stream()
//                        .filter(p -> p.getId().equals(id)).findFirst().orElse(null))
//                .collect(Collectors.toList());
//
//        int offset = (int) pageRequest.getOffset();
//        int pageSize = pageRequest.getPageSize();
//        if (pageSize > sortedProducts.size()) {
//            pageSize = sortedProducts.size();
//        }
//        if (offset > sortedProducts.size()) {
//            offset = sortedProducts.size();
//        }
//        List<ProductListDto> subList = sortedProducts.subList(offset, pageSize);


        return pagedProductDtos;
    }

    public Integer countProducts(List<Integer> categoryIds, Map<Integer, List<Integer>> filterFieldsMap) {
        if (filterFieldsMap == null || filterFieldsMap.isEmpty()) {
            return 0;
        }
        if (categoryIds == null) {
            return 0;
        }

        Category category = categoryQueryService.findById(categoryIds.get(0));
        // 한 번에 모든 필터 캐시 조회
        List<CategoryFilterProducts> caches = categoryFilterProductsQueryRepository.findByFilterIdAndFieldIdsPairs(category, filterFieldsMap);
        
        // 조회 결과가 없으면 0 반환
        if (caches.isEmpty()) {
            return 0;
        }
        
        // productIds의 길이를 기준으로 정렬 (가장 짧은 것부터)
//        caches.sort((a, b) -> {
//            int aLength = a.getProductIds().split(",").length;
//            int bLength = b.getProductIds().split(",").length;
//            return Integer.compare(aLength, bLength);
//        });
        
        // 첫 번째 캐시의 상품 ID 목록을 초기 결과로 사용
        Set<Integer> resultProductIds = Arrays.stream(caches.get(0).getProductIds().split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
        
        // 나머지 캐시들과 AND 연산 수행
        for (int i = 1; i < caches.size(); i++) {
            Set<Integer> currentProductIds = Arrays.stream(caches.get(i).getProductIds().split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
            
            // AND 연산 수행 (교집합)
            resultProductIds.retainAll(currentProductIds);
            
            // 중간 결과가 비어있으면 더 이상 진행할 필요 없음
            if (resultProductIds.isEmpty()) {
                return 0;
            }
        }
        
        return resultProductIds.size();
    }

    public int countProducts(
            List<Integer> categoryIds,
            List<Integer> filterFieldIds,
            Integer curationId,
            String keyword,
            Integer storeId) {

        Map<Integer, List<Integer>> filterFieldsMap = createFilterFieldsMap(filterFieldIds);

        return productQueryRepository.countProducts(
                categoryIds,
                filterFieldsMap,
                curationId,
                keyword,
                null,
                storeId);
    }

    public ExpectedArrivalDateResponse getExpectedArrivalDate(LocalDateTime now, Integer productId) {
        Product findProduct = findById(productId);
        List<WeeksDate> weeksDatesWithHoliday = weeksDateRepository.findByDateBetween(
                DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()),
                DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now().plusWeeks(2))
        );

        int calculateExpectedArrivalDate = findProduct.getExpectedDeliverDay();
        if (findProduct.getExpectedDeliverDay() == 1) {
            calculateExpectedArrivalDate = calculateExpectedArrivalDate(now, Integer.valueOf(findProduct.getForwardingTime()), findProduct.getExpectedDeliverDay(), weeksDatesWithHoliday);
        }

        return ExpectedArrivalDateResponse.builder()
                .productExpectedArrivalDate(findProduct.getExpectedDeliverDay())
                .calculatedExpectedArrivalDate(calculateExpectedArrivalDate)
                .build();
    }

    public int calculateExpectedArrivalDate(LocalDateTime now, Integer productForwardingTime, int productExpectedArrivalDate, List<WeeksDate> weeksDatesWithHoliday) {
        LocalTime localTime = LocalTime.of(productForwardingTime, 0, 0);
        LocalDateTime forwardingTime = LocalDateTime.of(LocalDate.now(), localTime);

        boolean isNowBeforeForwardingTime = now.isBefore(forwardingTime);

        int expectedArrivalDate = productExpectedArrivalDate;

        boolean isTodayHoliday = weeksDatesWithHoliday.get(0).isDeliveryCompanyHoliday();

        // 오늘이 휴일이 아니면
        if (!isTodayHoliday) {
            // 출고시간 전에 주문했고, 다음날이 휴일이 아니면 배송도착기간은 1일
            if (isNowBeforeForwardingTime) {
                if (!weeksDatesWithHoliday.get(1).isDeliveryCompanyHoliday()) {
                    expectedArrivalDate = 1;
                }
            }

            // 출고시간 이후에 주문하면 +2일 기간에 공휴일이 포함돼 있으면 넘김,
            // 2일 동안 공휴일이 포함돼 있지 않으면 배송 출발
            if (!isNowBeforeForwardingTime) {
                expectedArrivalDate = getExpectedArrivalDate(weeksDatesWithHoliday);
            }
        }

        // 오늘이 휴일인 경우 출고시간에 상관 없이 배송도착기간이 계산됨
        if (isTodayHoliday) {
            expectedArrivalDate = getExpectedArrivalDate(weeksDatesWithHoliday);
        }

        return expectedArrivalDate;
    }

    private int getExpectedArrivalDate(List<WeeksDate> weeksDatesWithHoliday) {
        int expectedArrivalDate = 2;

        int seq = 1;

        boolean isTwoConsecutiveDayContainsHoliday = true;
        while (isTwoConsecutiveDayContainsHoliday) {
            boolean isOneDayLatterHoliday = weeksDatesWithHoliday.get(seq).isDeliveryCompanyHoliday();
            boolean isTwoDayLatterHoliday = weeksDatesWithHoliday.get(seq + 1).isDeliveryCompanyHoliday();

            if (!isOneDayLatterHoliday && !isTwoDayLatterHoliday) {
                isTwoConsecutiveDayContainsHoliday = false;
                break;
            }
            seq++;
            expectedArrivalDate++;
        }

        if (expectedArrivalDate >= 10) {
            return -1;
        }
        return expectedArrivalDate;
    }

    public Page<ProductListDto> selectTopBarProductList(Integer topBarId,
                                                        PageRequest pageRequest,
                                                        List<Integer> filterFieldsIds) {
        List<ProductListDto> productDtos = null;

        Map<Integer, List<Integer>> filterFieldsMap = createFilterFieldsMap(filterFieldsIds);
        int count = countProducts(filterFieldsIds, filterFieldsMap);

        if (topBarId == 1) {
            productDtos = productQueryRepository.selectNewerProducts(
                    pageRequest, filterFieldsMap, count);
        }
        if (topBarId == 2) {
            productDtos = productQueryRepository.selectPopularProducts(
                    pageRequest, filterFieldsMap, count);
        }
        if (topBarId == 3) {
            productDtos = productQueryRepository.selectDiscountProducts(
                    pageRequest, filterFieldsMap, count);
        }

//        for (ProductListDto productDto : productDtos) {
//            // 여러개 이미지 중 하나로 세팅
//            productDto.convertImageUrlsToFirstUrl();
//            productDto.setReviewCount(reviewQueryService.countReviewWithoutDeleted(productDto.getId(), false));
//        }

        return new PageImpl<>(productDtos, pageRequest, count);
    }

    private Map<Integer, List<Integer>> createFilterFieldsMap(List<Integer> filterFieldsIds) {
        List<SearchFilterField> searchFilterFields = null;
        if (filterFieldsIds == null) {
            searchFilterFields = searchFilterFieldRepository.findAll();
        } else {
            searchFilterFields = searchFilterFieldRepository.findAllById(filterFieldsIds);
        }

        Map<Integer, List<Integer>> filterAndFieldMapper = new HashMap<>();

        for (SearchFilterField filterField : searchFilterFields) {
            int searchFilterId = filterField.getSearchFilterId();
            List<Integer> existingValue = filterAndFieldMapper.getOrDefault(searchFilterId, new ArrayList<>());
            existingValue.add(filterField.getId());
            filterAndFieldMapper.put(
                    searchFilterId,
                    existingValue
            );
        }

        return filterAndFieldMapper;
    }

    public Integer countTopBarProduct(Integer topBarId,
                                      List<Integer> filterFieldsIds,
                                      List<Integer> categoryIds) {

        Map<Integer, List<Integer>> filterFieldsMap = createFilterFieldsMap(filterFieldsIds);

        if (topBarId == 1) {
            return productQueryRepository.countNewerProducts(categoryIds, filterFieldsMap);
        }
        if (topBarId == 2) {
            return productQueryRepository.countPopularProducts(categoryIds, filterFieldsMap);
        }
        if (topBarId == 3) {
            return productQueryRepository.countDiscountProducts(categoryIds, filterFieldsMap);
        }


        throw new BusinessException("탑바를 찾을 수 없습니다.");
    }

    public List<ProductPhotoReviewDto> getProductPictures(Integer productId) {
        List<ProductReviewPictureInquiryDto> reviews = reviewQueryRepository.getReviewsWhichPictureExists(productId, "product");
        if (reviews.contains(null)) {
            return null;
        }

        List<ProductPhotoReviewDto> response = new ArrayList<>();
        convertStringImageUrlsToList(reviews, response);
        return response;
    }

    private void convertStringImageUrlsToList(List<ProductReviewPictureInquiryDto> reviews,
                                              List<ProductPhotoReviewDto> response) {
        for (ProductReviewPictureInquiryDto review : reviews) {
            String reviewPictureUrls = review.getReviewPictureUrls();

            String removedBrackets = removeBrackets(reviewPictureUrls);
            List<String> reviewImages =
                    removedBrackets != "[]" || removedBrackets != null
                            ? Arrays.stream(removedBrackets.split(", ")).toList()
                            : new ArrayList<>();

            response.add(ProductPhotoReviewDto.builder()
                    .reviewId(review.getReviewId())
                    .imageUrls(reviewImages)
                    .imageCount(reviewImages.isEmpty() ? null : reviewImages.size())
                    .build()
            );
        }
    }

    private String removeBrackets(String reviewPictureUrls) {
        if (reviewPictureUrls == "[]") {
            return reviewPictureUrls;
        }
        if (reviewPictureUrls != null) {
            StringBuilder sb = new StringBuilder();
            for (char c : reviewPictureUrls.toCharArray()) {
                if (c == '[' || c == ']') {
                    continue;
                }
                sb.append(c);
            }
            return sb.toString();
        }
        return null;
    }

    private void validateProductExists(Integer productId) {
        boolean isProductExists = productRepository.existsById(productId);
        if (!isProductExists) {
            throw new BusinessException("상품이 존재하지 않습니다.");
        }
    }

    public List<Product> findAllActiveProductsByStoreId(int storeId) {
        return productQueryRepository.findAllActiveProductsByStoreId(storeId);
    }

    public List<Product> findAllTemporaryInactiveProductsByStoreId(int storeId) {
        return productQueryRepository.findAllTemporaryInactiveProductsByStoreId(storeId);
    }

    public List<Product> findByIds(List<Integer> productIds) {
        return productRepository.findAllById(productIds);
    }
}
