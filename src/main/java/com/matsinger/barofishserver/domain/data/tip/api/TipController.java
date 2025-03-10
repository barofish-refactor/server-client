package com.matsinger.barofishserver.domain.data.tip.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matsinger.barofishserver.domain.data.tip.application.TipCommandService;
import com.matsinger.barofishserver.domain.data.tip.application.TipQueryService;
import com.matsinger.barofishserver.domain.data.tip.domain.*;
import com.matsinger.barofishserver.domain.data.tip.dto.AddTipReq;
import com.matsinger.barofishserver.domain.data.tip.dto.TipInfoUpdateReq;
import com.matsinger.barofishserver.domain.data.tip.dto.UpdateTipStateReq;
import com.matsinger.barofishserver.domain.siteInfo.application.SiteInfoCommandService;
import com.matsinger.barofishserver.domain.siteInfo.application.SiteInfoQueryService;
import com.matsinger.barofishserver.domain.siteInfo.domain.SiteInformation;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.jwt.JwtService;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.CustomResponse;
import com.matsinger.barofishserver.utils.S3.Uploader;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tip")
public class TipController {

    private final TipQueryService tipQueryService;
    private final TipCommandService tipCommandService;

    private final Common utils;
    private final Uploader s3;
    private final SiteInfoQueryService siteInfoQueryService;
    private final SiteInfoCommandService siteInfoCommandService;
    private final JwtService jwt;

