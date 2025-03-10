package com.matsinger.barofishserver.domain.product.optionitem.application;

import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.product.optionitem.repository.OptionItemRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OptionItemQueryService {

    private final OptionItemRepository optionItemRepository;

    public OptionItem findById(int optionItemId) {
        return optionItemRepository.findById(optionItemId)
                                   .orElseThrow(() -> new BusinessException("옵션 아이템 정보를 찾을 수 없습니다."));
    }

    public List<OptionItem> findByIds(List<Integer> optionItemIds) {
        return optionItemRepository.findAllById(optionItemIds);
    }
}
