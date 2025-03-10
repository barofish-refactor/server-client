package com.matsinger.barofishserver.order.domain.repository;

import com.matsinger.barofishserver.order.domain.model.BankCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCodeRepository extends JpaRepository<BankCode, Integer> {
    boolean existsByCode(String code);
}