    @GetMapping("")
    public ResponseEntity<CustomResponse<List<Tip>>> selectTipList(@RequestParam(value = "type", required = false) TipType type) {
        CustomResponse<List<Tip>> res = new CustomResponse<>();

        List<Tip> tips = tipQueryService.selectTipList(type, TipState.ACTIVE);
        res.setData(Optional.ofNullable(tips));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/info")
    public ResponseEntity<CustomResponse<TipInfo>> selectTipInfo() {
        CustomResponse<TipInfo> res = new CustomResponse<>();

        SiteInformation info = siteInfoQueryService.selectSiteInfo("INTERNAL_TIP_INFO");
        String jsonStr = info.getContent();
        JSONObject jsonObject = new JSONObject(jsonStr);
        TipInfo
                tipInfo =
                TipInfo.builder().thumbnailImage(jsonObject.getString("thumbnailImage")).name(jsonObject.getString(
                        "name")).title(jsonObject.getString("title")).subTitle(jsonObject.getString("subTitle")).build();
        res.setData(Optional.ofNullable(tipInfo));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/management")
    public ResponseEntity<CustomResponse<Page<Tip>>> selectTipList(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                   @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                   @RequestParam(value = "take", required = false, defaultValue = "10") Integer take,
                                                                   @RequestParam(value = "orderby", defaultValue = "id") TipOrderBy orderBy,
                                                                   @RequestParam(value = "orderType", defaultValue = "DESC") Sort.Direction sort,
                                                                   @RequestParam(value = "title", required = false) String title,
                                                                   @RequestParam(value = "content", required = false) String content,
                                                                   @RequestParam(value = "type", required = false) String type,
                                                                   @RequestParam(value = "createdAtS", required = false) Timestamp createdAtS,
                                                                   @RequestParam(value = "createdAtE", required = false) Timestamp createdAtE) {
        CustomResponse<Page<Tip>> res = new CustomResponse<>();

                jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Specification<Tip> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (title != null) predicates.add(builder.like(root.get("title"), "%" + title + "%"));
            if (content != null) predicates.add(builder.like(root.get("content"), "%" + content + "%"));
            if (type != null)
                predicates.add(builder.and(root.get("type").in(Arrays.stream(type.split(",")).map(TipType::valueOf).toList())));
            if (createdAtS != null) predicates.add(builder.greaterThan(root.get("createdAt"), createdAtS));
            if (createdAtE != null) predicates.add(builder.lessThan(root.get("createdAt"), createdAtE));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageRequest = PageRequest.of(page, take, Sort.by(sort, orderBy.label));
        Page<Tip> tips = tipQueryService.selectTip(pageRequest, spec);
        res.setData(Optional.ofNullable(tips));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<Tip>> selectTip(@PathVariable("id") Integer id) {
        CustomResponse<Tip> res = new CustomResponse<>();

        Tip tip = tipQueryService.selectTip(id);
        res.setData(Optional.ofNullable(tip));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/add")
    public ResponseEntity<CustomResponse<Tip>> addTip(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                      @RequestPart(value = "data") AddTipReq data,
                                                      @RequestPart(value = "image") MultipartFile image,
                                                      @RequestPart(value = "imageDetail") MultipartFile imageDetail) throws Exception {
        CustomResponse<Tip> res = new CustomResponse<>();

                jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        String title = utils.validateString(data.getTitle(), 100L, "제목");
        String description = utils.validateString(data.getDescription(), 200L, "설명");
        String imageUrl = s3.upload(image, new ArrayList<>(List.of("tip")));
        String imageDetailUrl = s3.upload(imageDetail, new ArrayList<>(List.of("tip")));
        if (data.getContent() == null) throw new BusinessException("내용을 입력해주세요.");
        String contentUrl = s3.uploadEditorStringToS3(data.getContent(), new ArrayList<>(List.of("tip")));
        Tip
                tip =
                Tip.builder().title(title).type(data.getType() ==
                        null ? TipType.COMPARE : data.getType()).description(description).state(TipState.ACTIVE).image(
                        imageUrl).imageDetail(imageDetailUrl).content(contentUrl).createdAt(utils.now()).build();
        res.setData(Optional.ofNullable(tipCommandService.add(tip)));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<CustomResponse<Tip>> updateTip(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                         @PathVariable("id") Integer id,
                                                         @RequestPart(value = "data") AddTipReq data,
                                                         @RequestPart(value = "image", required = false) MultipartFile image,
                                                         @RequestPart(value = "imageDetail", required = false) MultipartFile imageDetail) throws Exception {
        CustomResponse<Tip> res = new CustomResponse<>();

                jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Tip tip = tipQueryService.selectTip(id);
        if (data.getTitle() != null) {
            String title = utils.validateString(data.getTitle(), 100L, "제목");
            tip.setTitle(title);
        }
        if (data.getDescription() != null) {
            String description = utils.validateString(data.getDescription(), 200L, "설명");
            tip.setDescription(description);
        }
        if (data.getType() != null) {
            tip.setType(data.getType());
        }
        if (image != null) {
            String imageUrl = s3.upload(image, new ArrayList<>(List.of("tip")));
            tip.setImage(imageUrl);
        }
        if (data.getContent() != null) {
            String contentUrl = s3.uploadEditorStringToS3(data.getContent(), new ArrayList<>(List.of("tip")));
            tip.setContent(contentUrl);
        }
        if (imageDetail != null) {
            String imageDetailUrl = s3.upload(imageDetail, new ArrayList<>(List.of("tip")));
            tip.setImageDetail(imageDetailUrl);
        }
        res.setData(Optional.ofNullable(tipCommandService.update(id, tip)));
        return ResponseEntity.ok(res);
    }


    @PostMapping("/info/update")
    public ResponseEntity<CustomResponse<TipInfo>> updateTipInfo(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                 @RequestPart(value = "data") TipInfoUpdateReq data,
                                                                 @RequestPart(value = "thumbnailImage", required = false) MultipartFile thumbnailImage) throws Exception {
        CustomResponse<TipInfo> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        SiteInformation info = siteInfoQueryService.selectSiteInfo("INTERNAL_TIP_INFO");
        String jsonStr = info.getContent();
        JSONObject jsonObject = new JSONObject(jsonStr);
        TipInfo
                tipInfo =
                TipInfo.builder().thumbnailImage(jsonObject.getString("thumbnailImage")).name(jsonObject.getString(
                        "name")).title(jsonObject.getString("title")).subTitle(jsonObject.getString("subTitle")).build();
        if (data.getName() != null) {
            String name = utils.validateString(data.getName(), 20L, " 이름");
            tipInfo.setName(name);
        }
        if (data.getTitle() != null) {
            String title = utils.validateString(data.getTitle(), 100L, "제목");
            tipInfo.setTitle(title);
        }
        if (data.getSubTitle() != null) {
            String subTitle = utils.validateString(data.getSubTitle(), 200L, "부제목");
            tipInfo.setSubTitle(subTitle);
        }
        if (thumbnailImage != null) {
            String thumbnailImageUrl = s3.upload(thumbnailImage, new ArrayList<>(List.of("tip_info")));
            tipInfo.setThumbnailImage(thumbnailImageUrl);
        }
        ObjectMapper mapper = new ObjectMapper();
        info.setContent(mapper.writeValueAsString(tipInfo));
        siteInfoCommandService.updateSiteInfo(info);
        res.setData(Optional.ofNullable(tipInfo));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/update/state")
    public ResponseEntity<CustomResponse<Boolean>> updateTipState(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                  @RequestPart(value = "data") UpdateTipStateReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        if (data.getTipIds() == null) throw new BusinessException("아이디를 입력해주세요.");
        if (data.getState() == null) throw new BusinessException("변경할 상태를 입력해주세요.");
        List<Tip> tips = tipQueryService.selectTipListWithIds(data.getTipIds());
        tips.forEach(v -> v.setState(data.getState()));
        tipCommandService.updateTipList(tips);
        res.setData(Optional.of(true));
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CustomResponse<Boolean>> deleteTip(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                             @PathVariable("id") Integer id) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        tipCommandService.delete(id);
        res.setData(Optional.of(true));
        return ResponseEntity.ok(res);
    }

}
