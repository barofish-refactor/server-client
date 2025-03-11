package com.matsinger.barofishserver.domain.main.api;

import com.matsinger.barofishserver.domain.data.curation.application.CurationQueryService;
import com.matsinger.barofishserver.domain.data.curation.domain.Curation;
import com.matsinger.barofishserver.domain.data.curation.domain.CurationState;
import com.matsinger.barofishserver.domain.product.domain.Product;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class MainControllerTest {

    @Autowired
    private CurationQueryService curationQueryService;

    @Test
    @DisplayName("메인 큐레이션 API 테스트 - 실제 쿼리 로그 확인")
    @Transactional
    void getCurationTest() {
        // given
//        log.info("=== Starting test ===");
        
        // 실제 쿼리 실행 과정 로깅
//        log.info("=== Fetching active curations ===");
        List<Curation> curations = curationQueryService.selectCurationState(CurationState.ACTIVE);
        
//        log.info("=== Fetching products for each curation ===");
        for (Curation curation : curations) {
            List<Product> products = curationQueryService.selectCurationProducts(curation.getId(), PageRequest.of(0, 10));
            for (Product product : products) {
                String productTitle = product.getTitle();
            }
        }
    }
} 