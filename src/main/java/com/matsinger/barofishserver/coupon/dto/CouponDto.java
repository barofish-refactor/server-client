package com.matsinger.barofishserver.coupon.dto;

import com.matsinger.barofishserver.coupon.domain.CouponPublicType;
import com.matsinger.barofishserver.coupon.domain.CouponState;
import com.matsinger.barofishserver.coupon.domain.CouponType;
import com.matsinger.barofishserver.user.dto.UserDto;
import com.matsinger.barofishserver.userinfo.dto.UserInfoDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDto {
    private int id;
    private CouponState state;
    private CouponPublicType publicType;
    private String title;
    private CouponType type;
    private Integer amount;
    private Timestamp startAt;
    private Timestamp endAt;
    private Integer minPrice;
    private List<UserInfoDto> users;
}
