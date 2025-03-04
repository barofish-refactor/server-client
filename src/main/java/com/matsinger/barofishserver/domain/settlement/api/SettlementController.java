package com.matsinger.barofishserver.domain.settlement.api;

import com.amazonaws.util.IOUtils;
import com.matsinger.barofishserver.domain.admin.log.application.AdminLogCommandService;
import com.matsinger.barofishserver.domain.admin.log.application.AdminLogQueryService;
import com.matsinger.barofishserver.domain.admin.log.domain.AdminLog;
import com.matsinger.barofishserver.domain.admin.log.domain.AdminLogType;
import com.matsinger.barofishserver.order.application.service.OrderService;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.order.application.dto.OrderProductInfoDto;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.settlement.application.SettlementCommandService;
import com.matsinger.barofishserver.domain.settlement.application.SettlementExcelService;
import com.matsinger.barofishserver.domain.settlement.application.SettlementQueryService;
import com.matsinger.barofishserver.domain.settlement.domain.OrderProductInfoOrderBy;
import com.matsinger.barofishserver.domain.settlement.domain.Settlement;
import com.matsinger.barofishserver.domain.settlement.domain.SettlementOrderBy;
import com.matsinger.barofishserver.domain.settlement.domain.SettlementState;
import com.matsinger.barofishserver.domain.settlement.dto.*;
import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.jwt.JwtService;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.CustomResponse;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/settlement")
public class SettlementController {
    private final SettlementQueryService settlementQueryService;
    private final SettlementCommandService settlementCommandService;
    private final ProductService productService;
    private final OrderService orderService;
    private final StoreService storeService;
    private final JwtService jwt;
    private final Common utils;
    private final AdminLogCommandService adminLogCommandService;
    private final AdminLogQueryService adminLogQueryService;
    private final SettlementExcelService settlementExcelService;


    @GetMapping("/")
    ResponseEntity<CustomResponse<SettlementAmountRes>> selectSettlementAmount(@RequestHeader(value = "Authorization") Optional<String> auth) {
        CustomResponse<SettlementAmountRes> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN, TokenAuthType.PARTNER), auth);

        Integer storeId = tokenInfo.getType().equals(TokenAuthType.PARTNER) ? tokenInfo.getId() : null;
        Integer settledAmount = settlementQueryService.getSettlementAmount(storeId, true);
        Integer needSettleAmount = settlementQueryService.getSettlementAmount(storeId, false);
        res.setData(Optional.ofNullable(SettlementAmountRes.builder().settledAmount(settledAmount).needSettleAmount(
                needSettleAmount).build()));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/order/list")
    public ResponseEntity<CustomResponse<Page<OrderProductInfoDto>>> selectSettlementOrderList(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                                               @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                                               @RequestParam(value = "take", required = false, defaultValue = "10") Integer take,
                                                                                               @RequestParam(value = "orderby", required = false, defaultValue = "isSettled") OrderProductInfoOrderBy orderBy,
                                                                                               @RequestParam(value = "orderType", required = false, defaultValue = "DESC") Sort.Direction orderType,
                                                                                               @RequestParam(value = "isSettled", required = false) Boolean isSettled,
                                                                                               @RequestParam(value = "storeId", required = false) Integer storeId,
                                                                                               @RequestParam(value = "settledAtS", required = false) Timestamp settledAtS,
                                                                                               @RequestParam(value = "settledAtE", required = false) Timestamp settledAtE) {
        CustomResponse<Page<OrderProductInfoDto>> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN, TokenAuthType.PARTNER), auth);

        Specification<OrderProductInfo> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (storeId != null) predicates.add(builder.equal(root.get("product").get("storeId"), storeId));
            if (isSettled != null) predicates.add(builder.equal(root.get("isSettled"), isSettled));
            if (settledAtS != null) predicates.add(builder.greaterThan(root.get("settledAt"), settledAtS));
            if (settledAtE != null) predicates.add(builder.lessThan(root.get("settledAt"), settledAtE));
            if (tokenInfo.getType().equals(TokenAuthType.PARTNER))
                predicates.add(builder.equal(root.get("product").get("storeId"), tokenInfo.getId()));
            predicates.add(builder.equal(root.get("state"), OrderProductState.FINAL_CONFIRM));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageRequest = PageRequest.of(page, take, Sort.by(orderType, orderBy.label));
        Page<OrderProductInfo> infos = orderService.selectOrderProductInfoList(spec, pageRequest);
        Page<OrderProductInfoDto> result = infos.map(orderService::convert2InfoDto);
        res.setData(Optional.of(result));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/order/list/download")
    @CrossOrigin(value = ("*"), exposedHeaders = {"Content-Disposition"})
    public void selectSettlementOrderListDownload(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                                                      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                                                      @RequestParam(value = "take", required = false, defaultValue = "10") Integer take,
                                                                                                      @RequestParam(value = "orderby", required = false, defaultValue = "isSettled") OrderProductInfoOrderBy orderBy,
                                                                                                      @RequestParam(value = "orderType", required = false, defaultValue = "DESC") Sort.Direction orderType,
                                                                                                      @RequestParam(value = "isSettled", required = false) Boolean isSettled,
                                                                                                      @RequestParam(value = "storeId", required = false) Integer storeId,
                                                                                                      @RequestParam(value = "settledAtS", required = false) Timestamp settledAtS,
                                                                                                      @RequestParam(value = "settledAtE", required = false) Timestamp settledAtE,
                                                                                                      HttpServletRequest httpServletRequest,
                                                                                                      HttpServletResponse httpServletResponse) throws UnsupportedEncodingException {
        CustomResponse<List<SettlementOrderDto>> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN, TokenAuthType.PARTNER), auth);

        String nowDate = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
