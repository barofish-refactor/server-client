package com.matsinger.barofishserver.domain.review.api;

import com.matsinger.barofishserver.domain.product.dto.ProductPhotoReviewDto;
import com.matsinger.barofishserver.domain.review.application.ReviewCommandService;
import com.matsinger.barofishserver.domain.review.application.ReviewQueryService;
import com.matsinger.barofishserver.domain.review.domain.Review;
import com.matsinger.barofishserver.domain.review.domain.ReviewOrderBy;
import com.matsinger.barofishserver.domain.review.domain.ReviewOrderByType;
import com.matsinger.barofishserver.domain.review.dto.UpdateReviewReq;
import com.matsinger.barofishserver.domain.review.dto.v2.AdminReviewDto;
import com.matsinger.barofishserver.domain.review.dto.v2.ProductReviewDto;
import com.matsinger.barofishserver.domain.review.dto.v2.StoreReviewDto;
import com.matsinger.barofishserver.domain.review.dto.v2.UserReviewDto;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.jwt.JwtService;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.utils.CustomResponse;
import com.matsinger.barofishserver.utils.S3.S3Uploader;
import com.matsinger.barofishserver.utils.S3.Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/review")
public class ReviewControllerV2 {

    private final JwtService jwt;
    private final ReviewQueryService reviewQueryService;
    private final ReviewCommandService reviewCommandService;
    private final Uploader s3;

