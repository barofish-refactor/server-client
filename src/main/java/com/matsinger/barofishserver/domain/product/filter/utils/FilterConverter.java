package com.matsinger.barofishserver.domain.product.filter.utils;

import java.util.List;
import java.util.stream.Collectors;

public class FilterConverter {

    public static String convert(List<Integer> list) {
        return list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
