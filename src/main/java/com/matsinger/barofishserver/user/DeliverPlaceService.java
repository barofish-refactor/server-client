package com.matsinger.barofishserver.user;

import com.matsinger.barofishserver.user.object.DeliverPlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeliverPlaceService {
    private final DeliverPlaceRepository deliverPlaceRepository;

    void addDeliverPlace(DeliverPlace deliverPlace) {
        deliverPlaceRepository.save(deliverPlace);
    }

    void updateDeliverPlace(DeliverPlace deliverPlace) {
        deliverPlaceRepository.save(deliverPlace);
    }

    List<DeliverPlace> selectDeliverPlaceList(Integer userId) {
        return deliverPlaceRepository.findAllByUserId(userId);
    }

    DeliverPlace selectDeliverPlace(Integer deliverPlaceId) {
        return deliverPlaceRepository.findById(deliverPlaceId).orElseThrow(() -> {
            throw new Error("배송지 정보를 찾을 수 없습니다.");
        });
    }

    Optional<DeliverPlace> selectDefaultDeliverPlace(Integer userId) {
        return deliverPlaceRepository.findByUserIdAndIsDefault(userId, true);
    }

    void deleteDeliverPlace(Integer id) {
        deliverPlaceRepository.deleteById(id);
    }
}
