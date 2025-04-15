package com.matsinger.barofishserver.domain.product.filter.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FilterConverter {

    public static String convert(List<Integer> list) {
        return list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public static  List<String> splitCsv(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
