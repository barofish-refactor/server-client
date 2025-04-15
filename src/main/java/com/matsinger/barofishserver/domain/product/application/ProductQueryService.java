package com.matsinger.barofishserver.domain.product.application;

import com.matsinger.barofishserver.domain.category.application.CategoryQueryService;
import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.domain.ProductSortBy;
import com.matsinger.barofishserver.domain.product.dto.ExpectedArrivalDateResponse;
import com.matsinger.barofishserver.domain.product.dto.ProductListDto;
import com.matsinger.barofishserver.domain.product.dto.ProductPhotoReviewDto;
import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterProducts;
import com.matsinger.barofishserver.domain.product.infra.redis.ProductFilterSortRedisResolver;
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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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

@Slf4j
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
    private final CategoryQueryService categoryQueryService;
    private final ProductFilterSortRedisResolver productFilterSortRedisResolver;

    public Product findById(int productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("상품 정보를 찾을 수 없습니다."));
    }

    public Page<ProductListDto> getPagedProducts(
            PageRequest pageRequest,
            ProductSortBy sortBy,
            List<Integer> categoryIds,
            List<Integer> filterFieldIds,
            Integer userId) {
        if (categoryIds == null) {
            return null;
        }

        Map<Integer, List<Integer>> filterFieldsMap = createFilterFieldsMap(filterFieldIds);
        Category category = categoryQueryService.findById(categoryIds.get(0));

        long countStart = System.currentTimeMillis();
        List<Integer> filteredSortedProductIds = productFilterSortRedisResolver.getFilteredSortedProductIds(category, sortBy, filterFieldsMap);
        log.info("상품 조회 시간 = {}", System.currentTimeMillis() - countStart);

        List<Integer> slicedProductIds = slice(pageRequest, filteredSortedProductIds);

        List<Product> products = productRepository.findAllById(slicedProductIds);
        List<ProductListDto> productDtos = ProductListDto.listFrom(products);

        List<Integer> userBasketProductIds = new ArrayList<>();
        if (userId != null) {
            User findedUser = userQueryService.findById(userId);
            userBasketProductIds = basketTastingNoteRepository.findAllByUserId(findedUser.getId())
                    .stream().map(v -> v.getProductId()).toList();
        }

        long start4 = System.currentTimeMillis();
        for (ProductListDto productDto : productDtos) {
            if (userBasketProductIds.contains(productDto.getProductId())) {
                productDto.setIsLike(true);
            }
            productDto.convertImageUrlsToFirstUrl();
            productDto.setReviewCount(reviewQueryService.countReviewWithoutDeleted(productDto.getId(), false));
        }
        log.info("dto 변환 시간 = {}", System.currentTimeMillis() - start4);

        return new PageImpl<>(
                productDtos,                  // 페이지 내용 (상품 목록)
                pageRequest,               // 원래의 PageRequest
                filteredSortedProductIds.size()  // 총 요소 수
        );
    }

    @NotNull
    private static List<Integer> slice(PageRequest pageRequest, List<Integer> filteredSortedProductIds) {
        int start = (int) pageRequest.getOffset(); // page * size
        int end = Math.min(start + pageRequest.getPageSize(), filteredSortedProductIds.size());
        List<Integer> paged = filteredSortedProductIds.subList(start, end);
        return paged;
    }

    public Page<ProductListDto> selectTopBarProductList(Integer topBarId,
                                                        PageRequest pageRequest,
                                                        List<Integer> filterFieldsIds) {
        List<Integer> filteredSortedProductIds = null;

        Map<Integer, List<Integer>> filterFieldsMap = createFilterFieldsMap(filterFieldsIds);

        if (topBarId == 1) {
            filteredSortedProductIds = productFilterSortRedisResolver.getFilteredSortedProductIds(null, ProductSortBy.NEW, filterFieldsMap);
        }
        if (topBarId == 2) {
            filteredSortedProductIds = productFilterSortRedisResolver.getFilteredSortedProductIds(null, ProductSortBy.REVIEW, filterFieldsMap);
        }
        if (topBarId == 3) {
            filteredSortedProductIds = productFilterSortRedisResolver.getFilteredSortedProductIds(null, ProductSortBy.DISCOUNT, filterFieldsMap);
        }

        List<Integer> slicedProductIds = slice(pageRequest, filteredSortedProductIds);
        List<Product> products = productRepository.findAllById(slicedProductIds);
        List<ProductListDto> productListDtos = ProductListDto.listFrom(products);

        for (ProductListDto productDto : productListDtos) {
            // 여러개 이미지 중 하나로 세팅
            productDto.convertImageUrlsToFirstUrl();
            productDto.setReviewCount(reviewQueryService.countReviewWithoutDeleted(productDto.getId(), false));
        }

        return new PageImpl<>(productListDtos, pageRequest, filteredSortedProductIds.size());
    }

    public Long countProducts(
            List<Integer> categoryIds,
            List<Integer> filterFieldIds) {

        Map<Integer, List<Integer>> filterFieldsMap = createFilterFieldsMap(filterFieldIds);
        Category category = categoryQueryService.findById(categoryIds.get(0));

        return productFilterSortRedisResolver.getFilteredProductCnt(category, filterFieldsMap);
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

    public Long countTopBarProduct(Integer topBarId,
                                      List<Integer> filterFieldsIds,
                                      List<Integer> categoryIds) {

        Map<Integer, List<Integer>> filterFieldsMap = createFilterFieldsMap(filterFieldsIds);
        Category category = categoryIds == null
                ? null
                : categoryQueryService.findById(categoryIds.get(0));

        return productFilterSortRedisResolver.getFilteredProductCnt(category, filterFieldsMap);
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

    public List<Integer> findProductIdsByCategoryIdOrderByCreatedAtDesc(Integer categoryId) {
        return productRepository.findProductIdsByCategoryIdOrderByCreatedAtDesc(categoryId);
    }

    public List<Integer> findIdsByOrderByCreatedAtDesc() {
        return productRepository.findIdsByOrderByCreatedAtDesc();
    }

    public List<Integer> getProductIdsByCategoryIdsOrderByCreatedAtDesc(List<Integer> categoryIds) {
        return productRepository.getProductIdsByCategoryIdsOrderByCreatedAtDesc(categoryIds);
    }

    public List<Integer> getProductIdsByCategoryIdsOrderByReviewCntDesc(List<Integer> categoryIds) {
        return productRepository.getProductIdsByCategoryIdsOrderByReviewCntDesc(categoryIds);
    }

    public List<Integer> findProductIdsByCategoryIdOrderByReviewCntDesc(Integer categoryId) {
        return productRepository.findProductIdsByCategoryIdOrderByReviewCntDesc(categoryId);
    }

    public List<Integer> findIdsByOrderByReviewCntDesc() {
        return productRepository.findIdsByOrderByReviewCntDesc();
    }

    public List<Integer> getProductIdsByCategoryIdsOrderByDiscountRateDesc(List<Integer> categoryIds) {
        return productRepository.getProductIdsByCategoryIdsOrderByDiscountRateDesc(categoryIds);
    }

    public List<Integer> findProductIdsByCategoryIdOrderByDiscountRateDesc(Integer categoryId) {
        return productRepository.findProductIdsByCategoryIdOrderByDiscountRateDesc(categoryId);
    }

    public List<Integer> findIdsByOrderByDiscountRateDesc() {
        return productRepository.findIdsByOrderByDiscountRateDesc();
    }
}
