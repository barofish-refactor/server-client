package com.matsinger.barofishserver.domain.tastingNote.application;

import com.matsinger.barofishserver.order.domain.model.Orders;
import com.matsinger.barofishserver.order.application.service.OrderProductInfoQueryService;
import com.matsinger.barofishserver.order.domain.model.OrderProductInfo;
import com.matsinger.barofishserver.order.domain.model.OrderProductState;
import com.matsinger.barofishserver.domain.tastingNote.domain.TastingNote;
import com.matsinger.barofishserver.domain.tastingNote.dto.TastingNoteCreateRequest;
import com.matsinger.barofishserver.domain.tastingNote.repository.TastingNoteRepository;
import com.matsinger.barofishserver.domain.user.application.UserQueryService;
import com.matsinger.barofishserver.domain.user.domain.User;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TastingNoteCommandService {

    private final TastingNoteRepository tastingNoteRepository;
    private final OrderProductInfoQueryService orderProductInfoQueryService;
    private final UserQueryService userQueryService;

    @Transactional
    public boolean createTastingNote(int userId, TastingNoteCreateRequest request) {

        User findedUser = userQueryService.findById(userId);
        OrderProductInfo findedOrderProductInfo = orderProductInfoQueryService.findById(request.getOrderProductInfoId());
        Orders findedOrder = findedOrderProductInfo.getOrder();

        if (findedUser.getId() != findedOrder.getUserId()) {
            throw new BusinessException("주문인이 일치하지 않습니다.");
        }

        if (findedOrderProductInfo.getState() != OrderProductState.FINAL_CONFIRM) {
            throw new BusinessException("구매 확정 후 테이스팅 노트 작성이 가능합니다.");
        }

        boolean isTastingNoteExists = tastingNoteRepository.existsByOrderProductInfoId(findedOrderProductInfo.getId());
        if (isTastingNoteExists) {
            throw new BusinessException("테이스팅 노트 정보가 이미 존재합니다.");
        }

        convertTastingNoteRequestToEntity(request, findedOrderProductInfo, findedUser);

        TastingNote tastingNote = request.toEntity();
        tastingNoteRepository.save(tastingNote);
        return true;
    }

    private TastingNote convertTastingNoteRequestToEntity(TastingNoteCreateRequest request, OrderProductInfo findedOrderProductInfo, User findedUser) {
        return TastingNote.builder()
                .orderProductInfoId(findedOrderProductInfo.getId())
                .productId(findedOrderProductInfo.getProductId())
                .userId(findedUser.getId())
                .taste1(request.getTaste1())
                .taste2(request.getTaste2())
                .taste3(request.getTaste3())
                .taste4(request.getTaste4())
                .taste5(request.getTaste5())
                .texture1(request.getTexture1())
                .texture2(request.getTexture2())
                .texture3(request.getTexture3())
                .texture4(request.getTexture4())
                .texture5(request.getTexture5())
                .build();
    }
}
