package com.matsinger.barofishserver.domain.product.index.infra;

public interface RedisProductIdCsvTarget {
    Integer getCategoryId();
    Integer getSubCategoryId();
    String getProductIds();  // "1,2,3,4"
}