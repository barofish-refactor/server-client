package com.matsinger.barofishserver.banner;

import com.matsinger.barofishserver.category.CategoryService;
import com.matsinger.barofishserver.data.curation.CurationService;
import com.matsinger.barofishserver.jwt.JwtService;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.CustomResponse;
import com.matsinger.barofishserver.utils.RegexConstructor;
import com.matsinger.barofishserver.utils.S3.S3Uploader;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/banner")
public class BannerController {
    private final BannerService bannerService;
    private final BannerRepository bannerRepository;
    private final Common util;

    private final CategoryService categoryService;
    private final CurationService curationService;
    private final S3Uploader s3;
    private final RegexConstructor re;
    private final JwtService jwt;


    @GetMapping("/")
    public ResponseEntity<CustomResponse<List<BannerDto>>> selectBannerList() {
        CustomResponse<List<BannerDto>> res = new CustomResponse<>();
        try {
            List<BannerDto> banners = bannerService.selectBannerList().stream().map(Banner::convert2Dto).toList();
            res.setData(Optional.of(banners));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @Getter
    @NoArgsConstructor
    private static class SortBannerReq {
        List<Integer> bannerIds;
    }

    @PostMapping("/sort-banner")
    public ResponseEntity<CustomResponse<List<BannerDto>>> sortCuration(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                        @RequestPart(value = "data") SortBannerReq data) {
        CustomResponse<List<BannerDto>> res = new CustomResponse<>();
        Optional<TokenInfo> tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);
        if (tokenInfo == null) return res.throwError("인증이 필요합니다.", "FORBIDDEN");
        try {
            List<BannerDto> bannerDtos = new ArrayList<>();
            List<Banner> banners = new ArrayList<>();
            for (int i = 0; i < data.bannerIds.size(); i++) {
                Banner banner = bannerService.selectBanner(data.bannerIds.get(i));
                banner.setSortNo(i + 1);
                banners.add(banner);
                bannerDtos.add(banner.convert2Dto());
            }
            bannerService.updateAllBanners(banners);
            res.setData(Optional.of(bannerDtos));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @GetMapping("/management")
    public ResponseEntity<CustomResponse<Page<BannerDto>>> selectBannerListByAdmin(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                                   @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                                   @RequestParam(value = "take", required = false, defaultValue = "10") Integer take,
                                                                                   @RequestParam(value = "types", required = false) String types,
                                                                                   @RequestParam(value = "orderby", defaultValue = "id") BannerOrderBy orderBy,
                                                                                   @RequestParam(value = "orderType", defaultValue = "DESC") Sort.Direction sort) {
        CustomResponse<Page<BannerDto>> res = new CustomResponse<>();
        Optional<TokenInfo> tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);
        if (tokenInfo == null) return res.throwError("인증이 필요합니다.", "FORBIDDEN");
        try {
            PageRequest pageRequest = PageRequest.of(page, take, Sort.by(sort, orderBy.label));
            Specification<Banner> spec = (root, query, builder) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (types != null)
                    predicates.add(builder.and(root.get("type").in(Arrays.stream(types.split(",")).map(BannerType::valueOf).toList())));
                return builder.and(predicates.toArray(new Predicate[0]));
            };
            Page<BannerDto> banners = bannerService.selectBannerListByAdmin(pageRequest, spec).map(Banner::convert2Dto);
            res.setData(Optional.of(banners));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @GetMapping("/pcweb")
    public ResponseEntity<CustomResponse<BannerDto>> selectPcWebBanner() {
        CustomResponse<BannerDto> res = new CustomResponse<>();
        try {
            Banner banner = bannerService.selectPcWebBanner();
            res.setData(Optional.ofNullable(banner == null ? null : banner.convert2Dto()));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @GetMapping("/my-page")
    public ResponseEntity<CustomResponse<List<BannerDto>>> selectMyPageBanner() {
        CustomResponse<List<BannerDto>> res = new CustomResponse<>();
        try {
            List<Banner> banner = bannerService.selectMyPageBanner();
            res.setData(Optional.of(banner.stream().map(Banner::convert2Dto).toList()));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<BannerDto>> selectBanner(@PathVariable("id") Integer id) {
        CustomResponse<BannerDto> res = new CustomResponse<>();
        try {
            BannerDto banner = bannerService.selectBanner(id).convert2Dto();
            res.setData(Optional.ofNullable(banner));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<CustomResponse<BannerDto>> createBanner(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                  @RequestPart(value = "type") BannerType type,
                                                                  @RequestPart(value = "image") MultipartFile image,
                                                                  @RequestPart(value = "link", required = false) String link,
                                                                  @RequestPart(value = "curationId", required = false) Integer curationId,
                                                                  @RequestPart(value = "noticeId", required = false) Integer noticeId,
                                                                  @RequestPart(value = "categoryId", required = false) Integer categoryId) {
        CustomResponse<BannerDto> res = new CustomResponse<>();
        Optional<TokenInfo> tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);
        if (tokenInfo == null) return res.throwError("인증이 필요합니다.", "FORBIDDEN");
        try {
            Banner banner = new Banner();
            if (type == BannerType.CATEGORY) {
                if (categoryId == null) return res.throwError("카테고리 아이디를 입력해주세요.", "INPUT_CHECK_REQUIRED");
                categoryService.findById(categoryId);
                banner.setCategoryId(categoryId);
            } else if (type == BannerType.CURATION) {
                if (curationId == null) return res.throwError("큐레이션 아이디를 입력해주세요.", "INPUT_CHECK_REQUIRED");
                curationService.selectCuration(curationId);
                banner.setCurationId(curationId);
            } else if (type == BannerType.NOTICE) {
                if (noticeId == null) return res.throwError("배너 아이디를 입력해주세요.", "INPUT_CHECK_REQUIRED");
//                curationService.findById(categoryId);
                banner.setNoticeId(noticeId);
            }
            if (link != null) {
                if (!Pattern.matches(re.httpUrl, link)) return res.throwError("링크 형식을 확인해주세요.", "INPUT_CHECK_REQUIRED");
                banner.setLink(link);
            }
            banner.setState(BannerState.ACTIVE);
            banner.setType(type);
            if (!type.equals(BannerType.MAIN) && !type.equals(BannerType.PC_WEB) && !type.equals(BannerType.MY_PAGE))
                banner.setSortNo(bannerService.getSortNo());
            String imageUrl = s3.upload(image, new ArrayList<>(List.of("banner")));
            banner.setImage(imageUrl);
            BannerDto result = bannerService.addBanner(banner).convert2Dto();
            res.setData(Optional.ofNullable(result));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<CustomResponse<BannerDto>> updateBanner(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                  @PathVariable("id") Integer id,
                                                                  @RequestPart(value = "type", required = false) BannerType type,
                                                                  @RequestPart(value = "image", required = false) MultipartFile image,
                                                                  @RequestPart(value = "link", required = false) String link,
                                                                  @RequestPart(value = "curationId", required = false) Integer curationId,
                                                                  @RequestPart(value = "noticeId", required = false) Integer noticeId,
                                                                  @RequestPart(value = "categoryId", required = false) Integer categoryId) {
        CustomResponse<BannerDto> res = new CustomResponse<>();
        Optional<TokenInfo> tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);
        if (tokenInfo == null) return res.throwError("인증이 필요합니다.", "FORBIDDEN");
        try {
            Banner banner = bannerService.selectBanner(id);
            if (type != null) {
                if (type.equals(BannerType.NONE)) {
                    banner.setCurationId(null);
                    banner.setNoticeId(null);
                    banner.setCategoryId(null);
                    banner.setLink(null);
                } else if (type.equals(BannerType.CATEGORY)) {
                    if (categoryId == null) return res.throwError("카테고리 아이디를 입력해주세요.", "INPUT_CHECK_REQUIRED");
                    banner.setCategoryId(categoryId);
                    banner.setNoticeId(null);
                    banner.setCurationId(null);
                    banner.setLink(null);
                } else if (type.equals(BannerType.NOTICE)) {
                    if (noticeId == null) return res.throwError("공지사항 아이디를 입력해주세요.", "INPUT_CHECK_REQUIRED");
                    banner.setNoticeId(noticeId);
                    banner.setCurationId(null);
                    banner.setCategoryId(null);
                    banner.setLink(null);
                } else if (type.equals(BannerType.CURATION)) {
                    if (curationId == null) return res.throwError("큐레이션 아이디를 입력해주세요.", "INPUT_CHECK_REQUIRED");
                    banner.setCurationId(curationId);
                    banner.setCategoryId(null);
                    banner.setNoticeId(null);
                    banner.setLink(null);
                }
                banner.setType(type);
            }
            if (link != null) {
                if (!Pattern.matches(re.httpUrl, link)) return res.throwError("링크 형식을 확인해주세요.", "INPUT_CHECK_REQUIRED");
            }
            banner.setLink(link);
            if (image != null) {
                String
                        imageUrl =
                        s3.upload(image, new ArrayList<>(Arrays.asList("banner", String.valueOf(banner.getId()))));
                banner.setImage(imageUrl);
            }
            BannerDto result = bannerService.updateBanner(banner).convert2Dto();
            res.setData(Optional.ofNullable(result));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @Getter
    @NoArgsConstructor
    private static class UpdateBannerStateReq {
        List<Integer> ids;
        BannerState state;
    }

    @PostMapping("/update/state")
    public ResponseEntity<CustomResponse<Boolean>> updateBannerState(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                     @RequestPart(value = "data") UpdateBannerStateReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();
        Optional<TokenInfo> tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);
        if (tokenInfo == null) return res.throwError("인증이 필요합니다.", "FORBIDDEN");
        try {
            if (data.ids == null || data.ids.size() == 0) return res.throwError("아이디를 입력해주세요.", "INPUT_CHECK_REQUIRED");
            if (data.state == null) return res.throwError("변경할 상태를 입력해주세요.", "INPUT_CHECK_REQUIRED");
            List<Banner> banners = bannerService.selectBannerListWithIds(data.ids);
            banners.forEach(v -> {
                v.setState(data.state);
            });
            bannerService.updateAllBanners(banners);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CustomResponse<Boolean>> deleteBanner(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                @PathVariable("id") Integer id) {
        CustomResponse<Boolean> res = new CustomResponse<>();
        Optional<TokenInfo> tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);
        if (tokenInfo == null) return res.throwError("인증이 필요합니다.", "FORBIDDEN");
        try {
            bannerService.selectBanner(id);
            Boolean result = bannerService.deleteBanner(id);
            List<Banner> banners = bannerService.selectBannerListWithSortNo();
            for (int i = 0; i < banners.size(); i++) {
                banners.get(i).setSortNo(i + 1);
            }
            bannerService.updateAllBanners(banners);
            res.setData(Optional.ofNullable(result));
            res.setIsSuccess(result);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }
}
