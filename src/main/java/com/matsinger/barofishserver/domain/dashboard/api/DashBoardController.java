package com.matsinger.barofishserver.domain.dashboard.api;

import com.matsinger.barofishserver.domain.dashboard.application.DashBoardService;
import com.matsinger.barofishserver.domain.dashboard.dto.DashBoardType;
import com.matsinger.barofishserver.domain.dashboard.dto.DashBoard;
import com.matsinger.barofishserver.domain.dashboard.dto.ProductRankDto;
import com.matsinger.barofishserver.domain.inquiry.dto.InquiryDto;
import com.matsinger.barofishserver.jwt.JwtService;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.utils.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
public class DashBoardController {
    private final JwtService jwt;
    private final DashBoardService dashBoardService;


    @GetMapping("")
    public ResponseEntity<CustomResponse<DashBoard>> selectDashBoard(@RequestHeader(value = "Authorization") Optional<String> auth) {
        CustomResponse<DashBoard> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN, TokenAuthType.PARTNER), auth);

        Integer storeId = null;
        if (tokenInfo.getType().equals(TokenAuthType.PARTNER)) storeId = tokenInfo.getId();
        Integer dailyJoinCount = storeId == null ? dashBoardService.selectJoinCount(DashBoardType.DAILY) : null;
        List<InquiryDto> inquiries = new ArrayList<>();
        if (storeId == null) inquiries = dashBoardService.selectInquiryList(null);
        else inquiries = dashBoardService.selectInquiryList(storeId);
        DashBoardService.DashBoardOrderAggregation
                dailyOrderAggregation =
                dashBoardService.getOrderCount(DashBoardType.DAILY, storeId);
        Integer dailyOrderCount = dailyOrderAggregation.getCount();
        Integer dailyOrderAmount = dailyOrderAggregation.getAmount();
        List<OrderProductInfo> orderSituation = null;
        Integer monthlyJoinCount = null;
        if (storeId == null) monthlyJoinCount = dashBoardService.selectJoinCount(DashBoardType.MONTHLY);
        DashBoardService.DashBoardOrderAggregation
                monthlyOrderAggregation =
                dashBoardService.getOrderCount(DashBoardType.MONTHLY, storeId);
        Integer monthlyOrderCount = monthlyOrderAggregation.getCount();
        Integer monthlyOrderAmount = monthlyOrderAggregation.getAmount();
        List<ProductRankDto>
                dailyMostSoldProduct =
                dashBoardService.getProductRankList(DashBoardType.DAILY, storeId);
        List<ProductRankDto>
                monthlyMostSoldProduct =
                dashBoardService.getProductRankList(DashBoardType.MONTHLY, storeId);
        res.setData(Optional.ofNullable(DashBoard.builder().dailyJoinCount(dailyJoinCount).inquiries(inquiries).dailyOrderCount(
                dailyOrderCount).dailyOrderAmount(dailyOrderAmount).monthlyOrderCount(monthlyOrderCount).monthlyJoinCount(
                monthlyJoinCount).monthlyOrderAmount(monthlyOrderAmount).dailyMostSoldProduct(dailyMostSoldProduct).monthlyMostSoldProduct(
                monthlyMostSoldProduct).build()));
        return ResponseEntity.ok(res);
    }
}
