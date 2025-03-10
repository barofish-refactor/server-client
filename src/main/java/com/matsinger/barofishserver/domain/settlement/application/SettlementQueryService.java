package com.matsinger.barofishserver.domain.settlement.application;

import com.matsinger.barofishserver.domain.coupon.application.CouponQueryService;
import com.matsinger.barofishserver.domain.deliver.application.DeliveryQueryService;
import com.matsinger.barofishserver.order.application.service.OrderQueryService;
import com.matsinger.barofishserver.order.application.service.OrderService;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.repository.OrderProductInfoRepository;
import com.matsinger.barofishserver.domain.product.option.application.OptionQueryService;
import com.matsinger.barofishserver.domain.product.optionitem.application.OptionItemQueryService;
import com.matsinger.barofishserver.domain.settlement.domain.Settlement;
import com.matsinger.barofishserver.domain.settlement.domain.SettlementState;
import com.matsinger.barofishserver.domain.settlement.dto.SettlementOrderDto;
import com.matsinger.barofishserver.domain.settlement.dto.SettlementOrderRawDto;
import com.matsinger.barofishserver.domain.settlement.dto.SettlementProductOptionItemDto;
import com.matsinger.barofishserver.domain.settlement.dto.SettlementStoreDto;
import com.matsinger.barofishserver.domain.settlement.repository.SettlementRepository;
import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.domain.user.application.UserQueryService;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class SettlementQueryService {
    private final SettlementRepository settlementRepository;
    private final OrderProductInfoRepository orderProductInfoRepository;
    private final StoreService storeService;
    private final OrderService orderService;
    private final OrderQueryService orderQueryService;
    private final OptionItemQueryService optionItemQueryService;
    private final OptionQueryService optionQueryService;
    private final CouponQueryService couponQueryService;
    private final UserQueryService userQueryService;
    private final DeliveryQueryService deliveryQueryService;

    public Settlement selectSettlement(Integer id) {
        return settlementRepository.findById(id).orElseThrow(() -> {
            throw new BusinessException("정산 내역 정보를 찾을 수 없습니다.");
        });
    }

    public Page<Settlement> selectSettlementList(Specification<Settlement> spec, Pageable pageable) {
        return settlementRepository.findAll(spec, pageable);
    }

    public List<Settlement> selectSettlementListWithState(SettlementState state) {
        if (state == null) return settlementRepository.findAll();
        else return settlementRepository.findAllByState(state);
    }
    public Integer getSettlementAmount(Integer storeId, Boolean isSettled) {
        List<OrderProductInfo>
                infos =
                orderService.selectOrderProductInfoListWithStoreIdAndIsSettled(storeId, isSettled);
        return getSettlementAmount(infos, storeId);
    }
    public Integer getSettlementAmount(List<OrderProductInfo> infos, Integer storeId) {
        int totalPrice = 0;
        if (infos != null && infos.size() != 0) {
            StoreInfo storeInfo = storeService.selectStoreInfo(storeId);
            Float
                    settlementRate =
                    storeInfo.getSettlementRate() == null ||
                            storeInfo.getSettlementRate() == 100 ? null : storeInfo.getSettlementRate();
            for (OrderProductInfo info : infos) {
                int salePrice = info.getSettlePrice() != null ? info.getSettlePrice() : 0;
                int
                        price =
                        (settlementRate == null ? salePrice * info.getAmount() : (int) (salePrice * settlementRate /
                                100.) / 10 * 10);
                totalPrice += price;
            }
        }
        return totalPrice;
    }

    @Transactional(readOnly = true)
    public List<SettlementOrderDto> createOrderSettlementResponse(Integer storeId) {
        List<SettlementOrderRawDto> settlementOrderRawDtos = orderProductInfoRepository.getExcelRawDataWithNotSettled(storeId);

        List<SettlementOrderDto> settlementDtos = new ArrayList<>();
        for (int i = 0; i < settlementOrderRawDtos.size(); i++) {
            SettlementOrderRawDto orderRawDto = settlementOrderRawDtos.get(i);

            SettlementStoreDto settlementStoreDto = new SettlementStoreDto();
            List<SettlementProductOptionItemDto> storeItems = new ArrayList<>();
            for (SettlementProductOptionItemDto optionItemDto : orderRawDto.getSettlementProductOptionItemDtos()) {
                
                calculateOptionItemData(optionItemDto, settlementStoreDto);
                storeItems.add(optionItemDto);
            }

            settlementStoreDto.setStoreId(orderRawDto.getStoreId());
            settlementStoreDto.setPartnerName(orderRawDto.getPartnerName());
            settlementStoreDto.setStoreOptionItemDtos(storeItems);
            settlementStoreDto.setSettlementRate(orderRawDto.getSettlementRate());

            // 같은 주문일 때 settlementOrderDto에 settlementStoreDto 추가
            SettlementOrderDto settlementOrderDto = new SettlementOrderDto();
            if (i >= 1) {
                if (settlementOrderRawDtos.get(i - 1).getOrderId()
                    .equals(
                    settlementOrderRawDtos.get(i).getOrderId())) {

                    settlementDtos.get(settlementDtos.size() - 1).addDeliveryFee(settlementStoreDto.getStoreDeliveryFeeSum());
                    settlementDtos.get(settlementDtos.size() - 1).addStoreInSameOrder(settlementStoreDto);
                    continue;
                }
            }

            // 같은 주문이 아닐 때 settlementDtos에 새로운 settlementOrderDto 추가
            settlementOrderDto.setOrderId(orderRawDto.getOrderId());
            settlementOrderDto.setCouponName(orderRawDto.getCouponName());
            settlementOrderDto.setCouponDiscount(orderRawDto.getCouponDiscount());
            settlementOrderDto.setUsePoint(orderRawDto.getUsePoint());
            settlementOrderDto.addDeliveryFee(settlementStoreDto.getStoreDeliveryFeeSum());
            settlementOrderDto.addStoreInSameOrder(settlementStoreDto);

            settlementDtos.add(settlementOrderDto);
        }

        return settlementDtos;
    }

    private void calculateOptionItemData(SettlementProductOptionItemDto optionItemDto, SettlementStoreDto settlementStoreDto) {
        int sellingPrice = optionItemDto.getSellingPrice();
        int purchasePrice = optionItemDto.getPurchasePrice();
        int deliveryFee = optionItemDto.getDeliveryFee();

        int commissionPrice = sellingPrice - purchasePrice;
        int totalPrice = (sellingPrice * optionItemDto.getQuantity()) + deliveryFee;
        int settlementPrice = purchasePrice + deliveryFee;
        optionItemDto.setCommissionPrice(commissionPrice);
        optionItemDto.setTotalPrice(totalPrice);
        optionItemDto.setSettlementPrice(settlementPrice);

        settlementStoreDto.addDeliveryFee(deliveryFee);
        settlementStoreDto.addPrice(totalPrice);
    }
}
