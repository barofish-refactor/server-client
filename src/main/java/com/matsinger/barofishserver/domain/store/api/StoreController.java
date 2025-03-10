package com.matsinger.barofishserver.domain.store.api;

import com.matsinger.barofishserver.domain.admin.log.application.AdminLogCommandService;
import com.matsinger.barofishserver.domain.admin.log.application.AdminLogQueryService;
import com.matsinger.barofishserver.domain.admin.log.domain.AdminLog;
import com.matsinger.barofishserver.domain.admin.log.domain.AdminLogType;
import com.matsinger.barofishserver.domain.product.LikePostType;
import com.matsinger.barofishserver.domain.product.application.ProductCommandService;
import com.matsinger.barofishserver.domain.product.application.ProductQueryService;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.siteInfo.application.SiteInfoCommandService;
import com.matsinger.barofishserver.domain.siteInfo.application.SiteInfoQueryService;
import com.matsinger.barofishserver.domain.siteInfo.domain.SiteInformation;
import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.domain.store.domain.*;
import com.matsinger.barofishserver.domain.store.dto.SimpleStore;
import com.matsinger.barofishserver.domain.store.dto.StoreDto;
import com.matsinger.barofishserver.domain.store.repository.StoreInfoRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.jwt.*;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.CustomResponse;
import com.matsinger.barofishserver.utils.S3.S3Uploader;
import com.matsinger.barofishserver.utils.S3.Uploader;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/store")
public class StoreController {
    private final StoreService storeService;
    private final ProductService productService;
    private final StoreInfoRepository storeInfoRepository;
    private final JwtService jwt;
    private final JwtProvider jwtProvider;
    private final SiteInfoCommandService siteInfoCommandService;
    private final SiteInfoQueryService siteInfoQueryService;
    private final Common utils;
    private final Uploader s3;
    private final AdminLogCommandService adminLogCommandService;
    private final AdminLogQueryService adminLogQueryService;
    private final ProductQueryService productQueryService;
    private final ProductCommandService productCommandService;

    @GetMapping("")
    public ResponseEntity<CustomResponse<List<StoreDto>>> selectStoreList(@RequestHeader(value = "Authorization", required = false) Optional<String> auth,
                                                                          @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                          @RequestParam(value = "take", required = false, defaultValue = "10") Integer take,
                                                                          @RequestParam(value = "orderby", defaultValue = "joinAt") StoreOrderByAdmin orderBy,
                                                                          @RequestParam(value = "orderType", defaultValue = "DESC") Sort.Direction sort) {
        CustomResponse<List<StoreDto>> res = new CustomResponse<>();

        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Boolean isAdmin = tokenInfo.getType().equals(TokenAuthType.ADMIN);

        PageRequest pageRequest = PageRequest.of(page, take, Sort.by(sort, orderBy.label));
        List<StoreDto>
                stores =
                storeService.selectStoreList(true,
                        pageRequest,
                        null).stream().map(store -> storeService.convert2Dto(store, false)).toList();

        res.setData(Optional.ofNullable(stores));

        return ResponseEntity.ok(res);
    }

    @GetMapping("/management/list")
    public ResponseEntity<CustomResponse<Page<StoreDto>>> selectStoreListByAdmin(@RequestHeader(value = "Authorization", required = false) Optional<String> auth,
                                                                                 @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                                 @RequestParam(value = "take", required = false, defaultValue = "10") Integer take,
                                                                                 @RequestParam(value = "orderby", defaultValue = "joinAt") StoreOrderByAdmin orderBy,
                                                                                 @RequestParam(value = "orderType", defaultValue = "DESC") Sort.Direction sort,
                                                                                 @RequestParam(value = "partnerId", required = false) String loginId,
                                                                                 @RequestParam(value = "name", required = false) String name,
                                                                                 @RequestParam(value = "location", required = false) String location,
                                                                                 @RequestParam(value = "keyword", required = false) String keyword,
                                                                                 @RequestParam(value = "state", required = false) String state,
                                                                                 @RequestParam(value = "ids", required = false) String ids,
                                                                                 @RequestParam(value = "joinAtS", required = false) Timestamp joinAtS,
                                                                                 @RequestParam(value = "joinAtE", required = false) Timestamp joinAtE) {
        CustomResponse<Page<StoreDto>> res = new CustomResponse<>();

        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Specification<Store> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null) predicates.add(builder.like(root.get("storeInfo").get("name"), "%" + name + "%"));
            if (loginId != null) predicates.add(builder.like(root.get("loginId"), "%" + loginId + "%"));
            if (location != null)
                predicates.add(builder.like(root.get("storeInfo").get("location"), "%" + location + "%"));
            if (ids != null) predicates.add(builder.and(root.get("id").in(utils.str2IntList(ids))));
            if (state != null)
                predicates.add(builder.and(root.get("state").in(Arrays.stream(state.split(",")).map(StoreState::valueOf).toList())));
            if (joinAtS != null) predicates.add(builder.greaterThan(root.get("joinAt"), joinAtS));
            if (joinAtE != null) predicates.add(builder.lessThan(root.get("joinAt"), joinAtE));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageRequest = PageRequest.of(page, take, Sort.by(sort, orderBy.label));