//        String fileName = nowDate + "_바로피쉬_정산.xlsx";
        String fileName = nowDate + "_barofish_settlement.xlsx";

        httpServletResponse.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
//        httpServletResponse.setHeader("Content-Transfer-Encoding", "binary;");
//        httpServletResponse.setContentType("ms-vnd/excel");
        httpServletResponse.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        try {
            List<SettlementOrderDto> result = null;
            if (tokenInfo.getType().equals(TokenAuthType.PARTNER)) {
                result = settlementQueryService.createOrderSettlementResponse(tokenInfo.getId());
            }

            if (tokenInfo.getType().equals(TokenAuthType.ADMIN)) {
                result = settlementQueryService.createOrderSettlementResponse(null);
            }

            ByteArrayInputStream stream = settlementExcelService.settlementExcelDownload(result);
            try {
                IOUtils.copy(stream, httpServletResponse.getOutputStream());
            } catch (Exception e) {
                e.getMessage();
            } finally {
                // , 출력 스트림을 닫아줌
                httpServletResponse.getOutputStream().close();
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @GetMapping("/log")
    public ResponseEntity<CustomResponse<Page<SettlementDto>>> selectSettlementLogs(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                                    @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                                    @RequestParam(value = "take", required = false, defaultValue = "10") Integer take,
                                                                                    @RequestParam(value = "orderby", required = false, defaultValue = "settledAt") SettlementOrderBy orderby,
                                                                                    @RequestParam(value = "orderType", required = false, defaultValue = "DESC") Sort.Direction orderType,
                                                                                    @RequestParam(value = "state", required = false) Boolean state,
                                                                                    @RequestParam(value = "storeName", required = false) Integer storeName,
                                                                                    @RequestParam(value = "settledAtS", required = false) Timestamp settledAtS,
                                                                                    @RequestParam(value = "settledAtE", required = false) Timestamp settledAtE) {
        CustomResponse<Page<SettlementDto>> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Specification<Settlement> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (state != null) predicates.add(builder.equal(root.get("state"), state));
            if (storeName != null)
                predicates.add(builder.like(root.get("store").get("name"), "%" + storeName + "%"));
            if (settledAtS != null) predicates.add(builder.greaterThan(root.get("settledAt"), settledAtS));
            if (settledAtE != null) predicates.add(builder.lessThan(root.get("settledAt"), settledAtE));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageRequest = PageRequest.of(page, take, Sort.by(orderType, orderby.label));
        Page<SettlementDto>
                settlements =
                settlementQueryService.selectSettlementList(spec,
                        pageRequest).map(settlementCommandService::convert2Dto);
        res.setData(Optional.of(settlements));
        return ResponseEntity.ok(res);
    }


    @PostMapping("/request")
    public ResponseEntity<CustomResponse<Boolean>> processSettleByAdmin(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                        @RequestPart(value = "data") ProcessSettleReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Integer adminId = tokenInfo.getId();
        if (data.getStoreId() == null) throw new BusinessException("파트너를 선택해주세요.");
        if (data.getOrderProductInfoIds() == null || data.getOrderProductInfoIds().size() == 0)
            throw new BusinessException("정산 처리할 주문 내역을 선택해주세요.");
        StoreInfo storeInfo = storeService.selectStoreInfo(data.getStoreId());
        List<OrderProductInfo>
                productInfos =
                orderService.selectOrderProductInfoWithIds(data.getOrderProductInfoIds());
        if (productInfos.stream().map(v -> productService.findById(v.getProductId())).anyMatch(v -> v.getStoreId() !=
                data.getStoreId())) throw new BusinessException("동일한 파트너의 주문만 묶어서 정산 처리 진행해주세요.");
        int totalPrice = settlementQueryService.getSettlementAmount(productInfos, storeInfo.getStoreId());

        for (OrderProductInfo info : productInfos) {
            info.setIsSettled(true);
            info.setSettledAt(utils.now());
        }
        orderService.updateOrderProductInfos(productInfos);
        settlementCommandService.addSettlement(Settlement.builder().storeId(storeInfo.getStoreId()).state(
                SettlementState.DONE).settlementAmount(totalPrice).settledAt(utils.now()).build());
        productInfos.forEach(v -> {
            AdminLog
                    adminLog =
                    AdminLog.builder().id(adminLogQueryService.getAdminLogId()).adminId(adminId).type(AdminLogType.SETTLEMENT).targetId(
                            String.valueOf(v.getId())).content("정산 처리 되었습니다.").createdAt(utils.now()).build();
            adminLogCommandService.saveAdminLog(adminLog);

        });
        res.setData(Optional.of(true));
        return ResponseEntity.ok(res);
    }

//    @Getter
//    @NoArgsConstructor
//    private static class CancelSettlementReq {
//        String cancelReason;
//    }
//
//    @PostMapping("/cancel/{id}")
//    ResponseEntity<CustomResponse<Boolean>> cancelSettlement(@RequestHeader(value = "Authorization") Optional<String> auth,
//                                                             @PathVariable("id") Integer id,
//                                                             @RequestPart(value = "data") CancelSettlementReq data) {
//        CustomResponse<Boolean> res = new CustomResponse<>();
//        Optional<TokenInfo> tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);
//        if (tokenInfo == null) throw new JwtBusinessException(ErrorCode.NOT_ALLOWED);;
//        try {
//            if (data.cancelReason == null) throw new BusinessException()("취소 사유를 입력해주세요);
//            Settlement settlement = settlementService.selectSettlement(id);
//            String cancelReason = utils.validateString(data.cancelReason, 500L, "취소 사유");
//            if (settlement.getState().equals(SettlementState.CANCELED))
//                throw new BusinessException()("이미 취소된 정산입니다);
//            settlement.setCancelReason(cancelReason);
//            settlementService.updateSettlement(settlement);
//            res.setData(Optional.of(true));
//            return ResponseEntity.ok(res);
//        } catch (Exception e) {
//            return res.defaultError(e);
//        }
//    }


    @PostMapping("/cancel")
    public ResponseEntity<CustomResponse<Boolean>> cancelSettleByAdmin(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                       @RequestPart(value = "data") cancelSettleReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Integer adminId = tokenInfo.getId();
        if (data.getStoreId() == null) throw new BusinessException("파트너를 선택해주세요.");
        if (data.getOrderProductInfoIds() == null || data.getOrderProductInfoIds().size() == 0)
            throw new BusinessException("정산 처리할 주문 내역을 선택해주세요.");
        StoreInfo storeInfo = storeService.selectStoreInfo(data.getStoreId());
        List<OrderProductInfo>
                productInfos =
                orderService.selectOrderProductInfoWithIds(data.getOrderProductInfoIds());
        if (productInfos.stream().map(v -> productService.findById(v.getProductId())).anyMatch(v -> v.getStoreId() !=
                data.getStoreId())) throw new BusinessException("동일한 파트너의 주문만 묶어서 정산 처리 진행해주세요.");
        int totalPrice = settlementQueryService.getSettlementAmount(productInfos, storeInfo.getStoreId());
        for (OrderProductInfo info : productInfos) {
            info.setIsSettled(false);
            info.setSettledAt(null);
        }
        settlementCommandService.addSettlement(Settlement.builder().storeId(storeInfo.getStoreId()).state(
                SettlementState.CANCELED).settlementAmount(totalPrice).settledAt(utils.now()).cancelReason(data.getCancelReason()).build());
        orderService.updateOrderProductInfos(productInfos);
        productInfos.forEach(v -> {
            AdminLog
                    adminLog =
                    AdminLog.builder().id(adminLogQueryService.getAdminLogId()).adminId(adminId).type(AdminLogType.SETTLEMENT).targetId(
                            String.valueOf(v.getId())).content("정산 취소 되었습니다.").createdAt(utils.now()).build();
            adminLogCommandService.saveAdminLog(adminLog);

        });
        return ResponseEntity.ok(res);
    }
}
