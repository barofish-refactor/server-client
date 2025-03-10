package com.matsinger.barofishserver.domain.product.difficultDeliverAddress.application;

import com.matsinger.barofishserver.domain.product.difficultDeliverAddress.domain.DifficultDeliverAddress;
import com.matsinger.barofishserver.domain.product.difficultDeliverAddress.repository.DifficultDeliverAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
@Service
public class DifficultDeliverAddressQueryService {
    private final DifficultDeliverAddressRepository difficultDeliverAddressRepository;

    public List<DifficultDeliverAddress> selectDifficultDeliverAddressWithProductId(Integer productId) {
        return difficultDeliverAddressRepository.findAllByProductId(productId);
    }
    
    public List<String> getDifficultDeliveryBcodes(Integer productId) {
        List<DifficultDeliverAddress> difficultDeliverAddresses = selectDifficultDeliverAddressWithProductId(productId);

        return difficultDeliverAddresses.stream().map(v -> v.getBcode()).toList();
    }

    public List<Integer> findDifficultDeliveryProductIds(Set<Integer> productIds, String bcode) {
        String orderBcode = bcode.substring(0, 5);
        return difficultDeliverAddressRepository.findProductIdsByProductIdInAndBcodeStartingWith(productIds, orderBcode);
    }
}
