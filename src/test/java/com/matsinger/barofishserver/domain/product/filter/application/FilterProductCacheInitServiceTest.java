package com.matsinger.barofishserver.domain.product.filter.application;

import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterProducts;
import com.matsinger.barofishserver.domain.product.filter.repository.CategoryFilterProductsQueryRepository;
import com.matsinger.barofishserver.domain.product.filter.repository.CategoryFilterProductsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
class FilterProductCacheInitServiceTest {

    private static final Logger log = LoggerFactory.getLogger(FilterProductCacheInitServiceTest.class);
    @Autowired private CategoryFilterProductsRepository categoryFilterProductsRepository;
    @Autowired private CategoryFilterProductsQueryRepository  categoryFilterProductsQueryRepository;

    @DisplayName("")
    @Test
    void test() {
        // given
        CategoryFilterProducts filterProducts1 = categoryFilterProductsQueryRepository.findBy(
                2, 56, 1, "1"
        ).get(0);
        List<String> productIds1 = Arrays.stream(
                filterProducts1.getProductIds().split(",")
        ).toList();

        CategoryFilterProducts filterProducts2 = categoryFilterProductsQueryRepository.findBy(
                2, 56, 1, "1,2"
        ).get(0);
        List<String> productIds2 = Arrays.stream(
                filterProducts1.getProductIds().split(",")
        ).toList();


        CategoryFilterProducts filterProducts3 = categoryFilterProductsQueryRepository.findBy(
                2, 56, 1, "1,2,42"
        ).get(0);
        List<String> productIds3 = Arrays.stream(
                filterProducts1.getProductIds().split(",")
        ).toList();

        // when
        log.info("productIds1 size = {}", productIds1.size());
        log.info("productIds2 size = {}", productIds2.size());
        log.info("productIds3 size = {}", productIds3.size());

        // then
    }

}