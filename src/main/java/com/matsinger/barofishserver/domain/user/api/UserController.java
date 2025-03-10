package com.matsinger.barofishserver.domain.user.api;


import com.matsinger.barofishserver.domain.grade.application.GradeQueryService;
import com.matsinger.barofishserver.domain.siteInfo.application.SiteInfoQueryService;
import com.matsinger.barofishserver.domain.user.application.UserCommandService;
import com.matsinger.barofishserver.domain.user.application.UserQueryService;
import com.matsinger.barofishserver.domain.user.domain.User;
import com.matsinger.barofishserver.domain.user.domain.UserOrderByAdmin;
import com.matsinger.barofishserver.domain.user.domain.UserState;
import com.matsinger.barofishserver.domain.user.dto.*;
import com.matsinger.barofishserver.domain.user.paymentMethod.application.PaymentMethodCommandService;
import com.matsinger.barofishserver.domain.user.paymentMethod.application.PaymentMethodQueryService;
import com.matsinger.barofishserver.domain.user.paymentMethod.domain.PaymentMethod;
import com.matsinger.barofishserver.domain.user.paymentMethod.dto.PaymentMethodDto;
import com.matsinger.barofishserver.domain.userauth.application.UserAuthCommandService;
import com.matsinger.barofishserver.domain.userauth.application.UserAuthQueryService;
import com.matsinger.barofishserver.domain.userauth.domain.LoginType;
import com.matsinger.barofishserver.domain.userauth.domain.UserAuth;
import com.matsinger.barofishserver.domain.userinfo.application.UserInfoCommandService;
import com.matsinger.barofishserver.domain.userinfo.application.UserInfoQueryService;
import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import com.matsinger.barofishserver.domain.userinfo.dto.UserInfoDto;
import com.matsinger.barofishserver.domain.verification.Verification;
import com.matsinger.barofishserver.domain.verification.VerificationService;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.jwt.*;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.CustomResponse;
import com.matsinger.barofishserver.utils.RegexConstructor;
import com.matsinger.barofishserver.utils.fcm.FcmToken;
import com.matsinger.barofishserver.utils.fcm.FcmTokenRepository;
import com.matsinger.barofishserver.utils.sms.SmsService;
import com.matsinger.barofishserver.utils.S3.Uploader;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserInfoQueryService userInfoQueryService;
    private final UserInfoCommandService userInfoCommandService;
    private final UserAuthCommandService userAuthCommandService;
    private final UserAuthQueryService userAuthQueryService;
    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final PaymentMethodQueryService paymentMethodQueryService;
    private final PaymentMethodCommandService paymentMethodCommandService;
    private final Uploader s3;
    private final RegexConstructor re;
    private final VerificationService verificationService;
    private final SiteInfoQueryService siteInfoQueryService;
    private final JwtProvider jwtProvider;
    private final JwtService jwt;
    private final GradeQueryService gradeQueryService;
    private final SmsService smsService;
    private final FcmTokenRepository fcmTokenRepository;
    private final Common utils;

    @PostMapping(value = "/join-sns")
    public ResponseEntity<CustomResponse<Object>> joinSnsUser(@RequestPart(value = "data") SnsJoinReq request) {

        CustomResponse<Object> res = new CustomResponse<>();
        String loginId;
        boolean isNew = false;

        if (request.getLoginType().equals(LoginType.APPLE)) {
            boolean isExist = userAuthQueryService.checkUserExist(request);
            if (!isExist) {
                return ResponseEntity.ok(res);
            }
        }
        if (!request.getLoginType().equals(LoginType.APPLE))
            userCommandService.addUserAuthIfPhoneNumberExists(request);

        loginId = userQueryService.getExistingLoginId(request);

        if (loginId == null) {
            SnsJoinLoginResponseDto responseDto = userCommandService.createSnsUserAndSave(request);
            loginId = responseDto.getLoginId();
            isNew = true;

            String profileImage = "";
            if (request.getProfileImage() != null) {
                profileImage =
                        s3.upload(s3.extractBase64FromImageUrl(request.getProfileImage()),
                                new ArrayList<>(Arrays.asList("user", String.valueOf(responseDto.getUserId()))));
            }
            userInfoCommandService.setImageUrl(responseDto.getUserId(), profileImage);
        }
        Integer userId = userCommandService.selectUserByLoginId(request.getLoginType(), loginId).getUserId();
        Jwt jwt = generateAndSetTokens(userId);
        JoinResponse joinResponse = JoinResponse.builder()
                .jwt(jwt)
                .isNew(isNew)
                .build();
        res.setData(Optional.of(jwt));
        return ResponseEntity.ok(res);
    }

    @PostMapping(value = "/join", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<CustomResponse<Boolean>> joinUser(@RequestPart(value = "data") UserJoinReq request,
                                                            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        CustomResponse<Boolean> res = new CustomResponse<>();

        verificationService.verifyPhoneVerification(request.getVerificationId()); // 휴대폰 번호 검증

        boolean
                isUserAlreadyExists =
                userCommandService.addUserAuthIfPhoneNumberExists(request); // 유저가 이미 있으면 user_auth에 추가

        if (!isUserAlreadyExists) {
            int userId = userCommandService.createIdPwUserAndSave(request);

            ArrayList<String> directoryElement = new ArrayList<String>();
            directoryElement.add("user");
            directoryElement.add(String.valueOf(userId));
            if (profileImage != null && !profileImage.isEmpty() && !s3.validateImageType(profileImage)) {
                throw new BusinessException("지원하지 않는 이미지 확장자입니다.");
            }
            String
                    imageUrl =
                    profileImage != null && !profileImage.isEmpty() ? s3.upload(profileImage,
                            directoryElement) : String.join("/",
                            Arrays.asList(s3.getS3Url(), "default_profile.png"));
            userInfoCommandService.setImageUrl(userId, imageUrl);
            Verification verification = verificationService.selectVerificationById(request.getVerificationId());
            if (verification != null) verificationService.deleteVerification(verification.getId());
            res.setData(Optional.of(true));
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping(value = "/join-apple", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<CustomResponse<Object>> joinAppleSns(@RequestPart(value = "data") AppleJoinReq request,
                                                               @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) throws IOException {
        CustomResponse<Object> res = new CustomResponse<>();
        verificationService.verifyPhoneVerification(request.getVerificationId()); // 휴대폰 번호 검증

        if (profileImage != null && !profileImage.isEmpty()) {
            if (!s3.validateImageType(profileImage))
                throw new BusinessException("지원하지 않는 이미지 확장자입니다.");
        }

        String phone = request.getPhone().replaceAll("-", "");
        Integer userId = userInfoQueryService.getAppleUserId(phone);
        if (userId != null) {
            UserAuth
                    userAuth =
                    UserAuth.builder().userId(userId).loginId(request.getLoginId()).loginType(LoginType.APPLE).build();
            userCommandService.addUserAuth(userAuth);
        } else {
            userId = userCommandService.addAppleUser(request, phone, profileImage);
        }

        Jwt jwt = generateAndSetTokens(userId);
        res.setData(Optional.of(jwt));
        return ResponseEntity.ok(res);
    }

    private Jwt generateAndSetTokens(int userId) {
        String accessToken = jwtProvider.generateAccessToken(String.valueOf(userId), TokenAuthType.USER);
        String refreshToken = jwtProvider.generateRefreshToken(String.valueOf(userId), TokenAuthType.USER);
        Jwt token = new Jwt();
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);

        return token;
    }

    @PostMapping("/mypage/update")
    public ResponseEntity<CustomResponse<UserInfoDto>> updateUser(@RequestHeader("Authorization") Optional<String> auth,
                                                                  @RequestPart(value = "data") UserUpdateReq data,
                                                                  @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        CustomResponse<UserInfoDto> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.USER), auth);
        Integer userId = tokenInfo.getId();

        UserInfo userInfo = userCommandService.selectUserInfo(userId);
        if (profileImage != null) {
            String
                    imageUrl =
                    s3.upload(profileImage,
                            new ArrayList<>(Arrays.asList("user", String.valueOf(userInfo.getUserId()))));
            userInfo.setProfileImage(imageUrl);
            userCommandService.updateUserInfo(userInfo);
        }
        if (data.getIsAgreeMarketing() != null) {
            userInfo.setIsAgreeMarketing(data.getIsAgreeMarketing());
            userCommandService.updateUserInfo(userInfo);
        }
        if (data.getName() != null) {
            String name = utils.validateString(data.getName(), 20L, "이름");
            userInfo.setName(name);
            userCommandService.updateUserInfo(userInfo);
        }
        if (data.getNickname() != null) {
            String nickname = utils.validateString(data.getNickname(), 50L, "닉네임");
            userInfo.setNickname(nickname);
            userCommandService.updateUserInfo(userInfo);
        }
        if (data.getNewPassword() != null) {
            if (data.getOldPassword() == null) throw new BusinessException("변경 전 비밀번호를 입력해주세요.");
            Optional<UserAuth> userAuth = userCommandService.findUserAuthWithIDPWType(userId);
            if (userAuth.isEmpty()) throw new BusinessException("소셜 로그인 유저입니다.");
            if (!BCrypt.checkpw(data.getOldPassword(), userAuth.get().getPassword()))
                throw new BusinessException("이전 비밀번호와 일치하지 않습니다.");
            if (!Pattern.matches(re.password, data.getNewPassword()))
                throw new BusinessException("비밀번호 형식을 확인해주세요.");
            String password = BCrypt.hashpw(data.getNewPassword(), BCrypt.gensalt());
            userAuth.get().setPassword(password);
            userCommandService.updateUserPassword(userAuth.get());
        }
        if (data.getPhone() != null) {
            if (data.getVerificationId() == null) throw new BusinessException("인증 먼저 진행해주세요.");
            if (!Pattern.matches(re.phone, data.getPhone()))
                throw new BusinessException("휴대폰 번호 형식을 확인해주세요.");
            String phone = data.getPhone().replaceAll(re.getPhone(), "0$1$2$3");
            Verification verification = verificationService.selectVerificationById(data.getVerificationId());
            if (verification.getExpiredAt() != null) throw new BusinessException("인증을 먼저 진행해주세요.");
            if (userCommandService.checkExistWithPhone(phone) &&
                    (userInfo.getPhone() == null || !userInfo.getPhone().equals(phone)))
                throw new BusinessException("이미 등록된 전화번호입니다.");
            userInfo.setPhone(phone);
            userCommandService.updateUserInfo(userInfo);
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<CustomResponse<Jwt>> loginUser(@RequestBody UserLoginReq request) {
        CustomResponse<Jwt> res = new CustomResponse<>();

        User findUser = userQueryService.login(request);

        String accessToken = jwtProvider.generateAccessToken(String.valueOf(findUser.getId()), TokenAuthType.USER);
        String
                refreshToken =
                jwtProvider.generateRefreshToken(String.valueOf(findUser.getId()), TokenAuthType.USER);
        Jwt token = new Jwt();
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        res.setData(Optional.of(token));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/mypage")
    public ResponseEntity<CustomResponse<UserInfoDto>> selectUserSelfInfo(@RequestHeader(value = "Authorization") Optional<String> auth) {
        CustomResponse<UserInfoDto> res = new CustomResponse<>();

        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.USER), auth);
        Integer userId = tokenInfo.getId();
        UserInfoDto userInfoDto = userInfoQueryService.showMyPage(userId);

        res.setData(Optional.ofNullable(userInfoDto));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/management/{id}")
    public ResponseEntity<CustomResponse<UserInfoDto>> selectUserList(@RequestHeader("Authorization") Optional<String> auth,
                                                                      @PathVariable("id") Integer id) {
        CustomResponse<UserInfoDto> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        UserInfoDto userInfoDto = userQueryService.manageByUserId(id);

        res.setData(Optional.of(userInfoDto));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/management")
    public ResponseEntity<CustomResponse<Page<UserInfoDto>>> selectUserList(@RequestHeader("Authorization") Optional<String> auth,
                                                                            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                            @RequestParam(value = "take", required = false, defaultValue = "10") Integer take,
                                                                            @RequestParam(value = "orderby", defaultValue = "joinAt") UserOrderByAdmin orderBy,
                                                                            @RequestParam(value = "orderType", defaultValue = "DESC") Sort.Direction sort,
                                                                            @RequestParam(value = "name", required = false) String name,
                                                                            @RequestParam(value = "nickname", required = false) String nickname,
                                                                            @RequestParam(value = "email", required = false) String email,
                                                                            @RequestParam(value = "phone", required = false) String phone,
                                                                            @RequestParam(value = "state", required = false) String state,
                                                                            @RequestParam(value = "loginType", required = false) String loginType,
                                                                            @RequestParam(value = "joinAtS", required = false) Timestamp joinAtS,
                                                                            @RequestParam(value = "joinAtE", required = false) Timestamp joinAtE) {
        CustomResponse<Page<UserInfoDto>> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Specification<UserInfo> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null) predicates.add(builder.like(root.get("name"), "%" + name + "%"));
            if (nickname != null) predicates.add(builder.like(root.get("nickname"), "%" + nickname + "%"));
            if (email != null) predicates.add(builder.like(root.get("email"), "%" + email + "%"));
            if (phone != null) predicates.add(builder.like(root.get("phone"), "%" + phone + "%"));
            if (state != null)
                predicates.add(builder.and(root.get("user").get("state").in(Arrays.stream(state.split(",")).map(
                        UserState::valueOf).toList())));
            if (joinAtS != null) predicates.add(builder.greaterThan(root.get("user").get("joinAt"), joinAtS));
            if (joinAtE != null) predicates.add(builder.lessThan(root.get("user").get("joinAt"), joinAtE));
            return builder.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(page, take, Sort.by(sort, orderBy.label));
        Page<UserInfoDto> userInfos = userQueryService.findPagedAndSpecificatedUserInfos(spec, pageRequest);
        res.setData(Optional.ofNullable(userInfos));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/management/update-state")
    public ResponseEntity<CustomResponse<Boolean>> updateUserState(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                   @RequestPart(value = "data") UpdateUserStateReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        if (data.getState() == null) throw new BusinessException("상태를 입력해주세요.");
        userCommandService.updateUserState(data.getUserIds(), data.getState());
        res.setData(Optional.of(true));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/verifyToken")
    public ResponseEntity<CustomResponse<String>> whoami(@RequestHeader(value = "Authorization") Optional<String> auth) {
        CustomResponse<String> res = new CustomResponse<>();

        Integer userId = null;
                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ALLOW), auth);

        res.setData(Optional.of(("" + tokenInfo.getType() + tokenInfo.getId())));
        return ResponseEntity.ok(res);
    }

    @PostMapping(value = "renew-token")
    public ResponseEntity<CustomResponse<Jwt>> renewToken(@RequestPart(value = "accessToken") String accessToken,
                                                          @RequestPart(value = "refreshToken") String refreshToken) {
        CustomResponse<Jwt> res = new CustomResponse<>();

        if (jwtProvider.isTokenExpired(refreshToken)) {
            return ResponseEntity.of((Optional<CustomResponse<Jwt>>) null);
        }

        TokenAuthType authType = jwtProvider.getTypeFromToken(refreshToken);
        Integer id = jwtProvider.getIdFromToken(refreshToken);
        accessToken = jwtProvider.generateAccessToken(String.valueOf(id), authType);
        refreshToken = jwtProvider.generateRefreshToken(String.valueOf(id), authType);

        Jwt token = new Jwt();
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        res.setData(Optional.of(token));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<CustomResponse<Boolean>> withdrawUser(@RequestHeader(value = "Authorization") Optional<String> auth) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.USER), auth);

        Integer userId = tokenInfo.getId();
        userCommandService.withdrawUser(userId);

        return ResponseEntity.ok(res);
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<CustomResponse<Boolean>> updateFcm(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                             @RequestBody UpdateFcmReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.USER), auth);

        Integer userId = tokenInfo.getId();
        if (data.getFcmToken() == null) throw new BusinessException("토큰을 입력해주세요.");
        Optional<FcmToken> token = fcmTokenRepository.findById(data.getFcmToken());
        if (data.getSet() == true) {
            if (token.isPresent()) {
                token.get().setToken(data.getFcmToken());
                fcmTokenRepository.save(token.get());
            } else {
                fcmTokenRepository.save(FcmToken.builder().token(data.getFcmToken()).userId(userId).build());
            }
        } else {
            if (token.isPresent()) {
                fcmTokenRepository.deleteById(data.getFcmToken());
            }
        }
        res.setData(Optional.of(true));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<CustomResponse<Boolean>> resetPassword(@RequestPart(value = "data") ResetPasswordReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();

        if (data.getVerificationId() == null) throw new BusinessException("인증 먼저 진행해주세요.");
        if (!Pattern.matches(re.phone, data.getPhone()))
            throw new BusinessException("휴대폰 번호 형식을 확인해주세요.");
        String phone = data.getPhone().replaceAll(re.getPhone(), "0$1$2$3");
        Verification verification = verificationService.selectVerificationById(data.getVerificationId());
        if (verification.getExpiredAt() != null) throw new BusinessException("인증을 먼저 진행해주세요.");

        String newPassword = userAuthCommandService.resetPassword(phone);
        smsService.sendSms(phone, "[바로피쉬]\n새로운 비밀번호는 " + newPassword + " 입니다.", null);

        return ResponseEntity.ok(res);
    }

    @PostMapping("/find-email")
    public ResponseEntity<CustomResponse<Boolean>> findEmail(@RequestPart(value = "data") FindEmailReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();

        if (data.getVerificationId() == null) throw new BusinessException("인증 먼저 진행해주세요.");
        if (!Pattern.matches(re.phone, data.getPhone()))
            throw new BusinessException("휴대폰 번호 형식을 확인해주세요.");
        String phone = data.getPhone().replaceAll(re.getPhone(), "0$1$2$3");
        Verification verification = verificationService.selectVerificationById(data.getVerificationId());
        if (verification.getExpiredAt() != null) throw new BusinessException("인증을 먼저 진행해주세요.");
        UserInfo userInfo = userCommandService.selectUserWithPhone(phone);
        UserAuth userAuth = userCommandService.selectUserAuth(userInfo.getUserId());
        if (!userAuth.getLoginType().equals(LoginType.IDPW)) throw new BusinessException("소셜 가입 사용자입니다.");
        smsService.sendSms(phone, String.format("[바로피쉬]\n회원님의 아이디는\n%s 입니다.", userAuth.getLoginId()), null);
        res.setData(Optional.of(true));
        return ResponseEntity.ok(res);
    }


    // ---------------------------결제수단 관리------------------------------------
    @GetMapping("/payment-method")
    public ResponseEntity<CustomResponse<List<PaymentMethodDto>>> selectPaymentMethodList(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                                          @RequestParam(value = "userId", required = false) Integer userId) {
        CustomResponse<List<PaymentMethodDto>> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN, TokenAuthType.USER), auth);

        if (tokenInfo.getType().equals(TokenAuthType.USER)) {
            userId = tokenInfo.getId();
        }
        if (userId == null) throw new BusinessException("유저 아이디를 입력해주세요.");

        Optional<List<PaymentMethodDto>> paymentMethods = paymentMethodQueryService.getPaymentMethods(userId);
        res.setData(paymentMethods);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/payment-method/{id}")
    public ResponseEntity<CustomResponse<PaymentMethodDto>> selectPaymentMethod(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                                @PathVariable("id") Integer id) {
        CustomResponse<PaymentMethodDto> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN, TokenAuthType.USER), auth);

        PaymentMethod paymentMethod = paymentMethodQueryService.selectPaymentMethod(id);
        if (tokenInfo.getType().equals(TokenAuthType.USER) &&
                paymentMethod.getUserId() != tokenInfo.getId())
            throw new BusinessException("타유저의 결제 수단입니다.");
        res.setData(Optional.ofNullable(paymentMethodQueryService.convert2Dto(paymentMethod)));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/payment-method/add")
    public ResponseEntity<CustomResponse<PaymentMethodDto>> addPaymentMethod(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                             @RequestPart(value = "data") AddPaymentMethodReq data) {
        CustomResponse<PaymentMethodDto> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.USER), auth);
        Integer userId = tokenInfo.getId();

        PaymentMethod paymentMethod = paymentMethodCommandService.addPaymentMethod(data, tokenInfo.getId());

        res.setData(Optional.ofNullable(paymentMethodQueryService.convert2Dto(paymentMethod)));
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("payment-method/{id}")
    public ResponseEntity<CustomResponse<Boolean>> deletePaymentMethod(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                       @PathVariable("id") Integer id) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.USER), auth);

        PaymentMethod paymentMethod = paymentMethodQueryService.selectPaymentMethod(id);
        if (paymentMethod.getUserId() != tokenInfo.getId())
            throw new BusinessException("타계정의 결제수단입니다.");
        paymentMethodQueryService.deletePaymentMethod(id);
        return ResponseEntity.ok(res);
    }
}
