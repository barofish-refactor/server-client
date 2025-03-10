package com.matsinger.barofishserver.domain.tastingNote.application;

import com.matsinger.barofishserver.order.domain.model.Orders;
import com.matsinger.barofishserver.order.application.service.OrderProductInfoQueryService;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.domain.tastingNote.dto.TastingNoteCreateRequest;
import com.matsinger.barofishserver.domain.tastingNote.repository.TastingNoteRepository;
import com.matsinger.barofishserver.domain.user.application.UserQueryService;
import com.matsinger.barofishserver.domain.user.domain.User;
import com.matsinger.barofishserver.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ActiveProfiles("local")
@ExtendWith(MockitoExtension.class)
class TastingNoteCommandServiceTest {

    @Mock
    TastingNoteRepository tastingNoteRepository;
    @Mock
    OrderProductInfoQueryService orderProductInfoQueryService;
    @Mock
    UserQueryService userQueryService;

    @InjectMocks
    TastingNoteCommandService tastingNoteCommandService;

    @DisplayName("유저가 특정 상품을 주문하고 구매 확정을 한 경우 테이스팅노트를 작성할 수 있다.")
    @Test
    void tastingNoteCreateTest1() {
        int userId = 1;
        int orderProductInfoId = 1;

        TastingNoteCreateRequest request = TastingNoteCreateRequest.builder()
                .orderProductInfoId(1)
                .taste1(1.0)
                .taste2(1.0)
                .taste3(1.0)
                .taste4(1.0)
                .taste5(1.0)
                .texture1(1.0)
                .texture2(1.0)
                .texture3(1.0)
                .texture4(1.0)
                .texture5(1.0)
                .build();

        // given
        given(userQueryService.findById(userId)).willReturn(
                User.builder().id(userId).build()
        );

        OrderProductInfo orderProductInfo = mock(OrderProductInfo.class);
        given(orderProductInfoQueryService.findById(orderProductInfoId))
                .willReturn(orderProductInfo);
        given(orderProductInfo.getOrder()).willReturn(
                Orders.builder().userId(userId).build()
        );
        given(orderProductInfo.getState()).willReturn(
                OrderProductState.FINAL_CONFIRM
        );

        // when
        boolean isSuccess = tastingNoteCommandService.createTastingNote(userId, request);

        // then
        assertThat(isSuccess).isEqualTo(true);
    }

    @DisplayName("테이스팅노트가 이미 있는 상황에서 테이스팅노트를 작성하려고 하면 오류가 발생한다.")
    @Test
    void tastingNoteCreateTest2() {
        int userId = 1;
        int orderProductInfoId = 1;

        TastingNoteCreateRequest request = TastingNoteCreateRequest.builder()
                .orderProductInfoId(1)
                .taste1(1.0)
                .taste2(1.0)
                .taste3(1.0)
                .taste4(1.0)
                .taste5(1.0)
                .texture1(1.0)
                .texture2(1.0)
                .texture3(1.0)
                .texture4(1.0)
                .texture5(1.0)
                .build();

        // given
        given(userQueryService.findById(userId)).willReturn(
                User.builder().id(userId).build()
        );

        OrderProductInfo orderProductInfo = mock(OrderProductInfo.class);
        given(orderProductInfoQueryService.findById(orderProductInfoId))
                .willReturn(orderProductInfo);
        given(orderProductInfo.getOrder()).willReturn(
                Orders.builder().userId(userId).build()
        );
        given(orderProductInfo.getState()).willReturn(
                OrderProductState.FINAL_CONFIRM
        );
        given(orderProductInfo.getId()).willReturn(1);

        // when
        given(tastingNoteRepository.existsByOrderProductInfoId(orderProductInfoId)).willReturn(true);

        // then
        assertThatThrownBy(() -> tastingNoteCommandService.createTastingNote(userId, request))
                .isInstanceOf(BusinessException.class);
    }
}