        if (orderBy.label.equals("state")) {
//                pageRequest = PageRequest.of(page, take, Sort.by(sort, "joinAt").and(Sort.by(sort, orderBy.label)));
            pageRequest = PageRequest.of(page, take, Sort.by(sort, orderBy.label).and(Sort.by(sort, "joinAt")));
        }
        Page<StoreDto>
                stores =
                storeService.selectStoreList(true, pageRequest, spec).map(store -> storeService.convert2Dto(store,
                        false));

        res.setData(Optional.ofNullable(stores));

        return ResponseEntity.ok(res);
    }

    @GetMapping("/recommend")
    public ResponseEntity<CustomResponse<List<SimpleStore>>> selectRecommendStoreList(@RequestHeader("Authorization") Optional<String> auth,
                                                                                      @RequestParam(value = "type") StoreRecommendType type,
                                                                                      @RequestParam(value = "page", defaultValue = "1", required = false) Integer page,
                                                                                      @RequestParam(value = "take", defaultValue = "10", required = false) Integer take,
                                                                                      @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {
        CustomResponse<List<SimpleStore>> res = new CustomResponse<>();

        Integer userId = null;
        TokenInfo tokenInfo = null;
        if (auth.isEmpty()) {
            userId = null;
        }
        if (auth.isPresent()) {
            tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ALLOW, TokenAuthType.USER), auth);
            userId = tokenInfo.getId();
        }

        List<StoreInfo> storeInfos = storeService.selectRecommendStore(type, page - 1, take, keyword);
        Integer finalUserId = userId;
        List<SimpleStore> stores = storeInfos.stream().map(v -> {
            SimpleStore simpleStore = storeService.convert2SimpleDto(v, finalUserId);
            simpleStore.setProducts(productService.selectProductListWithStoreIdAndStateActive(v.getStoreId()).stream().map(
                    productService::convert2ListDto).toList());
            return simpleStore;
        }).toList();

        res.setData(Optional.ofNullable(stores));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<CustomResponse<Jwt>> loginStore(@RequestPart(value = "loginId") String loginId,
                                                          @RequestPart(value = "password") String password) {
        CustomResponse<Jwt> res = new CustomResponse<>();

        Optional<Store> store = storeService.selectOptionalStoreByLoginId(loginId);
        if (store == null || store.isEmpty()) throw new BusinessException("아이디 및 비밀번호를 확인해주세요.");
        if (!BCrypt.checkpw(password, store.get().getPassword()))
            throw new BusinessException("아이디 및 비밀번호를 확인해주세요.");
        if (!store.get().getState().equals(StoreState.ACTIVE)) {
            if (store.get().getState().equals(StoreState.BANNED))
                throw new BusinessException("정지된 계정입니다.");
            if (store.get().getState().equals(StoreState.DELETED))
                throw new BusinessException("삭제된 계정입니다.");
        }
        String
                accessToken =
                jwtProvider.generateAccessToken(String.valueOf(store.get().getId()), TokenAuthType.PARTNER);
        String
                refreshToken =
                jwtProvider.generateRefreshToken(String.valueOf(store.get().getId()), TokenAuthType.PARTNER);
        Jwt token = new Jwt();
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        res.setData(Optional.of(token));
        return ResponseEntity.ok(res);
    }

    @GetMapping(value = {"/management/{id}", "management"})
    public ResponseEntity<CustomResponse<StoreDto>> selectStoreByAdmin(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                       @PathVariable(value = "id", required = false) Integer id) {
        CustomResponse<StoreDto> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN, TokenAuthType.PARTNER), auth);


        if (tokenInfo.getType().equals(TokenAuthType.PARTNER)) {
            id = tokenInfo.getId();
        } else {
            if (id == null) throw new BusinessException("스토어 아이디를 입력해주세요.");
        }
        Store store = storeService.selectStore(id);

        res.setData(Optional.ofNullable(storeService.convert2Dto(store, false)));
        return ResponseEntity.ok(res);
    }

    @GetMapping(value = {"/is-active"})
    public ResponseEntity<CustomResponse<Boolean>> checkStoreIsActive(@RequestHeader(value = "Authorization") Optional<String> auth) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.PARTNER), auth);

        Integer storeId = tokenInfo.getId();
        Store store = storeService.selectStore(storeId);
        res.setData(Optional.ofNullable(store.getState().equals(StoreState.ACTIVE)));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<SimpleStore>> selectStore(@PathVariable("id") Integer id,
                                                                   @RequestHeader(value = "Authorization") Optional<String> auth) {
        CustomResponse<SimpleStore> res = new CustomResponse<>();

        Integer userId = null;
        TokenInfo tokenInfo = null;
        if (auth.isEmpty()) {
            userId = null;
        }
        if (auth.isPresent()) {
            tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ALLOW, TokenAuthType.USER), auth);
            userId = tokenInfo.getId();
        }

        StoreInfo storeInfo = storeService.selectStoreInfo(id);
        SimpleStore data = storeService.convert2SimpleDto(storeInfo, userId);
        List<Product> products = productService.selectProductListWithStoreIdAndStateActive(id);
        data.setProducts(products.stream().map(productService::convert2ListDto).toList());
        res.setData(Optional.ofNullable(data));
        return ResponseEntity.ok(res);
    }

    @Getter
    @NoArgsConstructor
    private static class UpdateStorePasswordReq {
        Integer storeId;
        String oldPassword;
        String newPassword;
    }

    @PostMapping("/update/password")
    public ResponseEntity<CustomResponse<Boolean>> updatePassword(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                  @RequestPart(value = "data") UpdateStorePasswordReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN, TokenAuthType.PARTNER), auth);

        Integer storeId;
        if (tokenInfo.getType().equals(TokenAuthType.PARTNER)) {
            if (data.oldPassword == null) throw new BusinessException("이전 비밀번호를 입력해주세요.");
            else if (data.newPassword == null) throw new BusinessException("새로운 비밀번호를 입력해주세요.");
            storeId = tokenInfo.getId();
        } else {
            if (data.storeId == null) throw new BusinessException("파트너 아이디를 입력해주세요.");
            if (data.newPassword == null) throw new BusinessException("새로운 비밀번호를 입력해주세요.");
            storeId = data.storeId;
        }
        if (storeId == null) throw new BusinessException("파트너 정보를 찾을 수 없습니다.");
        Store store = storeService.selectStore(storeId);
        if (tokenInfo.getType().equals(TokenAuthType.PARTNER)) {
            if (!BCrypt.checkpw(data.oldPassword, store.getPassword()))
                throw new BusinessException("이전 비밀번호를 확인해주세요.");
        }
        store.setPassword(BCrypt.hashpw(data.newPassword, BCrypt.gensalt()));
        storeService.updateStore(store);
        res.setData(Optional.of(true));
        return ResponseEntity.ok(res);
    }

    @Getter
    @NoArgsConstructor
    private static class AddStoreAdditionalReq {
        Float settlementRate;
        String bankName;
        String bankHolder;
        String bankAccount;
        String representativeName;
        String companyId;
        String businessType;
        String mosRegistrationNumber;
        String businessAddress;
        String postalCode;
        String lotNumberAddress;
        String streetNameAddress;
        String addressDetail;
        String tel;
        String email;
        String faxNumber;
    }

    @PostMapping("/add")
    public ResponseEntity<CustomResponse<StoreDto>> addStore(@RequestHeader("Authorization") Optional<String> auth,
                                                             @RequestPart(value = "loginId") String loginId,
                                                             @RequestPart(value = "password") String password,
                                                             @RequestPart(value = "backgroundImage") MultipartFile backgroundImage,
                                                             @RequestPart(value = "profileImage") MultipartFile profileImage,
                                                             @RequestPart(value = "name") String name,
                                                             @RequestPart(value = "location") String location,
                                                             @RequestPart(value = "keyword") String keyword,
                                                             @RequestPart(value = "visitNote", required = false) String visitNote,
                                                             @RequestPart(value = "refundDeliverFee", required = false) Integer refundDeliverFee,
                                                             @RequestPart(value = "isConditional", required = true) Boolean isConditional,
                                                             @RequestPart(value = "minStorePrice", required = true) Integer minStorePrice,
                                                             @RequestPart(value = "deliveryFee", required = true) Integer deliveryFee,
                                                             @RequestPart(value = "oneLineDescription", required = false) String oneLineDescription,
                                                             @RequestPart(value = "deliverCompany", required = false) String deliverCompany,
                                                             @RequestPart(value = "additionalData") AddStoreAdditionalReq data,
                                                             @RequestPart(value = "mosRegistration") MultipartFile mosRegistration,
                                                             @RequestPart(value = "businessRegistration") MultipartFile businessRegistration,
                                                             @RequestPart(value = "bankAccountCopy") MultipartFile bankAccountCopy) {
        CustomResponse<StoreDto> res = new CustomResponse<>();

        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Integer adminId = tokenInfo.getId();
        loginId = utils.validateString(loginId, 50L, "로그인 아이디");
        Boolean check = storeService.checkStoreLoginIdValid(loginId);
        if (!check) throw new BusinessException("중복된 아이디입니다.");
        if (!password.matches("^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_\\-+=\\[\\]{}|\\\\:;\"'<>,.?/])(?=" +
                ".*[^\\s]).{8,20}$")) throw new BusinessException("비밀번호 형식을 확인해주세요.");
        name = utils.validateString(name, 50L, "이름");
        location = utils.validateString(location, 50L, "위치");
        deliverCompany = utils.validateString(deliverCompany, 20L, "택배사");
        if (data.settlementRate == null) throw new BusinessException("정산 비율을 입력해주세요.");
        if (data.getSettlementRate() > 100 || data.getSettlementRate() < 0)
            throw new BusinessException("정산 비율을 확인해주세요.");
        String bankName = utils.validateString(data.bankName, 20L, "은행명");
        String bankHolder = utils.validateString(data.bankHolder, 50L, "예금주");
        String bankAccount = utils.validateString(data.bankAccount, 50L, "계좌번호");
        String representativeName = utils.validateString(data.representativeName, 20L, "대표자 이름");
        String companyId = utils.validateString(data.companyId, 20L, "사업자 번호");
        String businessType = utils.validateString(data.businessType, 50L, "업태/종목");
        String mosRegistrationNumber = utils.validateString(data.mosRegistrationNumber, 50L, "통신판매신고번호");
        String businessAddress = utils.validateString(data.businessAddress, 200L, "사업장 주소");
        String postalCode = utils.validateString(data.postalCode, 5L, "우편번호");
        String lotNumberAddress = utils.validateString(data.lotNumberAddress, 200L, "지번 주소");
        String streetNameAddress = utils.validateString(data.streetNameAddress, 200L, "도로명 주소");
        String addressDetail = utils.validateString(data.addressDetail, 200L, "상세 주소");
        String tel = utils.validateString(data.tel, 20L, "전화번호");
        String email = utils.validateString(data.email, 300L, "이메일");
        String faxNumber = data.faxNumber != null ? utils.validateString(data.faxNumber, 20L, "팩스 번호") : null;
        String oneLineDescriptionData = "";
        if (oneLineDescription != null)
            oneLineDescriptionData = utils.validateString(oneLineDescription, 500L, "한 줄 소개");
        Store
                storeData =
                Store.builder().loginId(loginId).password(BCrypt.hashpw(password, BCrypt.gensalt())).state(
                        StoreState.ACTIVE).joinAt(utils.now()).build();
        StoreInfo
                storeInfoData =
                StoreInfo.builder().name(name).location(location).keyword(keyword).visitNote("").refundDeliverFee(
                        refundDeliverFee).oneLineDescription(oneLineDescriptionData).settlementRate(data.settlementRate).bankName(
                        bankName).bankHolder(bankHolder).bankAccount(bankAccount).representativeName(
                        representativeName).companyId(companyId).businessType(businessType).mosRegistrationNumber(
                        mosRegistrationNumber).businessAddress(businessAddress).postalCode(postalCode).lotNumberAddress(
                        lotNumberAddress).streetNameAddress(streetNameAddress).addressDetail(addressDetail).tel(tel).email(
                        email).faxNumber(faxNumber).deliverCompany(deliverCompany).isReliable(false).build();

        storeInfoData.setIsConditional(isConditional);
        storeInfoData.setMinStorePrice(minStorePrice);
        storeInfoData.setDeliveryFee(deliveryFee);

        Store store = storeService.addStore(storeData);
        String
                backgroundImageUrl =
                s3.upload(backgroundImage, new ArrayList<>(Arrays.asList("store", String.valueOf(store.getId()))));
        String
                profileImageUrl =
                s3.upload(profileImage, new ArrayList<>(Arrays.asList("store", String.valueOf(store.getId()))));
        String
                visitNoteUrl =
                visitNote != null ? s3.uploadEditorStringToS3(visitNote,
                        new ArrayList<>(Arrays.asList("store", String.valueOf(store.getId())))) : null;
        String
                mosRegistrationUrl =
                s3.upload(mosRegistration, new ArrayList<>(Arrays.asList("store", String.valueOf(store.getId()))));
        String
                businessRegistrationUrl =
                s3.upload(businessRegistration,
                        new ArrayList<>(Arrays.asList("store", String.valueOf(store.getId()))));
        String
                bankAccountCopyUrl =
                s3.upload(bankAccountCopy, new ArrayList<>(Arrays.asList("store", String.valueOf(store.getId()))));

        storeInfoData.setStoreId(store.getId());
        storeInfoData.setVisitNote(visitNoteUrl);
        storeInfoData.setbackgroundImage(backgroundImageUrl);
        storeInfoData.setProfileImage(profileImageUrl);
        storeInfoData.setMosRegistration(mosRegistrationUrl);
        storeInfoData.setBusinessRegistration(businessRegistrationUrl);
        storeInfoData.setBankAccountCopy(bankAccountCopyUrl);
        StoreInfo storeInfo = storeService.addStoreInfo(storeInfoData);
        res.setData(Optional.of(storeService.convert2Dto(store, false)));
        AdminLog
                adminLog =
                AdminLog.builder().id(adminLogQueryService.getAdminLogId()).adminId(adminId).type(AdminLogType.PARTNER).targetId(
                        String.valueOf(store.getId())).content("파트너 등록을 완료하였습니다.").createdAt(utils.now()).build();
        adminLogCommandService.saveAdminLog(adminLog);
        return ResponseEntity.ok(res);
    }

    @Getter
    @NoArgsConstructor
    private static class StoreStateUpdateReq {
        List<Integer> storeIds;
        StoreState state;
    }

    @PostMapping("/update/state")
    public ResponseEntity<CustomResponse<Boolean>> updateStoreState(@RequestHeader("Authorization") Optional<String> auth,
                                                                    @RequestPart(value = "data") StoreStateUpdateReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Integer adminId = tokenInfo.getId();
        List<Store> stores = new ArrayList<>();
        for (Integer storeId : data.getStoreIds()) {
            Store store = storeService.selectStore(storeId);
            store.setState(data.state);
            if (data.state.equals(StoreState.BANNED)) {
                StoreInfo storeInfo = storeService.selectStoreInfo(storeId);
                storeInfo.setIsReliable(false);
                storeService.updateStoreInfo(storeInfo);
            }
            stores.add(store);
            String
                    content =
                    String.format("%s -> %s 상태 변경하였습니다.",
                            store.getState().equals(StoreState.ACTIVE) ? "활동중" : "정지",
                            data.getState().equals(StoreState.ACTIVE) ? "활동중" : "정지");
            AdminLog
                    adminLog =
                    AdminLog.builder().id(adminLogQueryService.getAdminLogId()).adminId(adminId).type(AdminLogType.PARTNER).targetId(
                            storeId.toString()).content(content).createdAt(utils.now()).build();
            adminLogCommandService.saveAdminLog(adminLog);

            productCommandService.convertStoreProductsStateWhenPartnerUpdate(store.getId(), data.getState());
        }
        storeService.updateStores(stores);
        res.setData(Optional.of(true));
        return ResponseEntity.ok(res);
    }

    @PostMapping(value = {"/update/{id}", "/update"})
    public ResponseEntity<CustomResponse<StoreDto>> updateStoreInfo(@RequestHeader("Authorization") Optional<String> auth,
                                                                    @PathVariable(value = "id", required = false) Integer id,
                                                                    @RequestPart(value = "backgroundImage", required = false) MultipartFile backgroundImage,
                                                                    @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
                                                                    @RequestPart(value = "name", required = false) String name,
                                                                    @RequestPart(value = "isReliable", required = false) Boolean isReliable,
                                                                    @RequestPart(value = "location", required = false) String location,
                                                                    @RequestPart(value = "keyword", required = false) String keyword,
                                                                    @RequestPart(value = "visitNote", required = false) String visitNote,
                                                                    @RequestPart(value = "deliverCompany", required = false) String deliverCompany,
                                                                    @RequestPart(value = "refundDeliverFee", required = false) Integer refundDeliverFee,
                                                                    @RequestPart(value = "isConditional", required = true) Boolean isConditional,
                                                                    @RequestPart(value = "minStorePrice", required = true) Integer minStorePrice,
                                                                    @RequestPart(value = "deliveryFee", required = true) Integer deliveryFee,
                                                                    @RequestPart(value = "oneLineDescription", required = false) String oneLineDescription,
                                                                    @RequestPart(value = "additionalData", required = false) AddStoreAdditionalReq data,
                                                                    @RequestPart(value = "mosRegistration", required = false) MultipartFile mosRegistration,
                                                                    @RequestPart(value = "businessRegistration", required = false) MultipartFile businessRegistration,
                                                                    @RequestPart(value = "bankAccountCopy", required = false) MultipartFile bankAccountCopy) {
        CustomResponse<StoreDto> res = new CustomResponse<>();

        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN, TokenAuthType.PARTNER), auth);

        if (tokenInfo.getType().equals(TokenAuthType.PARTNER)) {
            id = tokenInfo.getId();
        } else {
            if (id == null) throw new BusinessException("스토어 아이디를 입력해주세요.");
        }
        boolean isAdmin = tokenInfo.getType().equals(TokenAuthType.ADMIN);
        StoreInfo storeInfo = storeService.selectStoreInfo(id);
        if (backgroundImage != null) {
            String
                    imageUrl =
                    s3.upload(backgroundImage, new ArrayList<>(Arrays.asList("store", String.valueOf(id))));
            storeInfo.setbackgroundImage(imageUrl);
        }
        if (profileImage != null) {
            String imageUrl = s3.upload(profileImage, new ArrayList<>(Arrays.asList("store", String.valueOf(id))));
            storeInfo.setProfileImage(imageUrl);
        }
        if (name != null) {
            name = utils.validateString(name, 50L, "이름");
            storeInfo.setName(name);
        }
        if (location != null) {
            location = utils.validateString(location, 50L, "위치");
            storeInfo.setLocation(location);
        }
        if (deliverCompany != null) {
            deliverCompany = utils.validateString(deliverCompany, 20L, "택배사");
            storeInfo.setDeliverCompany(deliverCompany);
        }
        if (keyword != null) {
            keyword = utils.validateString(keyword, 50L, "위치");
            storeInfo.setKeyword(keyword);
        }
        if (oneLineDescription != null) {
            oneLineDescription = utils.validateString(oneLineDescription, 500L, "한 줄 소개");
            storeInfo.setOneLineDescription(oneLineDescription);
        }
        if (isReliable != null) {
            if (tokenInfo.getType().equals(TokenAuthType.ADMIN)) storeInfo.setIsReliable(isReliable);
        }
        if (refundDeliverFee != null) {
            storeInfo.setRefundDeliverFee(refundDeliverFee);
        }
        if (data != null) {
            if (data.settlementRate != null) {
                if (!isAdmin) throw new BusinessException("수정 불가능한 항목입니다.");
                if (data.getSettlementRate() > 100 || data.getSettlementRate() < 0)
                    throw new BusinessException("정산 비율을 확인해주세요.");
                storeInfo.setSettlementRate(data.settlementRate);
            }
            if (data.bankName != null) {
                String bankName = utils.validateString(data.bankName, 20L, "은행명");
                storeInfo.setBankName(bankName);
            }
            if (data.bankHolder != null) {
                String bankHolder = utils.validateString(data.bankHolder, 50L, "예금주");
                storeInfo.setBankHolder(bankHolder);
            }
            if (data.bankAccount != null) {
                String bankAccount = utils.validateString(data.bankAccount, 50L, "계좌번호");
                storeInfo.setBankAccount(bankAccount);
            }
            if (data.representativeName != null) {
                String representativeName = utils.validateString(data.representativeName, 20L, "대표자 이름");
                storeInfo.setRepresentativeName(representativeName);
            }
            if (data.companyId != null) {
                String companyId = utils.validateString(data.companyId, 20L, "사업자 번호");
                storeInfo.setCompanyId(data.companyId);
            }
            if (data.businessType != null) {
                String businessType = utils.validateString(data.businessType, 50L, "업태/종목");
                storeInfo.setBusinessType(businessType);
            }
            if (data.mosRegistrationNumber != null) {
                String mosRegistrationNumber = utils.validateString(data.mosRegistrationNumber, 50L, "통신판매신고번호");
                storeInfo.setMosRegistrationNumber(mosRegistrationNumber);
            }
            if (data.businessAddress != null) {
                String businessAddress = utils.validateString(data.businessAddress, 200L, "사업장 주소");
                storeInfo.setBusinessAddress(businessAddress);
            }
            if (data.postalCode != null) {
                String postalCode = utils.validateString(data.postalCode, 5L, "우편번호");
                storeInfo.setPostalCode(postalCode);
            }
            if (data.lotNumberAddress != null) {
                String lotNumberAddress = utils.validateString(data.lotNumberAddress, 200L, "지번 주소");
                storeInfo.setLotNumberAddress(lotNumberAddress);
            }
            if (data.streetNameAddress != null) {
                String streetNameAddress = utils.validateString(data.streetNameAddress, 200L, "도로명 주소");
                storeInfo.setStreetNameAddress(streetNameAddress);
            }
            if (data.addressDetail != null) {
                String addressDetail = utils.validateString(data.addressDetail, 200L, "상세 주소");
                storeInfo.setAddressDetail(addressDetail);
            }
            if (data.tel != null) {
                String tel = utils.validateString(data.tel, 20L, "전화번호");
                storeInfo.setTel(tel);
            }
            if (data.email != null) {
                String email = utils.validateString(data.email, 300L, "이메일");
                storeInfo.setEmail(email);
            }
            if (data.faxNumber != null) {
                String faxNumber = utils.validateString(data.faxNumber, 20L, "팩스 번호");
                storeInfo.setFaxNumber(faxNumber);
            }
        }
        if (visitNote != null) {
            String
                    visitNoteUrl =
                    s3.uploadEditorStringToS3(visitNote,
                            new ArrayList<>(Arrays.asList("store", String.valueOf(id))));
            storeInfo.setVisitNote(visitNoteUrl);
        }
        if (mosRegistration != null) {
            String
                    mosRegistrationUrl =
                    s3.upload(mosRegistration, new ArrayList<>(Arrays.asList("store", String.valueOf(id))));
            storeInfo.setMosRegistration(mosRegistrationUrl);
        }
        if (businessRegistration != null) {
            String
                    businessRegistrationUrl =
                    s3.upload(businessRegistration, new ArrayList<>(Arrays.asList("store", String.valueOf(id))));
            storeInfo.setBusinessRegistration(businessRegistrationUrl);
        }
        if (bankAccountCopy != null) {
            String
                    bankAccountCopyUrl =
                    s3.upload(bankAccountCopy, new ArrayList<>(Arrays.asList("store", String.valueOf(id))));
            storeInfo.setBankAccountCopy(bankAccountCopyUrl);
        }

        storeInfo.setIsConditional(isConditional);
        storeInfo.setMinStorePrice(minStorePrice);
        storeInfo.setDeliveryFee(deliveryFee);

        StoreInfo result = storeService.updateStoreInfo(storeInfo);
        Store store = storeService.selectStore(result.getStoreId());
        if (isAdmin) {
            String content = "정보를 수정하였습니다.";
            AdminLog
                    adminLog =
                    AdminLog.builder().id(adminLogQueryService.getAdminLogId()).adminId(tokenInfo.getId()).type(
                            AdminLogType.PARTNER).targetId(id.toString()).createdAt(utils.now()).content(content).build();
            adminLogCommandService.saveAdminLog(adminLog);
        }
        res.setData(Optional.ofNullable(storeService.convert2Dto(store, false)));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/like")
    public ResponseEntity<CustomResponse<Boolean>> likeStoreByUser(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                   @RequestParam(value = "storeId") Integer storeId,
                                                                   @RequestParam(value = "type") LikePostType type) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.USER), auth);


        Boolean check = storeService.checkLikeStore(storeId, tokenInfo.getId());
        if (!check && type.equals(LikePostType.LIKE)) {
            storeService.likeStore(storeId, tokenInfo.getId());
            res.setData(Optional.of(true));
        } else if (check && type.equals(LikePostType.UNLIKE)) {
            storeService.unlikeStore(storeId, tokenInfo.getId());
            res.setData(Optional.of(true));
        }
        return ResponseEntity.ok(res);
    }

    @GetMapping("/star")
    public ResponseEntity<CustomResponse<List<SimpleStore>>> selectScrapedStore(@RequestHeader("Authorization") Optional<String> auth) {
        CustomResponse<List<SimpleStore>> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.USER), auth);


        List<StoreInfo> storeInfos = storeService.selectScrapedStore(tokenInfo.getId());
        List<SimpleStore> stores = new ArrayList<>();
        for (StoreInfo storeInfo : storeInfos) {
            stores.add(storeInfo.convert2Dto());
        }
        res.setData(Optional.ofNullable(stores));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/main")
    public ResponseEntity<CustomResponse<StoreDto>> selectMainStore(@RequestHeader(value = "Authorization") Optional<String> auth) {
        CustomResponse<StoreDto> res = new CustomResponse<>();

                jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        SiteInformation siteInformation = siteInfoQueryService.selectSiteInfo("INTERNAL_MAIN_STORE");
        Integer storeId = Integer.valueOf(siteInformation.getContent());
        Store store = storeService.selectStore(storeId);
        StoreDto storeDto = storeService.convert2Dto(store, false);
        res.setData(Optional.ofNullable(storeDto));
        return ResponseEntity.ok(res);
    }

    @Getter
    @NoArgsConstructor
    private static class SetMainPartnerReq {
        Integer storeId;
    }

    @PostMapping("/set-main")
    public ResponseEntity<CustomResponse<StoreDto>> setMainPartnerByAdmin(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                          @RequestPart(value = "data") SetMainPartnerReq data) {
        CustomResponse<StoreDto> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);


        SiteInformation siteInformation = siteInfoQueryService.selectSiteInfo("INTERNAL_MAIN_STORE");
        if (data.storeId == null) throw new BusinessException("파트너 아이디를 입력해주세요.");
        Store store = storeService.selectStore(data.storeId);
        siteInformation.setContent(String.valueOf(data.storeId));
        siteInfoCommandService.updateSiteInfo(siteInformation);
        res.setData(Optional.ofNullable(storeService.convert2Dto(store, false)));
        return ResponseEntity.ok(res);
    }

    @Getter
    @NoArgsConstructor
    private static class UpdateStoreIsReliableReq {
        List<Integer> storeIds;
        Boolean isReliable;
    }

    @PostMapping("/update/is-reliable")
    public ResponseEntity<CustomResponse<Boolean>> updateStoreIsReliable(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                         @RequestPart(value = "data") UpdateStoreIsReliableReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Integer adminId = tokenInfo.getId();
        if (data.getStoreIds() == null || data.getStoreIds().size() == 0)
            throw new BusinessException("파트너 아이디를 입력해주세요.");
        if (data.getIsReliable() == null) throw new BusinessException("체크 여부를 입력해주세요.");
        List<StoreInfo> storeInfos = data.getStoreIds().stream().map(v -> {
            StoreInfo storeInfo = storeService.selectStoreInfo(v);
            storeInfo.setIsReliable(data.getIsReliable());
            String content = "";
            if (data.getIsReliable()) content = "믿고 구매할 수 있는 파트너로 지정되었습니다.";
            else content = "믿고 구매할 수 있는 파트너 지정이 해제되었습니다.";
            AdminLog
                    adminLog =
                    AdminLog.builder().id(adminLogQueryService.getAdminLogId()).adminId(adminId).type(AdminLogType.PARTNER).targetId(
                            String.valueOf(storeInfo.getStoreId())).content(content).createdAt(utils.now()).build();
            adminLogCommandService.saveAdminLog(adminLog);
            return storeInfo;
        }).toList();
        storeInfoRepository.saveAll(storeInfos);
        res.setData(Optional.of(true));
        return ResponseEntity.ok(res);
    }
}