    @GetMapping("/management")
    public ResponseEntity<CustomResponse<Page<AdminReviewDto>>> selectAllReviewListByAdminV2(
            @RequestHeader(value = "Authorization") Optional<String> auth,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "take", required = false, defaultValue = "10") Integer take,
            @RequestParam(value = "orderby", defaultValue = "createdAt") ReviewOrderBy orderBy,
            @RequestParam(value = "orderType", defaultValue = "DESC") Sort.Direction sort,
            @RequestParam(value = "orderNo", required = false) String orderId,
            @RequestParam(value = "productName", required = false) String productName,
            @RequestParam(value = "partnerName", required = false) String partnerName,
            @RequestParam(value = "reviewer", required = false) String reviewer,
            @RequestParam(value = "evaluation", required = false) String evaluation,
            @RequestParam(value = "createdAtS", required = false) Timestamp createdAtS,
            @RequestParam(value = "createdAtE", required = false) Timestamp createdAtE
    ) {
        CustomResponse<Page<AdminReviewDto>> res = new CustomResponse<>();

                TokenInfo tokenInfo = null;
        if (auth.isPresent()) {
            tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN, TokenAuthType.PARTNER), auth);
        }

        PageRequest pageRequest = PageRequest.of(page, take);
        Integer storeId = null;
        if (tokenInfo.getType().equals(TokenAuthType.PARTNER)) {
            storeId = tokenInfo.getId();
        }

        Page<AdminReviewDto> pagedReviewDtos = reviewQueryService.findAllReviewExceptDeleted(orderBy, sort, orderId, productName, partnerName, reviewer, evaluation, createdAtS, createdAtE, storeId, pageRequest);
        res.setData(Optional.of(pagedReviewDtos));

        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}")
    public ResponseEntity<CustomResponse<Boolean>> deleteReview(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                @PathVariable("id") Integer reviewId) {
        CustomResponse<Boolean> res = new CustomResponse<>();

        TokenInfo tokenInfo = null;
        if (auth.isPresent()) {
            tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.USER, TokenAuthType.ADMIN, TokenAuthType.PARTNER), auth);
        }

        Review review = reviewQueryService.selectReview(reviewId);
        if (tokenInfo.getType().equals(TokenAuthType.USER) && review.getUserId() != tokenInfo.getId())
            throw new BusinessException("타인의 리뷰는 삭제할 수 없습니다.");
        else if (tokenInfo.getType().equals(TokenAuthType.PARTNER) && review.getStore().getId() != tokenInfo.getId())
            throw new BusinessException("타 상점의 리뷰입니다.");

        review.setIsDeleted(true);
        res.setData(Optional.ofNullable(true));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<CustomResponse<ProductReviewDto>> getReviews(@PathVariable("id") Integer productId,
                                                                       @RequestHeader(value = "Authorization") Optional<String> auth,
                                                                       @RequestParam(value = "orderType", required = false, defaultValue = "RECENT") ReviewOrderByType orderType,
                                                                       @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                       @RequestParam(value = "take", required = false, defaultValue = "10") Integer take) {

        Integer userId = null;
        if (auth.isEmpty()) {
            userId = null;
        }
        if (auth.isPresent()) {
            TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ALLOW, TokenAuthType.USER), auth);
            userId = tokenInfo.getId();
        }

        PageRequest pageRequest = PageRequest.of(page-1, take);
        CustomResponse<ProductReviewDto> res = new CustomResponse<>();

        ProductReviewDto pagedProductReviewInfo = reviewQueryService.getPagedProductReviewInfo(productId, userId, orderType, pageRequest);

        res.setData(Optional.of(pagedProductReviewInfo));
        return ResponseEntity.ok(res);
    }

    @GetMapping(value = {"/store/{id}", "/store"})
    public ResponseEntity<CustomResponse<StoreReviewDto>> selectReviewListWithStoreIdV2(
            @RequestHeader(value = "Authorization") Optional<String> auth,
            @PathVariable(value = "id", required = false) Integer storeId,
            @RequestParam(value = "orderType", required = false, defaultValue = "RECENT") ReviewOrderByType orderType,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "take", required = false, defaultValue = "10") Integer take) {

        CustomResponse<StoreReviewDto> res = new CustomResponse<>();

        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ALLOW, TokenAuthType.USER), auth);

        Integer userId = tokenInfo.getId();
        if (tokenInfo.getType().equals(TokenAuthType.USER)) {
            userId = tokenInfo.getId();
        }

        PageRequest pageRequest = PageRequest.of(page-1, take);

        StoreReviewDto pagedStoreReviewDto = reviewQueryService.getPagedProductSumStoreReviewInfo(storeId, userId, orderType, pageRequest);
        res.setData(Optional.of(pagedStoreReviewDto));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}/pictures")
    public ResponseEntity<CustomResponse<List<ProductPhotoReviewDto>>> getProductReviewPhotos(@PathVariable(value = "id") Integer id,
                                                                                              @RequestParam(value = "type") String type) {
        CustomResponse<List<ProductPhotoReviewDto>> response = new CustomResponse<>();

        List<ProductPhotoReviewDto> productPhotoReviewDtos = reviewQueryService.getPictures(id, type);
        response.setIsSuccess(true);
        response.setData(Optional.of(productPhotoReviewDtos));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<CustomResponse<UserReviewDto>> selectMyReviewListV2(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                              @RequestParam(value = "orderType", required = false, defaultValue = "RECENT") ReviewOrderByType orderType,
                                                                              @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                              @RequestParam(value = "take", required = false, defaultValue = "10") Integer take) {
        CustomResponse<UserReviewDto> res = new CustomResponse<>();

        Integer userId = null;
                if (auth.isPresent()) {
            TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.USER), auth);
            userId = tokenInfo.getId();
        }

        PageRequest pageRequest = PageRequest.of(page-1, take);

        UserReviewDto pagedUserReview = reviewQueryService.getPagedUserReview(userId, orderType, pageRequest);
        res.setData(Optional.of(pagedUserReview));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<CustomResponse<Object>> updateReviewV2(
            @RequestHeader(value = "Authorization") Optional<String> auth,
            @PathVariable("id") Integer id,
            @RequestPart(value = "data") UpdateReviewReq data,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        CustomResponse<Object> res = new CustomResponse<>();

        Integer userId = null;
                if (auth.isPresent()) {
            TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.USER), auth);
            userId = tokenInfo.getId();
        }

        Integer updatedReviewId = reviewCommandService.update(userId, id, data, images);

        res.setData(Optional.ofNullable(updatedReviewId));
        return ResponseEntity.ok(res);
    }
}
