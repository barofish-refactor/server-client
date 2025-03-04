package com.matsinger.barofishserver.domain.coupon.application;

import com.matsinger.barofishserver.domain.coupon.domain.Coupon;
import com.matsinger.barofishserver.domain.coupon.domain.CouponUserMap;
import com.matsinger.barofishserver.domain.coupon.domain.CouponUserMapId;
import com.matsinger.barofishserver.domain.coupon.dto.CouponDeleteRequest;
import com.matsinger.barofishserver.domain.coupon.repository.CouponRepository;
import com.matsinger.barofishserver.domain.coupon.repository.CouponUserMapRepository;
import com.matsinger.barofishserver.domain.notification.application.NotificationCommandService;
import com.matsinger.barofishserver.domain.notification.dto.NotificationMessage;
import com.matsinger.barofishserver.domain.notification.dto.NotificationMessageType;
import com.matsinger.barofishserver.order.domain.repository.OrderProductInfoRepository;
import com.matsinger.barofishserver.domain.user.domain.User;
import com.matsinger.barofishserver.domain.user.domain.UserState;
import com.matsinger.barofishserver.domain.user.repository.UserRepository;
import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import com.matsinger.barofishserver.domain.userinfo.repository.UserInfoRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.utils.Common;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class CouponCommandService {
    private final CouponUserMapRepository couponUserMapRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final NotificationCommandService notificationCommandService;
    private final OrderProductInfoRepository infoRepository;
    private final Common utils;
    private final UserInfoRepository userInfoRepository;
    private final CouponQueryService couponQueryService;
    private final CouponUserMapCommandService couponUserMapCommandService;

    public void downloadCoupon(Integer userId, Integer couponId) {
        couponUserMapRepository.save(CouponUserMap.builder().couponId(couponId).userId(userId).isUsed(false).build());
    }

    public Coupon addCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    public void updateCoupon(Coupon coupon) {
        couponRepository.save(coupon);
    }

    public void deleteCoupon(Integer id) {
        couponRepository.deleteById(id);
    }

    public void sendCouponCreateNotification(Coupon coupon, List<Integer> userIds) {
        List<User>
                users =
                userIds == null ? userRepository.findAllByState(UserState.ACTIVE) : userRepository.findAllByIdIn(userIds);;
        for (User user : users) {
            Optional<UserInfo> userInfo = userInfoRepository.findByUserId(user.getId());
            if (userInfo.isPresent()) notificationCommandService.sendFcmToUser(
                    user.getId(),
                    NotificationMessageType.COUPON_ARRIVED,
                    NotificationMessage.builder()
                            .couponName(coupon.getTitle())
                            .userName(userInfo.get().getNickname())
                            .build()
            );
        }
    }

    public void useCouponV1(Integer couponId, Integer userId) {
        if (couponId == null) {
            return;
        }
        Optional<CouponUserMap> map = couponUserMapRepository.findByUserIdAndCouponId(userId, couponId);
        map.ifPresent(couponUserMap -> {
            couponUserMap.setIsUsed(true);
            couponUserMapRepository.save(couponUserMap);
        });
    }

    public void unUseCoupon(Integer couponId, Integer userId) {
        Optional<CouponUserMap> map = couponUserMapRepository.findById(new CouponUserMapId(userId, couponId));
        map.ifPresent(couponUserMap -> {
            couponUserMap.setIsUsed(false);
            couponUserMapRepository.save(couponUserMap);
        });
    }

    public void publishSystemCoupon(Integer userId) throws Exception {
        Tuple countData = infoRepository.countFinalConfirmedOrderWithUserId(userId);
        int count = Integer.parseInt(countData.get("count").toString());
        Integer couponId = null;
        if (count == 1) couponId = 1;
        else if (count == 2) couponId = 2;
        else if (count == 3) couponId = 3;
        if (count > 3) {
            return;
        }

        Optional<CouponUserMap> couponUserMapOptional = couponUserMapRepository.findById(new CouponUserMapId(userId, couponId));

        if (couponUserMapOptional.isEmpty()) {
            Coupon coupon = couponRepository.findById(couponId).orElseThrow(() -> new Exception("쿠폰 정보를 찾을 수 없습니다."));
            CouponUserMap
                    couponUserMap =
                    CouponUserMap.builder().couponId(couponId).userId(userId).isUsed(false).build();

            couponUserMapRepository.findById(new CouponUserMapId(userId, couponId));

            couponUserMapRepository.save(couponUserMap);

            UserInfo findUserInfo = userInfoRepository.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException("유저 정보를 찾을 수 없습니다."));

            notificationCommandService.sendFcmToUser(
                    findUserInfo.getUserId(),
                    NotificationMessageType.COUPON_ARRIVED,
                    NotificationMessage.builder()
                            .couponName(coupon.getTitle())
                            .userName(findUserInfo.getNickname())
                            .build());
        }
    }

    public Coupon publishNewUserCoupon(Integer userId) {
        Coupon signUpCoupon = couponRepository.findById(7)
                .orElseThrow(() -> new BusinessException("회원가입 쿠폰을 찾을 수 없습니다."));

        if (couponUserMapRepository.findById(
                new CouponUserMapId(userId, signUpCoupon.getId())).isPresent()) {
            throw new BusinessException("이미 회원가입 쿠폰을 발급 받으셨습니다.");
        }
        CouponUserMap userSignUpCoupon = CouponUserMap.builder()
                .userId(userId)
                .couponId(signUpCoupon.getId())
                .isUsed(false)
                .build();

        couponUserMapRepository.save(userSignUpCoupon);

        return signUpCoupon;
    }

    public void addCouponUserMapList(List<CouponUserMap> couponUserMaps) {
        couponUserMapRepository.saveAll(couponUserMaps);
    }

    @Transactional
    public Coupon useCoupon(Integer userId, Integer couponId) {
        if (couponId == null) {
            return null;
        }

        Coupon coupon = couponQueryService.findById(couponId);
        coupon.isAvailable(coupon.getMinPrice());
        coupon.checkExpiration();

        couponUserMapCommandService.useCoupon(userId, couponId);
        return coupon;
    }

    @Transactional
    public void deleteUserCoupon(CouponDeleteRequest request) {
        List<CouponUserMap> userCoupons = couponUserMapRepository
                .findByUserIdAndCouponIdIn(request.getUserId(), request.getCouponIds());
        userCoupons.forEach(v -> v.setIsUsed(true));

        couponUserMapRepository.saveAll(userCoupons);
    }
}
