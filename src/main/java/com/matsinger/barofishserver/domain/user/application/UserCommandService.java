package com.matsinger.barofishserver.domain.user.application;

import com.matsinger.barofishserver.domain.basketProduct.application.BasketCommandService;
import com.matsinger.barofishserver.domain.basketProduct.application.BasketQueryService;
import com.matsinger.barofishserver.domain.basketProduct.domain.BasketProductInfo;
import com.matsinger.barofishserver.domain.basketProduct.dto.BasketProductDto;
import com.matsinger.barofishserver.domain.basketProduct.repository.BasketProductInfoRepository;
import com.matsinger.barofishserver.domain.basketProduct.repository.BasketProductOptionRepository;
import com.matsinger.barofishserver.domain.compare.domain.CompareSet;
import com.matsinger.barofishserver.domain.compare.repository.CompareItemRepository;
import com.matsinger.barofishserver.domain.compare.repository.CompareSetRepository;
import com.matsinger.barofishserver.domain.compare.repository.SaveProductRepository;
import com.matsinger.barofishserver.domain.coupon.application.CouponCommandService;
import com.matsinger.barofishserver.domain.coupon.domain.Coupon;
import com.matsinger.barofishserver.domain.coupon.repository.CouponUserMapRepository;
import com.matsinger.barofishserver.domain.grade.application.GradeQueryService;
import com.matsinger.barofishserver.domain.grade.domain.Grade;
import com.matsinger.barofishserver.domain.grade.repository.GradeRepository;
import com.matsinger.barofishserver.domain.inquiry.application.InquiryCommandService;
import com.matsinger.barofishserver.domain.inquiry.repository.InquiryRepository;
import com.matsinger.barofishserver.domain.notification.application.NotificationCommandService;
import com.matsinger.barofishserver.domain.notification.dto.NotificationMessage;
import com.matsinger.barofishserver.domain.notification.dto.NotificationMessageType;
import com.matsinger.barofishserver.domain.notification.repository.NotificationRepository;
import com.matsinger.barofishserver.order.domain.model.Orders;
import com.matsinger.barofishserver.order.domain.repository.OrderProductInfoRepository;
import com.matsinger.barofishserver.order.domain.repository.OrderDeliverPlaceRepository;
import com.matsinger.barofishserver.order.domain.repository.OrderRepository;
import com.matsinger.barofishserver.domain.payment.domain.Payments;
import com.matsinger.barofishserver.domain.payment.repository.PaymentRepository;
import com.matsinger.barofishserver.domain.report.repository.ReportRepository;
import com.matsinger.barofishserver.domain.review.application.ReviewCommandService;
import com.matsinger.barofishserver.domain.review.repository.ReviewEvaluationRepository;
import com.matsinger.barofishserver.domain.review.repository.ReviewLikeRepository;
import com.matsinger.barofishserver.domain.review.repository.ReviewRepository;
import com.matsinger.barofishserver.domain.siteInfo.application.SiteInfoQueryService;
import com.matsinger.barofishserver.domain.siteInfo.domain.SiteInformation;
import com.matsinger.barofishserver.domain.user.deliverplace.DeliverPlace;
import com.matsinger.barofishserver.domain.user.deliverplace.repository.DeliverPlaceRepository;
import com.matsinger.barofishserver.domain.user.domain.User;
import com.matsinger.barofishserver.domain.user.domain.UserState;
import com.matsinger.barofishserver.domain.user.dto.AppleJoinReq;
import com.matsinger.barofishserver.domain.user.dto.SnsJoinLoginResponseDto;
import com.matsinger.barofishserver.domain.user.dto.SnsJoinReq;
import com.matsinger.barofishserver.domain.user.dto.UserJoinReq;
import com.matsinger.barofishserver.domain.user.exception.UserException;
import com.matsinger.barofishserver.domain.user.paymentMethod.repository.PaymentMethodRepository;
import com.matsinger.barofishserver.domain.user.repository.UserRepository;
import com.matsinger.barofishserver.domain.userauth.application.UserAuthCommandService;
import com.matsinger.barofishserver.domain.userauth.domain.LoginType;
import com.matsinger.barofishserver.domain.userauth.domain.UserAuth;
import com.matsinger.barofishserver.domain.userauth.repository.UserAuthRepository;
import com.matsinger.barofishserver.domain.userinfo.application.UserInfoCommandService;
import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import com.matsinger.barofishserver.domain.userinfo.dto.UserInfoDto;
import com.matsinger.barofishserver.domain.userinfo.repository.UserInfoRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.S3.Uploader;
import com.matsinger.barofishserver.utils.fcm.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserCommandService {
    private final UserRepository userRepository;

    private final UserInfoRepository userInfoRepository;
    private final UserAuthRepository userAuthRepository;
    private final DeliverPlaceRepository deliverPlaceRepository;
    private final GradeRepository gradeRepository;
    private final BasketQueryService basketQueryService;
    private final BasketCommandService basketCommandService;
    private final UserException userJoinValidator;
    private final ReviewCommandService reviewService;
    private final InquiryCommandService inquiryCommandService;
    private final UserAuthCommandService userAuthCommandService;
    private final UserInfoCommandService userInfoCommandService;
    private final FcmTokenRepository fcmTokenRepository;
    private final BasketProductInfoRepository basketProductInfoRepository;
    private final BasketProductOptionRepository basketProductOptionRepository;
    private final NotificationRepository notificationRepository;
    private final ReportRepository reportRepository;
    private final CompareItemRepository compareItemRepository;
    private final CompareSetRepository compareSetRepository;
    private final SaveProductRepository saveProductRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderProductInfoRepository orderProductInfoRepository;
    private final OrderDeliverPlaceRepository orderDeliverPlaceRepository;
    private final InquiryRepository inquiryRepository;
    private final ReviewEvaluationRepository reviewEvaluationRepository;
    private final ReviewRepository reviewRepository;
    private final CouponUserMapRepository couponUserMapRepository;
    private final SiteInfoQueryService siteInfoQueryService;
    private final GradeQueryService gradeQueryService;
    private final Uploader uploader;
    private final Common utils;
    private final CouponCommandService couponCommandService;
    private final NotificationCommandService notificationCommandService;

    @Transactional
    public SnsJoinLoginResponseDto createSnsUserAndSave(SnsJoinReq request) {

        userJoinValidator.nullCheck(request.getLoginType());
        userJoinValidator.nullCheck(request.getLoginId());

        User user = User.builder().joinAt(new Timestamp(System.currentTimeMillis())).state(UserState.ACTIVE).build();
        userRepository.save(user);

        UserAuth createdUserAuth = userAuthCommandService.createUserAuth(request, user);
        Grade grade = gradeRepository.findById(1).orElseThrow(() -> new BusinessException("등급 정보를 찾을 수 없습니다."));
        UserInfo createdUserInfo = userInfoCommandService.createAndSaveUserInfo(user, request, "", grade);

        Coupon publishedCoupon = couponCommandService.publishNewUserCoupon(user.getId());

        notificationCommandService.sendFcmToUser(
                user.getId(),
                NotificationMessageType.COUPON_ARRIVED,
                NotificationMessage.builder()
                        .couponName(publishedCoupon.getTitle())
                        .userName(createdUserInfo.getNickname())
                        .build());

        return SnsJoinLoginResponseDto.builder().userId(user.getId()).loginId(createdUserAuth.getLoginId()).build();
    }

    @Transactional
    public int createIdPwUserAndSave(UserJoinReq request) {
        User
                createdUser =
                User.builder().state(UserState.ACTIVE).joinAt(new Timestamp(System.currentTimeMillis())).build();
        userRepository.save(createdUser);

        userAuthCommandService.createIdPwUserAuthAndSave(createdUser, request);
        Grade
                findGrade =
                gradeRepository.findById(1).orElseThrow(() -> new BusinessException("등급 정보를 찾을 수 없습니다."));
        UserInfo createdUserInfo = userInfoCommandService.createAndSaveIdPwUserInfo(createdUser, request, findGrade);

        setAndSaveDeliverPlace(createdUser, createdUserInfo, request);

        Coupon publishedCoupon = couponCommandService.publishNewUserCoupon(createdUser.getId());

        notificationCommandService.sendFcmToUser(
                createdUserInfo.getUserId(),
                NotificationMessageType.COUPON_ARRIVED,
                NotificationMessage.builder()
                        .couponName(publishedCoupon.getTitle())
                        .userName(createdUserInfo.getNickname())
                        .build());

        return createdUser.getId();
    }

    @Transactional
    public int addAppleUser(AppleJoinReq request, String phoneNumber, MultipartFile profileImage) {

        utils.validateString(request.getName(), 20L, "이름");
        utils.validateString(request.getNickname(), 50L, "닉네임");

        User savedUser = userRepository.save(request.toUserEntity());

        userAuthRepository.save(request.toUserAuthEntity(savedUser));

        SiteInformation siteInformation = siteInfoQueryService.selectSiteInfo("INT_JOIN_POINT");
        int point = Integer.parseInt(siteInformation.getContent());
        Grade grade = gradeQueryService.selectGrade(1);
        UserInfo createdUserInfo = userInfoRepository.save(request.toUserInfoEntity(savedUser, "", phoneNumber, point, grade));

        deliverPlaceRepository.save(request.toDeliveryPlaceEntity(savedUser, phoneNumber));

        ArrayList<String> directoryElement = new ArrayList<>(Arrays.asList("user", String.valueOf(savedUser.getId())));
        String profileImageUrl = uploader.getS3Url() + "/default_profile.png";
        if (profileImage != null) {
            profileImageUrl = uploader.upload(profileImage, directoryElement);
            userInfoCommandService.setImageUrl(savedUser.getId(), profileImageUrl);
            return savedUser.getId();
        }

        userInfoCommandService.setImageUrl(savedUser.getId(), profileImageUrl);

        Coupon publishedCoupon = couponCommandService.publishNewUserCoupon(savedUser.getId());

        notificationCommandService.sendFcmToUser(
                createdUserInfo.getUserId(),
                NotificationMessageType.COUPON_ARRIVED,
                NotificationMessage.builder()
                        .couponName(publishedCoupon.getTitle())
                        .userName(createdUserInfo.getNickname())
                        .build());

        return savedUser.getId();
    }

    public DeliverPlace setAndSaveDeliverPlace(User user, UserInfo userInfo, UserJoinReq request) {

        String address = utils.validateString(request.getAddress(), 100L, "주소");
        String addressDetail = utils.validateString(request.getAddressDetail(), 100L, "상세 주소");

        if (request.getPostalCode() == null) {
            throw new BusinessException("우편 번호를 입력해주세요.");
        }

        DeliverPlace
                createdDeliver =
                DeliverPlace.builder().userId(user.getId()).name(userInfo.getName()).receiverName(userInfo.getName()).tel(
                        userInfo.getPhone()).address(address).addressDetail(addressDetail).deliverMessage("").postalCode(
                        request.getPostalCode()).isDefault(true).bcode(request.getBcode()).build();

        return deliverPlaceRepository.save(createdDeliver);
    }

    public void withdrawUser(int userId) {
        User findUser = userRepository.findById(userId).orElseThrow(() -> {
            throw new BusinessException("유저를 찾을 수 없습니다.");
        });
        UserInfo findUserInfo = userInfoRepository.findByUserId(userId).orElseThrow(() -> {
            throw new BusinessException("유저를 찾을 수 없습니다.");
        });
        findUser.setState(UserState.DELETED);
        findUser.setWithdrawAt(utils.now());
//        findUserInfo.setNickname("탈퇴한 회원");

        // 장바구니 제거
        List<Integer>
                basketIds =
                basketQueryService.selectBasketList(userId).stream().map(BasketProductDto::getId).toList();
        basketCommandService.deleteBasket(basketIds);
        // 리뷰제거
        reviewService.deleteReviewsByUserId(userId);
        inquiryCommandService.deleteInquiryByUserId(userId);
        userRepository.save(findUser);
    }

    public Page<UserInfoDto> selectUserInfoList(PageRequest pageRequest, Specification<UserInfo> spec) {
        return userInfoRepository.findAll(spec, pageRequest).map(userInfo -> {
            UserInfoDto userInfoDto = userInfo.convert2Dto();
            userInfoDto.setUser(selectUser(userInfo.getUserId()).convert2Dto());
            UserAuth userAuth = selectUserAuth(userInfo.getUserId());
            if (userAuth != null) {
                userAuth.setPassword(null);
                userInfoDto.setAuth(userAuth.convert2Dto());
            }
            List<DeliverPlace> deliverPlaces = selectUserDeliverPlaceList(userInfo.getUserId());
            userInfoDto.setDeliverPlaces(deliverPlaces);
            return userInfoDto;
        });
    }

    public DeliverPlace selectDeliverPlace(Integer id) {
        return deliverPlaceRepository.findById(id).orElseThrow(() -> {
            throw new BusinessException("배송지 정보를 찾을 수 없습니다.");
        });
    }

    public Boolean checkExistWithPhone(String phone) {
        return userInfoRepository.existsByPhone(phone);
    }

    public Boolean checkExistWithNickname(String nickname) {
        return userInfoRepository.existsByNickname(nickname);
    }

    public Optional<User> selectUserOptional(Integer id) {
        try {
            return userRepository.findById(id);
        } catch (Exception e) {
            return null;
        }
    }

    public List<User> selectUserListWithIds(List<Integer> ids) {
        return userRepository.findAllByIdIn(ids);
    }

    public UserInfo selectUserInfo(Integer id) {
        return userInfoRepository.findById(id).orElseThrow(() -> {
            throw new BusinessException("유저 정보를 찾을 수 없습니다.");
        });
    }

    public Optional<UserInfo> selectOptionalUserInfo(Integer id) {
        return userInfoRepository.findById(id);
    }

    public UserAuth selectUserAuth(Integer id) {
        return userAuthRepository.findFirstByUserId(id);
    }

    public Optional<UserAuth> findUserAuthWithIDPWType(Integer id) {
        return userAuthRepository.findByLoginTypeAndUserId(LoginType.IDPW, id);
    }

    public List<User> selectUserList() {
        return userRepository.findAll();
    }

    public User selectUser(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> {
            throw new BusinessException("유저 정보를 찾을 수 없습니다.");
        });
    }

    public User createSnsUserAndSave(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void addUser(User user, UserAuth userAuth, UserInfo userInfo, DeliverPlace deliverPlace) {
        User res = userRepository.save(user);
        userAuth.setUserId(res.getId());
        userInfo.setUserId(res.getId());
        userInfo.setUser(res);
        deliverPlace.setUserId(res.getId());
        userAuthRepository.save(userAuth);
        UserInfo result = userInfoRepository.save(userInfo);
        deliverPlaceRepository.save(deliverPlace);
    }

    public UserAuth selectUserByLoginId(LoginType loginType, String loginId) {
        try {
            return userAuthRepository.findByLoginTypeAndLoginId(loginType, loginId).orElseThrow();
        } catch (Exception e) {
            return null;
        }
    }

    public UserAuth checkUserAuthExist(LoginType loginType, String loginId) {
        return userAuthRepository.findByLoginTypeAndLoginId(loginType, loginId).orElseThrow(() -> {
            throw new BusinessException("유저를 찾을 수 없습니다.");
        });
    }

    public List<DeliverPlace> selectUserDeliverPlaceList(Integer userId) {
        return deliverPlaceRepository.findAllByUserId(userId);
    }

    public UserAuth addUserAuth(UserAuth userAuth) {
        return userAuthRepository.save(userAuth);
    }

    public void addUserInfo(UserInfo userInfo) {
        userInfoRepository.save(userInfo);
    }

    public void updateUserState(List<Integer> userIds, UserState state) {
        List<User> users = userRepository.findAllById(userIds);
        for (User user : users) {
            user.setState(state);
        }
        users = userRepository.saveAll(users);
    }

    public void updateUserInfo(UserInfo userInfo) {
        userInfoRepository.save(userInfo);
    }

    public void updateUserPassword(UserAuth userAuth) {
        userAuthRepository.save(userAuth);
    }

    public List<Grade> selectGradeList() {
        return gradeRepository.findAll();
    }

    public Grade selectGrade(Integer id) {
        return gradeRepository.findById(id).orElseThrow(() -> {
            throw new BusinessException("등급 정보를 찾을 수 없습니다.");
        });
    }

    public void addPoint(UserInfo userInfo, Integer point) {
        userInfo.setPoint(userInfo.getPoint() + point);
        userInfoRepository.save(userInfo);
    }

    public List<User> selectUserWithState(UserState state) {
        return userRepository.findAllByState(state);
    }

    public UserInfo selectUserWithPhone(String phone) {
        return userInfoRepository.findByPhone(phone).orElseThrow(() -> {
            throw new BusinessException("휴대폰 번호를 찾을 수 없습니다.");
        });
    }

    public Optional<UserInfo> selectOptionalUserInfo(int userId) {
        return userInfoRepository.findByUserId(userId);
    }

    @Transactional
    public void addUserAuthIfPhoneNumberExists(SnsJoinReq request) {
        if (request.getLoginType().equals(LoginType.APPLE)) return;

        Optional<UserInfo> optionalUserInfo = userInfoRepository.findByPhone(request.getPhone().replace("-", ""));

        boolean isUserInfoExists = optionalUserInfo.isPresent();

        boolean isLoginTypeExists = false;
        if (isUserInfoExists) {
            UserInfo findUserInfo = optionalUserInfo.get();
            List<UserAuth> existingUserAuth = findUserInfo.getUser().getUserAuth();

            for (UserAuth userAuth : existingUserAuth) {
                if (userAuth.getLoginType() == request.getLoginType()) {
                    isLoginTypeExists = true;
                }
                if (userAuth.getUser().getState() == UserState.DELETED) {
//                    throw new BusinessException("탈퇴한 유저입니다.");
                    userAuth.getUser().setState(UserState.ACTIVE);
                    userAuth.getUser().setWithdrawAt(null);
                }
                if (userAuth.getUser().getState() == UserState.BANNED) {
                    throw new BusinessException("운영 정책상 비활성된 유저입니다.");
                }
            }
        }

        if (isUserInfoExists && !isLoginTypeExists) {
            UserAuth
                    createdUserAuth =
                    UserAuth.builder()
                            .loginType(request.getLoginType())
                            .loginId(request.getLoginId())
                            .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                            .build();
            User findUser = optionalUserInfo.get().getUser();
            createdUserAuth.setUserId(findUser.getId());
            createdUserAuth.setUser(findUser);

            userAuthRepository.save(createdUserAuth);
        }
    }

    @Transactional
    public boolean addUserAuthIfPhoneNumberExists(UserJoinReq request) {

        Optional<UserInfo> optionalUserInfo = userInfoRepository.findByPhone(request.getPhone().replace("-", ""));

        if (!optionalUserInfo.isPresent()) {
            return false;
        }

        UserInfo findUserInfo = optionalUserInfo.get();
        List<UserAuth> existingUserAuth = findUserInfo.getUser().getUserAuth();

        for (UserAuth userAuth : existingUserAuth) {
            if (userAuth.getLoginType() == LoginType.IDPW) {
                throw new BusinessException("회원가입된 이력이 있습니다");
            }
            if (userAuth.getUser().getState() == UserState.DELETED) {
//                throw new BusinessException("탈퇴한 유저입니다.");
                userAuth.getUser().setState(UserState.ACTIVE);
                userAuth.getUser().setWithdrawAt(null);
            }
            if (userAuth.getUser().getState() == UserState.BANNED) {
                throw new BusinessException("운영 정책상 비활성된 유저입니다.");
            }
        }

        userAuthCommandService.createIdPwUserAuthAndSave(findUserInfo.getUser(), request);

        return true;
    }

    public List<UserInfo> selectUserInfoListWithIds(List<Integer> userIds) {
        return userInfoRepository.findAllByUserIdIn(userIds);
    }

    @Transactional
    public void deleteWithdrawUserList() {
        Timestamp ts = utils.now();
        Calendar cal = Calendar.getInstance();
        cal.setTime(ts);

        cal.add(Calendar.YEAR, -1);
        List<User> users = userRepository.findAllByWithdrawAtBefore(new Timestamp(cal.getTime().getTime()));
        List<Integer> userIds = users.stream().map(User::getId).toList();
        //fcmToken
        fcmTokenRepository.deleteAllByUserIdIn(userIds);
        //deliverPlace
        deliverPlaceRepository.deleteAllByUserIdIn(userIds);
        //userInfo
        userInfoRepository.deleteAllByUserIdIn(userIds);
        //userAuth
        userAuthRepository.deleteAllByUserIdIn(userIds);
        //basketProductOption
        List<BasketProductInfo> basketProductInfos = basketProductInfoRepository.findAllByUserIdIn(userIds);
        basketProductOptionRepository.deleteAllByOrderProductIdIn(basketProductInfos.stream().map(BasketProductInfo::getId).toList());
        //basketProductInfo
        basketProductInfoRepository.deleteAllByIdIn(basketProductInfos.stream().map(BasketProductInfo::getId).toList());
        //notification
        notificationRepository.deleteAllByUserIdIn(userIds);
        //report
        reportRepository.deleteAllByUserIdIn(userIds);
        //compareItem
        List<CompareSet> compareSets = compareSetRepository.findAllByUserIdIn(userIds);
        compareItemRepository.deleteAllByCompareSetIdIn(compareSets.stream().map(CompareSet::getId).toList());
        //compareSet
        compareSetRepository.deleteAllByUserIdIn(userIds);
        //saveProduct
        saveProductRepository.deleteAllByUserIdIn(userIds);
        //paymentMethod
        paymentMethodRepository.deleteAllByUserIdIn(userIds);
        //reviewLike
        reviewLikeRepository.deleteAllByUserIdIn(userIds);
        //payment
        List<Orders> orders = orderRepository.findAllByUserIdIn(userIds);
        List<Payments> payments = paymentRepository.findAllByOrderIdIn(orders.stream().map(Orders::getId).toList());
        paymentRepository.saveAll(payments.stream().peek(v -> v.setOrderId(null)).toList());
        //orderProductInfo
        orderProductInfoRepository.deleteAllByOrderIdIn(orders.stream().map(Orders::getId).toList());
        //orderDeliverPlace
        orderDeliverPlaceRepository.deleteAllByOrderIdIn(orders.stream().map(Orders::getId).toList());
        //orders
        orderRepository.deleteAllByIdIn(orders.stream().map(Orders::getId).toList());
        //inquiry
        inquiryRepository.deleteAllByUserIdIn(userIds);
        //reviewEvaluation
        reviewEvaluationRepository.deleteAllByReview_UserIdIn(userIds);
        //review
        reviewRepository.deleteAllByUserIdIn(userIds);
        //couponUserMap
        couponUserMapRepository.deleteAllByUserIdIn(userIds);
        //user
        userRepository.findAllByIdIn(userIds);
    }
}
