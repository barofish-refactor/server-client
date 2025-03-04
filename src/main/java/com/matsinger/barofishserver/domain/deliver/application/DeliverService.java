package com.matsinger.barofishserver.domain.deliver.application;

import com.matsinger.barofishserver.domain.admin.log.application.AdminLogCommandService;
import com.matsinger.barofishserver.domain.admin.log.application.AdminLogQueryService;
import com.matsinger.barofishserver.domain.admin.log.domain.AdminLog;
import com.matsinger.barofishserver.domain.admin.log.domain.AdminLogType;
import com.matsinger.barofishserver.domain.deliver.ShippingApiAdapter;
import com.matsinger.barofishserver.domain.deliver.domain.Deliver;
import com.matsinger.barofishserver.domain.deliver.domain.DeliveryCompany;
import com.matsinger.barofishserver.domain.deliver.repository.DeliveryCompanyRepository;
import com.matsinger.barofishserver.domain.notification.application.NotificationCommandService;
import com.matsinger.barofishserver.domain.notification.dto.NotificationMessage;
import com.matsinger.barofishserver.domain.notification.dto.NotificationMessageType;
import com.matsinger.barofishserver.domain.order.application.OrderService;
import com.matsinger.barofishserver.domain.order.domain.Orders;
import com.matsinger.barofishserver.domain.order.orderprductinfo.domain.OrderProductInfo;
import com.matsinger.barofishserver.domain.order.orderprductinfo.domain.OrderProductState;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.user.deliverplace.DeliverPlace;
import com.matsinger.barofishserver.domain.user.deliverplace.repository.DeliverPlaceRepository;
import com.matsinger.barofishserver.domain.user.domain.User;
import com.matsinger.barofishserver.domain.user.dto.UserJoinReq;
import com.matsinger.barofishserver.domain.userinfo.domain.UserInfo;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.utils.Common;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeliverService {

    private final ProductService productService;
    private final OrderService orderService;
    private final Common util;
    private final DeliveryCompanyRepository deliveryCompanyRepository;
    private final AdminLogQueryService adminLogQueryService;
    private final AdminLogCommandService adminLogCommandService;
    private final Common utils;
    private final NotificationCommandService notificationCommandService;
    private final ShippingApiAdapter shippingApiAdapter;

    public List<Deliver.Company> selectDeliverCompanyList() {
        List<DeliveryCompany> deliveryCompanies = deliveryCompanyRepository.findAll();
        return deliveryCompanies.stream().map(v -> Deliver.Company.builder().Name(v.getName()).Code(v.getCode()).International(
                null).build()).toList();
    }

    public void refreshOrderDeliverState() {
        List<OrderProductInfo>
                infos =
                orderService.selectOrderProductInfoWithState(new ArrayList<>(List.of(OrderProductState.ON_DELIVERY)));
        infos = infos.stream().filter(v -> v.getInvoiceCode() != null).toList();
        for (OrderProductInfo info : infos) {
            Deliver.TrackingInfo trackingInfo = shippingApiAdapter.getTrackingInfo(info.getDeliverCompanyCode(), info.getInvoiceCode());
            if (trackingInfo != null && trackingInfo.getLevel() == 6) {
                info.setState(OrderProductState.DELIVERY_DONE);
                info.setDeliveryDoneAt(util.now());
                orderService.updateOrderProductInfo(new ArrayList<>(List.of(info)));

                Product product = productService.selectProduct(info.getProductId());

                Orders findOrder = orderService.selectOrder(info.getOrderId());
                notificationCommandService.sendFcmToUser(
                        findOrder.getUserId(),
                        NotificationMessageType.DELIVER_DONE,
                        NotificationMessage
                                .builder().productName(product.getTitle())
                                .build());

                String content = product.getTitle() + " 주문이 배송 완료 처리되었습니다.";
                AdminLog
                        adminLog =
                        AdminLog.builder().id(adminLogQueryService.getAdminLogId()).adminId(1).type(AdminLogType.ORDER).targetId(
                                info.getOrderId()).content(content).createdAt(utils.now()).build();
                adminLogCommandService.saveAdminLog(adminLog);
            }
        }
    }

    public Deliver.TrackingInfo getTrackingInfo(String deliverCompanyCode, String invoice) {
        return shippingApiAdapter.getTrackingInfo(deliverCompanyCode, invoice);
    }

    public List<Deliver.Company> getRecommendDeliverCompanyList(String invoice) {
        return shippingApiAdapter.getRecommendDeliverCompanyList(invoice);
    }

    public String getAccessKey() {
        return shippingApiAdapter.getAccessKey();
    }
}
