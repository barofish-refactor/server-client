package com.matsinger.barofishserver.domain.product.optionitem.repository;

import com.matsinger.barofishserver.domain.product.domain.OptionItemState;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface OptionItemRepository extends JpaRepository<OptionItem, Integer> {
    Optional<OptionItem> findByName(String name);

    @Transactional
    void deleteAllByOptionId(Integer optionId);

    List<OptionItem> findAllByOptionIdAndState(Integer optionId, OptionItemState state);

    Optional<OptionItem> findFirstByNameAndOptionId(String name, Integer optionId);

    List<OptionItem> findAllByOptionId(Integer optionId);

    List<OptionItem> findAllByIdIn(List<Integer> ids);
}
