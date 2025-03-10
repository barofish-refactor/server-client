package com.matsinger.barofishserver.domain.order.api;

import com.matsinger.barofishserver.domain.grade.domain.Grade;
import com.matsinger.barofishserver.order.application.service.OrderService;
import com.matsinger.barofishserver.order.domain.model.Orders;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.order.domain.repository.OrderProductInfoRepository;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.option.application.OptionQueryService;
import com.matsinger.barofishserver.domain.product.option.domain.Option;
import com.matsinger.barofishserver.domain.product.optionitem.application.OptionItemQueryService;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.userinfo.application.UserInfoQueryService;
import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import com.matsinger.barofishserver.domain.userinfo.repository.UserInfoRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.utils.Common;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@ActiveProfiles("local")
class OrderControllerTest {

    @Autowired private ProductService productService;
    @Autowired private OrderService orderService;
    @Autowired private UserInfoQueryService userInfoQueryService;
    @Autowired private OrderProductInfoRepository orderProductInfoRepository;
    @Autowired private OptionItemQueryService optionItemQueryService;
    @Autowired private OptionQueryService optionQueryService;
    @Autowired private Common utils;
    @Autowired private UserInfoRepository userInfoRepository;

    @DisplayName("")
    @Test
    void test() {
        Integer userId = 10000;
        Integer orderProductInfoId = 10134;

        OrderProductInfo info = orderService.selectOrderProductInfo(orderProductInfoId);
        OptionItem requestOptionItem = optionItemQueryService.findById(info.getOptionItemId());
        Option requestOption = optionQueryService.findById(requestOptionItem.getOption().getId());
        if (requestOption.isNeeded() == false) {
            throw new BusinessException("필수옵션인 경우에만 구매 확정이 가능합니다.");
        }
        if (!info.getState().equals(OrderProductState.DELIVERY_DONE)) {
            throw new BusinessException("배송 완료 후 처리 가능합니다.");
        }

        Orders order = orderService.selectOrder(info.getOrderId());
        if (userId != order.getUserId()) throw new BusinessException("타인의 주문 내역입니다.");

        UserInfo userInfo = userInfoQueryService.findByUserId(userId);
        Grade grade = userInfo.getGrade();

        List<OrderProductInfo> orderProductInfos = orderProductInfoRepository.findAllByOrderId(order.getId());
        Product product = productService.selectProduct(info.getProductId());
        List<OrderProductInfo> notNeededOption = orderProductInfos.stream().filter(v -> {
            OptionItem optionItem = optionItemQueryService.findById(v.getOptionItemId());
            Option option = optionQueryService.findById((optionItem.getOption().getId()));
            if (!option.isNeeded() &&
                v.getState() != OrderProductState.FINAL_CONFIRM &&
                v.getProductId() == product.getId()) {
                return true;
            }
            return false;
        }).toList();

        ArrayList<OrderProductInfo> productTobePurchaseConfirmation = new ArrayList<>();
        productTobePurchaseConfirmation.addAll(notNeededOption);
        productTobePurchaseConfirmation.add(info);
        int point = productTobePurchaseConfirmation.stream()
                .mapToInt(v ->
                        (int) Math.floor(v.getPrice() * (product.getPointRate() + grade.getPointRate()))
                ).sum();
        productTobePurchaseConfirmation.forEach(v -> v.setState(OrderProductState.FINAL_CONFIRM));


//        float pointRate = product.getPointRate() != null ? product.getPointRate() : 0;
//        Integer point = (int) (Math.floor(info.getPrice() * ((pointRate + grade.getPointRate()))));
        userInfo.setPoint(userInfo.getPoint() + point);
        info.setState(OrderProductState.FINAL_CONFIRM);
        info.setFinalConfirmedAt(utils.now());

        orderProductInfoRepository.saveAll(productTobePurchaseConfirmation);
        if (point != 0) userInfoRepository.save(userInfo);
    }
}