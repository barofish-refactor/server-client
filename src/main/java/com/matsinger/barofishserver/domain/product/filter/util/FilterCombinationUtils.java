package com.matsinger.barofishserver.domain.product.filter.util;

import com.matsinger.barofishserver.domain.product.filter.domain.FilterFieldCombination;
import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilter;
import com.matsinger.barofishserver.domain.searchFilter.domain.SearchFilterField;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 필터 조합 생성과 관련된 유틸리티 메서드들을 제공하는 클래스
 */
public class FilterCombinationUtils {
    
    private FilterCombinationUtils() {
        // 인스턴스 생성 방지
    }
    
    /**
     * 필터-필드 조합의 filterIds 문자열을 생성합니다.
     * 예: "1='2,3',2='4,5'"
     */
    public static String createFilterFieldIdsString(List<FilterFieldCombination> combination) {
        // 필터 ID 기준으로 정렬
        combination.sort(Comparator.comparing(FilterFieldCombination::getFilterId));
        
        StringBuilder filterIdsBuilder = new StringBuilder();
        
        for (int i = 0; i < combination.size(); i++) {
            FilterFieldCombination fieldCombination = combination.get(i);
            
            if (i > 0) {
                filterIdsBuilder.append(",");
            }
            
            filterIdsBuilder.append(fieldCombination.getFilterId())
                .append("='")
                .append(fieldCombination.getFieldIds())
                .append("'");
        }
        
        return filterIdsBuilder.toString();
    }
    
    /**
     * 필터-필드 조합의 filterKeys 문자열을 생성합니다.
     * 예: "구분='양식,자연산',지역='동해,서해'"
     */
    public static String createFilterFieldKeysString(List<FilterFieldCombination> combination) {
        // 필터 ID 기준으로 정렬
        combination.sort(Comparator.comparing(FilterFieldCombination::getFilterId));
        
        StringBuilder filterKeysBuilder = new StringBuilder();
        
        for (int i = 0; i < combination.size(); i++) {
            FilterFieldCombination fieldCombination = combination.get(i);
            
            if (i > 0) {
                filterKeysBuilder.append(",");
            }
            
            filterKeysBuilder.append(fieldCombination.getFilterName())
                .append("='")
                .append(fieldCombination.getFieldNames())
                .append("'");
        }
        
        return filterKeysBuilder.toString();
    }
    
    /**
     * 필터 조합의 ID 문자열을 생성합니다.
     * 예: "1,3,5"
     */
    public static String createFilterIdsString(List<SearchFilter> filters) {
        List<Integer> sortedFilterIds = filters.stream()
            .map(SearchFilter::getId)
            .sorted()
            .collect(Collectors.toList());
            
        return String.join(",", sortedFilterIds.stream()
            .map(String::valueOf)
            .collect(Collectors.toList()));
    }
    
    /**
     * 필터 조합의 이름 문자열을 생성합니다.
     * 예: "구분,지역,크기"
     */
    public static String createFilterNamesString(List<SearchFilter> filters) {
        // 필터 ID 오름차순 정렬
        List<Integer> sortedFilterIds = filters.stream()
            .map(SearchFilter::getId)
            .sorted()
            .collect(Collectors.toList());
        
        // 필터 이름
        Map<Integer, String> filterMap = filters.stream()
            .collect(Collectors.toMap(SearchFilter::getId, SearchFilter::getName));
        
        List<String> filterNames = sortedFilterIds.stream()
            .map(filterMap::get)
            .collect(Collectors.toList());
            
        return String.join(",", filterNames);
    }
    
    /**
     * 필터 필드 ID 목록을 문자열로 변환합니다.
     * 예: "2,3,5"
     */
    public static String createFieldIdsString(List<SearchFilterField> fields) {
        List<Integer> sortedFieldIds = fields.stream()
            .map(SearchFilterField::getId)
            .sorted()
            .collect(Collectors.toList());
            
        return String.join(",", sortedFieldIds.stream()
            .map(String::valueOf)
            .collect(Collectors.toList()));
    }
    
    /**
     * 필터 필드 이름 목록을 문자열로 변환합니다.
     * 예: "양식,자연산,냉동"
     */
    public static String createFieldNamesString(List<SearchFilterField> fields) {
        // 필드 ID 오름차순 정렬
        List<Integer> sortedFieldIds = fields.stream()
            .map(SearchFilterField::getId)
            .sorted()
            .collect(Collectors.toList());
        
        // 필드 이름 매핑
        Map<Integer, String> fieldMap = fields.stream()
            .collect(Collectors.toMap(SearchFilterField::getId, SearchFilterField::getField));
        
        List<String> fieldNames = sortedFieldIds.stream()
            .map(fieldMap::get)
            .collect(Collectors.toList());
            
        return String.join(",", fieldNames);
    }
    
    /**
     * 리스트의 모든 부분집합 생성 (빈 집합 제외)
     */
    public static <T> List<List<T>> generateNonEmptySubsets(List<T> list) {
        List<List<T>> result = new ArrayList<>();
        backtrack(list, 0, new ArrayList<>(), result);
        return result;
    }
    
    private static <T> void backtrack(List<T> list, int start, List<T> current, List<List<T>> result) {
        if (!current.isEmpty()) {
            result.add(new ArrayList<>(current));
        }
        
        for (int i = start; i < list.size(); i++) {
            current.add(list.get(i));
            backtrack(list, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
    
    /**
     * 여러 리스트의 데카르트 곱을 계산
     */
    public static <T> List<List<T>> cartesianProduct(List<List<T>> lists) {
        List<List<T>> result = new ArrayList<>();
        
        if (lists.isEmpty()) {
            return result;
        }
        
        // 초기값 설정
        List<T> firstList = lists.get(0);
        for (T item : firstList) {
            List<T> temp = new ArrayList<>();
            temp.add(item);
            result.add(temp);
        }
        
        // 나머지 리스트에 대해 데카르트 곱 계산
        for (int i = 1; i < lists.size(); i++) {
            List<T> currentList = lists.get(i);
            List<List<T>> newResult = new ArrayList<>();
            
            for (List<T> resultItem : result) {
                for (T item : currentList) {
                    List<T> newCombination = new ArrayList<>(resultItem);
                    newCombination.add(item);
                    newResult.add(newCombination);
                }
            }
            
            result = newResult;
        }
        
        return result;
    }
} 