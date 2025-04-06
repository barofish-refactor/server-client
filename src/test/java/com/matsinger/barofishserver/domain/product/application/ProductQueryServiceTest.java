package com.matsinger.barofishserver.domain.product.application;

import com.matsinger.barofishserver.domain.product.filter.domain.CategoryFilterProducts;
import com.matsinger.barofishserver.domain.product.filter.repository.CategoryFilterProductsQueryRepository;
import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilterField;
import com.matsinger.barofishserver.domain.searchFilter.repository.SearchFilterFieldRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("local")
class ProductQueryServiceTest {
    private static final Logger log = LoggerFactory.getLogger(ProductQueryServiceTest.class);
    private static final int THREAD_POOL_SIZE = 4; // 스레드 풀 크기 제한

    @Autowired private ProductQueryService productQueryService;
    @Autowired private SearchFilterFieldRepository searchFilterFieldRepository;
    @Autowired private CategoryFilterProductsQueryRepository categoryFilterProductsQueryRepository;

    @DisplayName("countProducts 메서드의 각 단계별 성능을 측정합니다")
    @Test
    void testCountProductsPerformance() {
        // given
        long startTime = System.currentTimeMillis();
        List<SearchFilterField> searchFilterFields = searchFilterFieldRepository.findAll();

        Map<Integer, List<Integer>> filterAndFieldMapper = new HashMap<>();
        for (SearchFilterField filterField : searchFilterFields) {
            int searchFilterId = filterField.getSearchFilterId();
            List<Integer> existingValue = filterAndFieldMapper.getOrDefault(searchFilterId, new ArrayList<>());
            existingValue.add(filterField.getId());
            filterAndFieldMapper.put(
                    searchFilterId,
                    existingValue
            );
        }

        Map<Integer, String> filterFieldPairs = new HashMap<>();

        // 각 필터의 필드 ID를 문자열로 변환하여 쌍으로 추가
        for (Map.Entry<Integer, List<Integer>> entry : filterAndFieldMapper.entrySet()) {
            Integer filterId = entry.getKey();
            List<Integer> fieldIds = entry.getValue();

            // fieldIds를 문자열로 변환 (예: "1,2,3")
            String fieldIdsString = fieldIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            // 필터 ID와 필드 ID 문자열을 쌍으로 추가
            filterFieldPairs.put(filterId, fieldIdsString);
        }
        long dataPrepareEndTime = System.currentTimeMillis();
        log.info("서치필터 필드 데이터 준비 시간: {}ms", dataPrepareEndTime - startTime);


        // 1. DB에서 엔티티 조회 시간 측정
        long dbQueryStartTime = System.currentTimeMillis();
        List<CategoryFilterProducts> caches = categoryFilterProductsQueryRepository.findByFilterIdAndFieldIdsPairs(filterFieldPairs);
        long dbQueryEndTime = System.currentTimeMillis();
        log.info("DB 조회 시간: {}ms", dbQueryEndTime - dbQueryStartTime);
        
        // 2. 정렬 시간 측정
        long sortStartTime = System.currentTimeMillis();
        caches.sort((a, b) -> a.getProductIds().split(",").length - b.getProductIds().split(",").length);
        long sortEndTime = System.currentTimeMillis();
        log.info("정렬 시간: {}ms", sortEndTime - sortStartTime);
        
        // 3. AND 연산 시간 측정
        long andStartTime = System.currentTimeMillis();
        Set<Integer> resultProductIds = null;
        
        if (!caches.isEmpty()) {
            // 첫 번째 캐시의 상품 ID 목록을 초기 결과로 사용
            resultProductIds = Arrays.stream(caches.get(0).getProductIds().split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
            
            log.info("첫 번째 필터의 상품 ID 수: {}", resultProductIds.size());
            
            // 나머지 캐시들과 AND 연산 수행
            for (int i = 1; i < caches.size(); i++) {
                Set<Integer> currentProductIds = Arrays.stream(caches.get(i).getProductIds().split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toSet());
                
                log.info("{}번째 필터의 상품 ID 수: {}", i + 1, currentProductIds.size());
                
                // AND 연산 수행 (교집합)
                resultProductIds.retainAll(currentProductIds);
                
                log.info("AND 연산 후 남은 상품 ID 수: {}", resultProductIds.size());
                
                // 중간 결과가 비어있으면 더 이상 진행할 필요 없음
                if (resultProductIds.isEmpty()) {
                    log.info("AND 연산 결과가 비어있어 더 이상 진행하지 않습니다.");
                    break;
                }
            }
        }
        
        long andEndTime = System.currentTimeMillis();
        log.info("AND 연산 시간: {}ms", andEndTime - andStartTime);
        
        // 4. 전체 countProducts 메서드 실행 시간 측정
        long methodStartTime = System.currentTimeMillis();
        Integer count = productQueryService.countProducts(null);
        long methodEndTime = System.currentTimeMillis();
        log.info("countProducts 메서드 전체 실행 시간: {}ms, 최종 결과 수: {}", 
            methodEndTime - methodStartTime, count);
        
        // 전체 실행 시간
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("테스트 총 실행 시간: {}ms", totalTime);
        
        assertNotNull(count);
    }

    @DisplayName("메모리 효율적인 병렬 처리를 적용한 countProducts 메서드의 각 단계별 성능을 측정합니다")
    @Test
    void testCountProductsPerformanceMemoryEfficient() {
        // given
        long startTime = System.currentTimeMillis();
        List<SearchFilterField> searchFilterFields = searchFilterFieldRepository.findAll();

        Map<Integer, List<Integer>> filterAndFieldMapper = new HashMap<>();
        for (SearchFilterField filterField : searchFilterFields) {
            int searchFilterId = filterField.getSearchFilterId();
            List<Integer> existingValue = filterAndFieldMapper.getOrDefault(searchFilterId, new ArrayList<>());
            existingValue.add(filterField.getId());
            filterAndFieldMapper.put(
                    searchFilterId,
                    existingValue
            );
        }

        Map<Integer, String> filterFieldPairs = new HashMap<>();

        // 각 필터의 필드 ID를 문자열로 변환하여 쌍으로 추가
        for (Map.Entry<Integer, List<Integer>> entry : filterAndFieldMapper.entrySet()) {
            Integer filterId = entry.getKey();
            List<Integer> fieldIds = entry.getValue();

            // fieldIds를 문자열로 변환 (예: "1,2,3")
            String fieldIdsString = fieldIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            // 필터 ID와 필드 ID 문자열을 쌍으로 추가
            filterFieldPairs.put(filterId, fieldIdsString);
        }
        long dataPrepareEndTime = System.currentTimeMillis();
        log.info("서치필터 필드 데이터 준비 시간: {}ms", dataPrepareEndTime - startTime);

        // 1. 병렬 DB 조회 시간 측정 (제한된 스레드 풀 사용)
        long dbQueryStartTime = System.currentTimeMillis();
        
        // 제한된 스레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        
        try {
            // 각 필터 ID와 필드 ID 쌍에 대해 병렬로 조회
            List<CompletableFuture<CategoryFilterProducts>> futures = filterFieldPairs.entrySet().stream()
                    .map(entry -> CompletableFuture.supplyAsync(() -> {
                        Map<Integer, String> singlePair = new HashMap<>();
                        singlePair.put(entry.getKey(), entry.getValue());
                        List<CategoryFilterProducts> result = categoryFilterProductsQueryRepository.findByFilterIdAndFieldIdsPairs(singlePair);
                        return result.isEmpty() ? null : result.get(0);
                    }, executor))
                    .collect(Collectors.toList());
            
            // 결과 수집
            List<CategoryFilterProducts> caches = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            long dbQueryEndTime = System.currentTimeMillis();
            log.info("병렬 DB 조회 시간: {}ms", dbQueryEndTime - dbQueryStartTime);
            
            // 3. 메모리 효율적인 AND 연산 시간 측정
            long andStartTime = System.currentTimeMillis();
            Set<Integer> resultProductIds = memoryEfficientAndOperation(caches);
            long andEndTime = System.currentTimeMillis();
            log.info("메모리 효율적인 AND 연산 시간: {}ms", andEndTime - andStartTime);
            
            // 전체 실행 시간
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("메모리 효율적인 병렬 처리 테스트 총 실행 시간: {}ms", totalTime);

        } finally {
            // 스레드 풀 종료
            executor.shutdown();
        }
    }

    /**
     * 메모리 효율적인 AND 연산을 수행합니다.
     * 가장 작은 집합을 기준으로 AND 연산을 수행하여 메모리 사용량을 최소화합니다.
     */
    private Set<Integer> memoryEfficientAndOperation(List<CategoryFilterProducts> caches) {
        if (caches.isEmpty()) {
            return new HashSet<>();
        }
        
        // 첫 번째 캐시의 상품 ID 목록을 초기 결과로 사용
        Set<Integer> resultProductIds = Arrays.stream(caches.get(0).getProductIds().split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
        
        // 나머지 캐시들과 AND 연산 수행
        for (int i = 1; i < caches.size(); i++) {
            Set<Integer> currentProductIds = Arrays.stream(caches.get(i).getProductIds().split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
            
            // 작은 집합을 기준으로 retainAll 수행하여 메모리 사용량 최소화
            if (currentProductIds.size() < resultProductIds.size()) {
                Set<Integer> temp = new HashSet<>(currentProductIds);
                temp.retainAll(resultProductIds);
                resultProductIds = temp;
            } else {
                resultProductIds.retainAll(currentProductIds);
            }
            
            // 중간 결과가 비어있으면 더 이상 진행할 필요 없음
            if (resultProductIds.isEmpty()) {
                log.info("AND 연산 결과가 비어있어 더 이상 진행하지 않습니다.");
                break;
            }
        }
        
        return resultProductIds;
    }
}