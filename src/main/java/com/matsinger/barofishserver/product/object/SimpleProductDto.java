package com.matsinger.barofishserver.product.object;

import com.matsinger.barofishserver.category.CategoryDto;
import com.matsinger.barofishserver.compare.filter.CompareFilterDto;
import com.matsinger.barofishserver.inquiry.InquiryDto;
import com.matsinger.barofishserver.product.filter.ProductFilterValueDto;
import com.matsinger.barofishserver.review.object.ReviewDto;
import com.matsinger.barofishserver.review.object.ReviewTotalStatistic;
import com.matsinger.barofishserver.searchFilter.object.SearchFilterFieldDto;
import com.matsinger.barofishserver.store.object.SimpleStore;
import lombok.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleProductDto {
    Integer id;
    ProductState state;
    SimpleStore store;
    CategoryDto category;
    String[] images;
    String title;
    Boolean isLike;
    Integer discountPrice;
    Integer originPrice;
    String deliveryInfo;
    Integer deliveryFee;
    Integer deliverBoxPerAmount;
    String description;
    String[] descriptionImages;
    Integer expectedDeliverDay;
    Integer representOptionItemId;
    Boolean needTaxation;
    Timestamp createdAt;
    List<CompareFilterDto> compareFilters;
    List<ProductFilterValueDto> filterValues;
    List<SearchFilterFieldDto> searchFilterFields;
    List<ProductListDto> comparedProduct = new ArrayList<>();

    ReviewTotalStatistic reviewStatistics;
    List<ReviewDto> reviews = new ArrayList<>();
    Integer reviewCount;
    List<InquiryDto> inquiries = new ArrayList<>();
}
