package com.matsinger.barofishserver.order.application.service;

import com.matsinger.barofishserver.domain.basketProduct.application.BasketCommandService;
import com.matsinger.barofishserver.domain.basketProduct.application.BasketQueryService;
import com.matsinger.barofishserver.domain.coupon.application.CouponCommandService;
import com.matsinger.barofishserver.domain.coupon.application.CouponQueryService;
import com.matsinger.barofishserver.domain.coupon.domain.Coupon;
import com.matsinger.barofishserver.domain.deliver.domain.DeliveryCompany;
import com.matsinger.barofishserver.domain.deliver.repository.DeliveryCompanyRepository;
import com.matsinger.barofishserver.domain.notification.application.NotificationCommandService;
import com.matsinger.barofishserver.domain.notification.dto.NotificationMessage;
import com.matsinger.barofishserver.domain.notification.dto.NotificationMessageType;
import com.matsinger.barofishserver.order.application.dto.GetCancelPriceDto;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.order.application.dto.OrderProductDto;
import com.matsinger.barofishserver.order.application.dto.OrderProductInfoDto;
import com.matsinger.barofishserver.order.domain.repository.OrderProductInfoRepository;
import com.matsinger.barofishserver.order.domain.repository.OrderProductOptionRepository;
import com.matsinger.barofishserver.domain.payment.application.PaymentService;
import com.matsinger.barofishserver.domain.payment.domain.PaymentState;
import com.matsinger.barofishserver.domain.payment.domain.Payments;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.difficultDeliverAddress.application.DifficultDeliverAddressQueryService;
import com.matsinger.barofishserver.domain.product.difficultDeliverAddress.domain.DifficultDeliverAddress;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.domain.ProductDeliverFeeType;
import com.matsinger.barofishserver.domain.product.option.application.OptionQueryService;
import com.matsinger.barofishserver.domain.product.option.domain.Option;
import com.matsinger.barofishserver.domain.product.optionitem.application.OptionItemQueryService;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.product.optionitem.dto.OptionItemDto;
import com.matsinger.barofishserver.domain.review.application.ReviewQueryService;
import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.domain.user.application.UserCommandService;
import com.matsinger.barofishserver.order.application.dto.OrderDto;
import com.matsinger.barofishserver.order.application.dto.OrderProductReq;
import com.matsinger.barofishserver.order.application.dto.VBankRefundInfo;
import com.matsinger.barofishserver.order.domain.model.*;
import com.matsinger.barofishserver.order.domain.repository.BankCodeRepository;
import com.matsinger.barofishserver.order.domain.repository.OrderDeliverPlaceRepository;
import com.matsinger.barofishserver.order.domain.repository.OrderRepository;
import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import com.matsinger.barofishserver.domain.userinfo.dto.UserInfoDto;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.utils.Common;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProductInfoRepository infoRepository;
    private final OrderProductOptionRepository optionRepository;
    private final ProductService productService;
    private final UserCommandService userService;
    private final StoreService storeService;
    private final OrderDeliverPlaceRepository orderDeliverPlaceRepository;
    private final ReviewQueryService reviewQueryService;
    private final PaymentService paymentService;
    private final DeliveryCompanyRepository deliveryCompanyRepository;
    private final DifficultDeliverAddressQueryService difficultDeliverAddressQueryService;
    private final CouponQueryService couponQueryService;
    private final NotificationCommandService notificationCommandService;
    private final CouponCommandService couponCommandService;
    private final BankCodeRepository bankCodeRepository;
    private final BasketCommandService basketCommandService;
    private final BasketQueryService basketQueryService;
    private final Common utils;
    private final OptionItemQueryService optionItemQueryService;
    private final OptionQueryService optionQueryService;

    public OrderDto convert2Dto(Orders order, Integer storeId, Specification<OrderProductInfo> productSpec) {
        OrderDeliverPlace deliverPlace = selectDeliverPlace(order.getId());
        if (productSpec != null) {
            productSpec = productSpec.and((root, query, builder) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(builder.equal(root.get("orderId"), order.getId()));
                return builder.and(predicates.toArray(new Predicate[0]));
            });
        }
        List<OrderProductInfo>
                infos =
                productSpec == null
                        ? selectOrderProductInfoListWithOrderId(order.getId())
                        : selectOrderProductInfoList(productSpec);

        List<OrderProductDto> orderProductDtos = infos.stream().map(opi -> {

            // OrderProductOption option =
            // optionRepository.findFirstByOrderProductId(opi.getId());
            Product product = productService.selectProduct(opi.getProductId());
            StoreInfo storeInfo = storeService.selectStoreInfo(product.getStoreId());

            if (storeId != null && storeId != storeInfo.getStoreId()) return null;

            Optional<DeliveryCompany> deliveryCompany =
                    opi.getDeliverCompanyCode() != null
                            ? deliveryCompanyRepository.findById(opi.getDeliverCompanyCode())
                            : Optional.empty();

            OptionItem optionItem = productService.selectOptionItem(opi.getOptionItemId());
            OptionItemDto optionItemDto = optionItem.convert2Dto(product);
            optionItemDto.setPointRate(product.getPointRate());

            Option option = optionQueryService.findById(optionItemDto.getOptionId());

            Boolean isWritten = reviewQueryService.checkReviewWritten(order.getUserId(), product.getId(), opi.getId());

            return OrderProductDto.builder()
                    .id(opi.getId())
                    .storeId(storeInfo.getStoreId())
                    .optionItem(optionItemDto)
                    .product(productService.convert2ListDto(productService.selectProduct(opi.getProductId())))
                    .optionName(optionItem.getName())
                    .isNeeded(option.getIsNeeded())
                    .amount(opi.getAmount())
                    .state(opi.getState())
                    .price(opi.getPrice())
                    .storeName(storeInfo.getName())
                    .storeProfile(storeInfo.getProfileImage())
                    .deliverFee(opi.getDeliveryFee())
                    .deliverCompanyCode(opi.getDeliverCompanyCode())
                    .deliverCompany(deliveryCompany.map(DeliveryCompany::getName).orElse(null))
                    .invoiceCode(opi.getInvoiceCode())
                    .cancelReason(opi.getCancelReason())
                    .cancelReasonContent(opi.getCancelReasonContent())
                    .isReviewWritten(isWritten)
                    .deliverFeeType(product.getDeliverFeeType())
                    .minOrderPrice(product.getMinOrderPrice())
                    .minStorePrice(storeInfo.getMinStorePrice())
                    .finalConfirmedAt(opi.getFinalConfirmedAt())
                    .needTaxation(product.getNeedTaxation())
                    .originPrice(opi.getOriginPrice())
                    .build();})
                .filter(Objects::nonNull).toList();
        String couponName = null;
        if (order.getCouponId() != null) {
            Coupon coupon = couponQueryService.selectCoupon(order.getCouponId());
            couponName = coupon.getTitle();
        }
        UserInfoDto userInfoDto = userService.selectUserInfo(order.getUserId()).convert2Dto();
        return OrderDto.builder()
                .id(order.getId())
                .state(order.getState())
                .orderedAt(order.getOrderedAt())
                .user(userInfoDto)
                .totalAmount(order.getTotalPrice())
                .deliverPlace(deliverPlace.convert2Dto())
                .paymentWay(order.getPaymentWay())
                .productInfos(orderProductDtos)
                .couponDiscount(order.getCouponDiscount())
                .usePoint(order.getUsePoint())
                .ordererName(order.getOrdererName())
                .ordererTel(order.getOrdererTel())
                .couponName(couponName)
                .bankHolder(order.getBankHolder())
                .bankAccount(order.getBankAccount())
                .bankCode(order.getBankCode())
                .bankName(order.getBankName())
                .originTotalPrice(order.getOriginTotalPrice())
                .build();
    }

    public OrderDeliverPlace selectDeliverPlace(String orderId) {
        return orderDeliverPlaceRepository.findById(orderId).orElseThrow(() -> {
            throw new BusinessException("주문 배송지 정보를 찾을 수 없습니다.");
        });
    }

    public Orders orderProduct(Orders orders, List<OrderProductInfo> infos, OrderDeliverPlace deliverPlace) {
        Orders order = orderRepository.save(orders);
        orderDeliverPlaceRepository.save(deliverPlace);
        infoRepository.saveAll(infos);
        return order;
    }

    public String getOrderId() {
        return orderRepository.selectOrderId().get("id").toString();
    }

    public Orders selectOrder(String id) {
        return orderRepository.findById(id).orElseThrow(() -> {
            throw new BusinessException("주문 정보를 찾을 수 없습니다.");
        });
    }

    public void updateOrder(Orders order) {
        orderRepository.save(order);
    }

    public Page<Orders> selectOrderLitByAdmin(PageRequest pageRequest, Specification<Orders> spec) {
        return orderRepository.findAll(spec, pageRequest);
    }

    public Page<Orders> selectOrderList(Integer userId, PageRequest pageRequest) {
        Specification<Orders> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("userId"), userId));
            Join<Orders, OrderProductInfo> t = root.join("productInfos", JoinType.INNER);
            predicates.add(builder.isNotNull(t.get("id")));
            predicates.add(builder.or(builder.notEqual(t.get("state"), OrderProductState.WAIT_DEPOSIT),
                    builder.and(root.get("paymentWay").in(List.of(OrderPaymentWay.DEPOSIT,
                            OrderPaymentWay.VIRTUAL_ACCOUNT)))));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        return orderRepository.findAll(spec, pageRequest);
    }

    public Integer countOrderList(Integer userId, String state) {
        Specification<Orders> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("userId"), userId));
            Join<Orders, OrderProductInfo> t = root.join("productInfos", JoinType.INNER);
            predicates.add(builder.isNotNull(t.get("id")));
            predicates.add(builder.or(builder.notEqual(t.get("state"), OrderProductState.WAIT_DEPOSIT),
                    builder.and(root.get("paymentWay").in(List.of(OrderPaymentWay.DEPOSIT,
                            OrderPaymentWay.VIRTUAL_ACCOUNT)))));
            if (state != null) {
                predicates.add(root.get("productInfos").get("state").in(Arrays.stream(state.split(",")).map(
                        OrderProductState::valueOf).toList()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        return (int) orderRepository.count(spec);
    }

    public List<Orders> selectCanceledOrderList(Integer userId) {
        Specification<Orders> spec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("userId"), userId));
            Join<Orders, OrderProductInfo> t = root.join("productInfos", JoinType.INNER);
            predicates.add(builder.and(t.get("state").in(OrderProductState.CANCELED,
                    OrderProductState.REFUND_DONE,
                    OrderProductState.CANCEL_REQUEST,
                    OrderProductState.REFUND_REQUEST,
                    OrderProductState.REFUND_ACCEPT)));
            predicates.add(builder.isNotNull(t.get("id")));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        // Page<Orders> orders = orderRepository.findAllByUserId(userId, pageRequest);
        return orderRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<Orders> selectOrderList() {
        return orderRepository.findAll();
    }

    public List<OrderProductInfo> selectOrderProductInfoList(Specification<OrderProductInfo> spec) {
        return infoRepository.findAll(spec);
    }

    public List<OrderProductInfo> selectOrderProductInfoListWithOrderId(String orderId) {
        return infoRepository.findAllByOrderId(orderId);
    }

    public OrderProductInfo selectOrderProductInfo(Integer orderProductInfoId) {
        return infoRepository.findById(orderProductInfoId).orElseThrow(() -> {
            throw new BusinessException("주문 상품 정보를 찾을 수 없습니다.");
        });
    }

    public void updateOrderProductInfo(List<OrderProductInfo> infos) {
        infoRepository.saveAll(infos);
    }

    public void requestCancelOrderProductsCouponUsed(Orders order) throws Exception {
        List<OrderProductInfo> infos = selectOrderProductInfoListWithOrderId(order.getId());
        if (infos.stream().anyMatch(v -> {
            switch (v.getState()) {
                case ON_DELIVERY:
                case DELIVERY_DONE:
                case EXCHANGE_REQUEST:
                case EXCHANGE_ACCEPT:
                case FINAL_CONFIRM:
                case REFUND_REQUEST:
                case REFUND_ACCEPT:
                case REFUND_DONE:
                case CANCEL_REQUEST:
                case CANCELED:
                    return true;
                case WAIT_DEPOSIT:
                case PAYMENT_DONE:
                case DELIVERY_READY:
                default:
                    return false;
            }
        })) throw new BusinessException("취소 불가능한 상태입니다.");
        if (infos.stream().anyMatch(v -> v.getState().equals(OrderProductState.WAIT_DEPOSIT))) {
            infos.forEach(info -> info.setState(OrderProductState.CANCELED));
            updateOrderProductInfos(infos);
        } else if (infos.stream().anyMatch(v -> v.getState().equals(OrderProductState.DELIVERY_READY))) {
            infos.forEach(info -> info.setState(OrderProductState.CANCEL_REQUEST));
            updateOrderProductInfo(infos);
        } else {
            VBankRefundInfo
                    vBankRefundInfo =
                    order.getPaymentWay().equals(OrderPaymentWay.VIRTUAL_ACCOUNT) ? VBankRefundInfo.builder().bankHolder(
                            order.getBankHolder()).bankCode(order.getBankCode()).bankName(order.getBankName()).bankAccount(
                            order.getBankAccount()).build() : null;
            paymentService.cancelPayment(order.getImpUid(), null, null, vBankRefundInfo);
            infos.forEach(info -> info.setState(OrderProductState.CANCELED));
            updateOrderProductInfo(infos);
            Integer returnPoint = checkReturnPoint(order);
            returnCouponIfAllCanceled(order);
            if (returnPoint != null) returnPoint(order.getUserId(), returnPoint);
            order.setState(OrderState.CANCELED);
            Payments payment = paymentService.selectPayment(order.getId());
            payment.setStatus(PaymentState.CANCELED);
            orderRepository.save(order);
            paymentService.updatePayment(payment);
        }
    }

    public void requestCancelOrderProduct(OrderProductInfo info) throws Exception {
        switch (info.getState()) {
            case WAIT_DEPOSIT:
                info.setState(OrderProductState.CANCELED);
                updateOrderProductInfo(new ArrayList<>(List.of(info)));
                break;
            case PAYMENT_DONE:
                cancelOrderedProduct(info.getId());
                break;
            case ON_DELIVERY:
            case DELIVERY_DONE:
            case EXCHANGE_REQUEST:
            case EXCHANGE_ACCEPT:
            case FINAL_CONFIRM:
            case REFUND_REQUEST:
            case REFUND_ACCEPT:
            case REFUND_DONE:
                throw new BusinessException("취소 불가능한 상태입니다.");
            case CANCEL_REQUEST:
                throw new BusinessException("이미 취소 요청된 상태입니다.");
            case CANCELED:
                throw new BusinessException("취소 완료된 상태입니다.");
            case DELIVERY_READY:
            default:
                info.setState(OrderProductState.CANCEL_REQUEST);
                updateOrderProductInfo(new ArrayList<>(List.of(info)));
        }
    }

    public void cancelOrderedProduct(Integer orderProductInfoId) throws Exception {
        OrderProductInfo info = infoRepository.findById(orderProductInfoId).orElseThrow(() -> {
            throw new BusinessException("주문 상품 정보를 찾을 수 없습니다.");
        });
        if (info.getState().equals(OrderProductState.CANCELED)) throw new Exception("이미 취소된 상품입니다.");
        Orders order = orderRepository.findById(info.getOrderId()).orElseThrow(() -> {
            try {
                throw new BusinessException("주문 정보를 찾을 수 없습니다.");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        GetCancelPriceDto cancelData = getCancelPrice(order, List.of(info));
        int price = cancelData.getCancelPrice();
        int taxFreeAmount = info.getTaxFreeAmount() != null && info.getIsTaxFree() ? info.getTaxFreeAmount() : 0;
        log.info("v1 cancelPrice = {}", price);
        log.info("v1 taxFreeAmount = {}", info.getTaxFreeAmount());
        VBankRefundInfo
                vBankRefundInfo =
                order.getPaymentWay().equals(OrderPaymentWay.VIRTUAL_ACCOUNT) ? VBankRefundInfo.builder().bankHolder(
                        order.getBankHolder()).bankCode(order.getBankCode()).bankName(order.getBankName()).bankAccount(
                        order.getBankAccount()).build() : null;
        paymentService.cancelPayment(order.getImpUid(), price, taxFreeAmount, vBankRefundInfo);
        info.setState(OrderProductState.CANCELED);
        infoRepository.save(info);
        List<OrderProductInfo> infos = infoRepository.findAllByOrderId(order.getId());
        Integer returnPoint = cancelData.getReturnPoint();
        if (returnPoint != null && returnPoint != 0) returnPoint(order.getUserId(), returnPoint);
        boolean allCanceled = infos.stream().allMatch(v -> v.getState().equals(OrderProductState.CANCELED));
        if (allCanceled) {
            returnCouponIfAllCanceled(order);
            order.setState(OrderState.CANCELED);
            Payments payment = paymentService.selectPayment(order.getId());
            payment.setStatus(PaymentState.CANCELED);
            orderRepository.save(order);
            paymentService.updatePayment(payment);

        }
    }


    public GetCancelPriceDto getCancelPrice(Orders order, List<OrderProductInfo> infos) {
        List<OrderProductInfo> orderProductInfos = selectOrderProductInfoListWithOrderId(order.getId());
        // 총 주문 상품이 단건이면
        if (orderProductInfos.size() == infos.size()) {
            return GetCancelPriceDto.builder().cancelPrice(order.getTotalPrice()).returnPoint(order.getUsePoint()).build();
        }

        List<StoreInfo>
                stores =
                orderProductInfos.stream().filter(v -> infos.stream().map(OrderProductInfo::getId).toList().contains(v.getId())).map(
                        v -> v.getProduct().getStoreId()).distinct().map(storeService::selectStoreInfo).toList();
        int deliveryFee = stores.stream().mapToInt(s -> {
            List<OrderProductInfo> sameStoreInfos =
                    orderProductInfos.stream()
                            .filter(opi -> opi.getProduct().getStoreId() == s.getStoreId()).toList();

            if (sameStoreInfos.size() != 0) {
                sameStoreInfos.forEach(ssi -> {
                    if (infos.stream().anyMatch(a -> a.getId() == ssi.getId() &&
                            (ssi.getState().equals(OrderProductState.WAIT_DEPOSIT) ||
                             ssi.getState().equals(OrderProductState.DELIVERY_READY) ||
                             ssi.getState().equals(OrderProductState.PAYMENT_DONE))))
                        ssi.setState(OrderProductState.CANCELED);
                });
                if (sameStoreInfos.stream().allMatch(a -> a.getState().equals(OrderProductState.CANCELED)))
                    return Collections.max(sameStoreInfos.stream().map(OrderProductInfo::getDeliveryFee).toList());
            }
            return 0;
        }).sum();
        int orderedPrice = orderProductInfos.stream()
                .filter(v -> !v.getState().equals(OrderProductState.CANCELED))
                .mapToInt(OrderProductInfo::getPrice).sum();

        AtomicInteger returnPoint = new AtomicInteger();
        AtomicInteger cancelPrice = new AtomicInteger();
        orderProductInfos.forEach(v -> {
            if (infos.stream().anyMatch(info -> info.getId() == v.getId())) {
                cancelPrice.addAndGet(v.getTaxFreeAmount());
                returnPoint.addAndGet((v.getPrice() - v.getTaxFreeAmount()));
                v.setState(OrderProductState.CANCELED);
            }
        });
        int couponDiscount = 0;
//        Coupon coupon = order.getCouponId() != null ? couponQueryService.selectCoupon(order.getCouponId()) : null;
//        if (coupon != null && coupon.getMinPrice() > orderedPrice - cancelPrice.get()) {
//            if (couponQueryService.checkUsedCoupon(order.getCouponId(), order.getUserId()))
//                couponDiscount = order.getCouponDiscount();
////            order.setCouponDiscount(0);
////            order.setCouponId(null);
////            orderRepository.save(order);
//            if (orderProductInfos.stream().allMatch(v -> v.getState().equals(OrderProductState.CANCELED))) {
//                couponCommandService.unUseCoupon(order.getCouponId(), order.getUserId());
//            }
//        }
//        Integer point = checkReturnPoint(orderProductInfos, order);
        int result = cancelPrice.get() + deliveryFee;
        return GetCancelPriceDto.builder().cancelPrice(result).returnPoint(returnPoint.get()).build();
    }

    public Integer checkReturnPoint(List<OrderProductInfo> orderProductInfos, Orders order) {
        if (orderProductInfos.stream().allMatch(v -> v.getState().equals(OrderProductState.CANCELED)))
            return order.getUsePoint();
        else return null;
    }

    public Integer checkReturnPoint(Orders order) {
        List<OrderProductInfo> infos = infoRepository.findAllByOrderId(order.getId());
        if (infos.stream().allMatch(v -> v.getState().equals(OrderProductState.CANCELED))) return order.getUsePoint();
        else return null;
    }

    public void returnCouponIfAllCanceled(Orders order) {
        List<OrderProductInfo> infos = infoRepository.findAllByOrderId(order.getId());
        boolean
                allCanceled =
                order.getCouponId() != null &&
                        infos.stream().allMatch(v -> v.getState().equals(OrderProductState.CANCELED));
        if (allCanceled) couponCommandService.unUseCoupon(order.getCouponId(), order.getUserId());
    }

    public void returnPoint(Integer userId, Integer returnPoint) {
        UserInfo userInfo = userService.selectUserInfo(userId);
        userInfo.setPoint(userInfo.getPoint() + returnPoint);
        userService.updateUserInfo(userInfo);
    }

    public List<OrderProductInfo> selectOrderProductInfoWithState(List<OrderProductState> states) {
        return infoRepository.findAllByStateIn(states);
    }

    public Integer getProductPrice(Integer OptionItemId, Integer amount) {
        OptionItem optionItem = productService.selectOptionItem(OptionItemId);
        Integer totalPrice = optionItem.getDiscountPrice() * amount;
        return totalPrice;
    }

    public Integer getProductDeliveryFee(Product product,
                                         Integer OptionItemId,
                                         Integer amount,
                                         List<OrderProductReq> productReqs) {
        OptionItem optionItem = productService.selectOptionItem(OptionItemId);
        StoreInfo storeInfo = storeService.selectStoreInfo(product.getStoreId());
        if (product.getDeliverFeeType().equals(ProductDeliverFeeType.FREE)) return 0;
        else if (product.getDeliverFeeType().equals(ProductDeliverFeeType.FREE_IF_OVER)) {
            int price = productReqs.stream().filter(v -> v.getProductId() == product.getId()).mapToInt(v -> {
                OptionItem oi = productService.selectOptionItem(v.getOptionId());
                return oi.getDiscountPrice() * v.getAmount();
            }).sum();
            return product.getMinOrderPrice() == null ||
                    price >= product.getMinOrderPrice() ? 0 : product.getDeliverFee();
        } else return product.getDeliverFee() != null ? product.getDeliverFee() : 0;
    }

    public List<OrderProductInfo> selectOrderProductInfoWithIds(List<Integer> ids) {
        return infoRepository.findAllByIdIn(ids);
    }

    public Page<OrderProductInfo> selectOrderProductInfoList(Specification<OrderProductInfo> spec, Pageable pageable) {
        return infoRepository.findAll(spec, pageable);
    }

    public void updateOrderProductInfos(List<OrderProductInfo> infos) {
        infoRepository.saveAll(infos);
    }

    public OrderProductInfoDto convert2InfoDto(OrderProductInfo info) {
        Orders order = selectOrder(info.getOrderId());
        OrderDto orderDto = convert2Dto(order, null, null);
        orderDto.setProductInfos(null);
        Product product = productService.selectProduct(info.getProductId());
        StoreInfo storeInfo = storeService.selectStoreInfo(product.getStoreId());
        OptionItem optionItem = productService.selectOptionItem(info.getOptionItemId());
        Optional<DeliveryCompany>
                deliveryCompany =
                info.getDeliverCompanyCode() !=
                        null ? deliveryCompanyRepository.findById(info.getDeliverCompanyCode()) : Optional.empty();

        return OrderProductInfoDto.builder().id(info.getId()).orderId(info.getOrderId()).productId(info.getProductId()).optionItemId(
                info.getOptionItemId()).optionItem(optionItem.convert2Dto(product)).state(info.getState()).settlePrice(info.getSettlePrice()).price(
                info.getPrice()).amount(info.getAmount()).deliveryFee(info.getDeliveryFee()).cancelReasonContent(info.getCancelReasonContent()).cancelReason(
                info.getCancelReason()).deliverCompanyCode(info.getDeliverCompanyCode()).invoiceCode(info.getInvoiceCode()).isSettled(
                info.getIsSettled()).settledAt(info.getSettledAt()).product(productService.convert2ListDto(info.getProduct())).order(
                orderDto).settlementRate(storeInfo.getSettlementRate()).deliverFeeType(product.getDeliverFeeType()).needTaxation(
                product.getNeedTaxation()).deliverCompany(deliveryCompany.map(DeliveryCompany::getName).orElse(null)).originPrice(
                info.getOriginPrice()).build();
    }


    public List<OrderProductInfo> selectOrderProductInfoListWithIds(List<Integer> ids) {
        return infoRepository.findAllByIdIn(ids);
    }

    public List<OrderProductInfo> selectOrderProductInfoListWithStoreIdAndIsSettled(Integer storeId,
                                                                                    Boolean isSettled) {
        return infoRepository.findAllByProduct_StoreIdAndIsSettled(storeId, isSettled);
    }

    public boolean canDeliver(OrderDeliverPlace orderDeliverPlace, OrderProductInfo orderProductInfo) {
        List<String>
                difficultDeliverBcode =
                difficultDeliverAddressQueryService
                        .selectDifficultDeliverAddressWithProductId(
                                orderProductInfo.getProductId()
                        ).stream().map(DifficultDeliverAddress::getBcode).toList();
        return difficultDeliverBcode.stream()
                .noneMatch(
                        v -> v.length() >= 5 &&
                                v.substring(0, 5).equals(orderDeliverPlace.getBcode().substring(0, 5))
                );
    }

    public void processOrderZeroAmount(Orders order) {
        List<OrderProductInfo> infos = selectOrderProductInfoListWithOrderId(order.getId());
        infos.forEach(info -> {
            OptionItem optionItem = productService.selectOptionItem(info.getOptionItemId());
            if (optionItem.getAmount() != null) {
                optionItem.setAmount(optionItem.getAmount() - info.getAmount());
            }
            productService.addOptionItem(optionItem);
            info.setState(OrderProductState.PAYMENT_DONE);
            notificationCommandService.sendFcmToUser(order.getUserId(),
                    NotificationMessageType.PAYMENT_DONE,
                    NotificationMessage.builder()
                            .productName(info.getProduct().getTitle())
                            .build());
        });
        order.setState(OrderState.PAYMENT_DONE);
        updateOrderProductInfo(infos);
        updateOrder(order);
        UserInfo userInfo = userService.selectUserInfo(order.getUserId());
        userInfo.setPoint(userInfo.getPoint() - order.getUsePoint());
        userService.updateUserInfo(userInfo);
        couponCommandService.useCouponV1(order.getCouponId(), order.getUserId());
    }

    public List<BankCode> selectBankCodeList() {
        return bankCodeRepository.findAll();
    }

    public BankCode selectBankCode(Integer id) {
        return bankCodeRepository.findById(id).orElseThrow(() -> new BusinessException("잘못된 은행 코드 정보입니다."));
    }

    public void finalConfirmOrderProduct(OrderProductInfo info) {
        Orders order = selectOrder(info.getOrderId());
        if (info.getState().equals(OrderProductState.DELIVERY_DONE)) {
            UserInfo userInfo = userService.selectUserInfo(order.getUserId());
            Product product = productService.selectProduct(info.getProductId());
            float pointRate = product.getPointRate() != null ? product.getPointRate() : 0;
            Integer point = (int) Math.floor(info.getPrice() * info.getAmount() * pointRate);
            userInfo.setPoint(userInfo.getPoint() + point);
            info.setState(OrderProductState.FINAL_CONFIRM);
            info.setFinalConfirmedAt(utils.now());
            if (point != 0) userService.updateUserInfo(userInfo);
            updateOrderProductInfo(new ArrayList<>(List.of(info)));
            try {
                couponCommandService.publishSystemCoupon(userInfo.getUserId());
            } catch (Exception e) {
            }
        }
    }

    public void automaticFinalConfirm() {
        Timestamp ts = utils.now();
        Calendar cal = Calendar.getInstance();
        cal.setTime(ts);

        cal.add(Calendar.DATE, -5);
        List<OrderProductInfo>
                orderProductInfoIds =
                infoRepository.findAllByDeliveryDoneAtBefore(new Timestamp(cal.getTime().getTime()));
        orderProductInfoIds.forEach(this::finalConfirmOrderProduct);
    }
}
