package com.matsinger.barofishserver.domain.product.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.product.dto.ProductListDto;
import com.matsinger.barofishserver.domain.product.option.domain.Option;
import com.matsinger.barofishserver.domain.product.optionitem.domain.OptionItem;
import com.matsinger.barofishserver.domain.product.productfilter.domain.ProductFilterValue;
import com.matsinger.barofishserver.domain.review.domain.Review;
import com.matsinger.barofishserver.domain.store.domain.ConditionalObject;
import com.matsinger.barofishserver.domain.store.domain.StoreInfo;
import com.matsinger.barofishserver.global.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "product", schema = "barofish_dev", catalog = "")
public class Product implements ConditionalObject {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @Basic
    @Column(name = "store_id", nullable = false)
    private int storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", insertable = false, updatable = false)
    private StoreInfo storeInfo;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Option> options = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private ProductState state;
    @Basic
    @Column(name = "images", nullable = false, length = -1)
    private String images;
    @Basic
    @Column(name = "title", nullable = false, length = 100)
    private String title;
    @Basic
    @Column(name = "origin_price", nullable = false)
    private int originPrice;
    @Basic
    @Column(name = "discount_rate", nullable = false)
    private int discountRate;
    @Basic
    @Column(name = "delivery_info", nullable = false, length = -1)
    private String deliveryInfo;
    @Basic
    @Column(name = "description_images", nullable = false, length = -1)
    private String descriptionImages;
    @Basic
    @Column(name = "expected_deliver_day", nullable = false)
    private int expectedDeliverDay;

    @Column(name = "forwarding_time", nullable = false)
    private String forwardingTime;

    @Basic
//    @Max(value = (long) 0.1, message = "상품 적립 포인트는 10퍼센트를 초과할 수 없습니다.")
    @Column(name = "point_rate", nullable = true)
    private Float pointRate;

    @Basic
    @Column(name = "represent_item_id", nullable = true)
    private Integer representOptionItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "represent_item_id", insertable = false, updatable = false)
    private OptionItem representOptionItem;

    @Basic
    @Column(name = "deliver_box_per_amount", nullable = true)
    private Integer deliverBoxPerAmount;

    @Basic
    @Column(name = "need_taxation", nullable = false)
    private Boolean needTaxation;

    @Basic
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Basic
    @Column(name = "promotion_start_at", nullable = true)
    private Timestamp promotionStartAt;

    @Basic
    @Column(name = "promotion_end_at", nullable = true)
    private Timestamp promotionEndAt;

    @Basic
    @Enumerated(EnumType.STRING)
    @ColumnDefault("FREE")
    @Column(name = "deliver_fee_type", nullable = false)
    private ProductDeliverFeeType deliverFeeType;
    @Basic
    @ColumnDefault("0")
    @Column(name = "deliver_fee", nullable = false)
    private Integer deliverFee;
    @Basic
    @Column(name = "min_order_price", nullable = true)
    private Integer minOrderPrice;

    @Builder.Default
    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "product")
    private List<Review> reviews = new ArrayList<>();
    // @Column(name = "delivery_fee", nullable = true)
    // private int deliveryFee;

    @Column(name = "item_code")
    private String itemCode;

    @Column(name = "difficulty_level_of_trimming", length =10)
    private Double difficultyLevelOfTrimming;

    @Column(name = "the_scent_of_the_sea", length = 10)
    private Double theScentOfTheSea;

    @Column(name = "recommended_cooking_way", length = 10)
    private String recommendedCookingWay;

    @OneToMany
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private List<ProductFilterValue> filterValues;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private ProductSummary productSummary;

    @Column(name = "review_cnt")
    private int reviewCnt;

    public void setPointRate(Float pointRate) {
        this.pointRate = pointRate / 100;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Integer getCategoryId() {
        return category != null ? category.getId() : null;
    }

    public ProductState getState() {
        return state;
    }

    public void setState(ProductState state) {
        this.state = state;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getOriginPrice() {
        return originPrice;
    }

    public void setOriginPrice(int originPrice) {
        this.originPrice = originPrice;
    }

    public int getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(int discountRate) {
        this.discountRate = discountRate;
    }

    public String getDeliveryInfo() {
        return deliveryInfo;
    }

    public void setDeliveryInfo(String deliveryInfo) {
        this.deliveryInfo = deliveryInfo;
    }

    public String getDescriptionImages() {
        return descriptionImages;
    }

    public void setDescriptionImages(String descriptionImages) {
        this.descriptionImages = descriptionImages;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public ProductListDto convert2ListDto() {
        return ProductListDto.builder()
                .id(this.id)
                .image(this.images.substring(1, images.length() - 1).split(", ")[0])
                .originPrice(this.originPrice)
                .title(this.title)
                .build();
    }

    public SimpleProductDto convert2SimpleDto() {

        return SimpleProductDto.builder()
                .id(this.getId())
                .category(this.category != null ? this.category.convert2Dto() : null)
                .expectedDeliverDay(this.getExpectedDeliverDay())
                .images(this.images.substring(1, images.length() - 1).split(", "))
                .title(title)
                .state(this.getState())
                .originPrice(originPrice)
                .deliveryInfo(deliveryInfo)
                .forwardingTime(this.forwardingTime)
                .description(this.descriptionImages)
                .descriptionImages(this.descriptionImages.substring(1, descriptionImages.length() - 1).split(", "))
                .representOptionItemId(this.representOptionItemId)
                .deliverBoxPerAmount(this.getDeliverBoxPerAmount())
                .createdAt(this.getCreatedAt())
                .pointRate(this.getPointRate())
                .promotionStartAt(this.promotionStartAt)
                .promotionEndAt(this.promotionEndAt)
                .minOrderPrice(minOrderPrice)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product that = (Product) o;
        return id == that.id &&
                originPrice == that.originPrice &&
                discountRate == that.discountRate &&
                Objects.equals(state, that.state) &&
                Objects.equals(images, that.images) &&
                Objects.equals(title, that.title) &&
                Objects.equals(deliveryInfo, that.deliveryInfo) &&
                Objects.equals(descriptionImages, that.descriptionImages) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,
                state,
                images,
                title,
                originPrice,
                discountRate,
                deliveryInfo,
                descriptionImages,
                createdAt);
    }

    public boolean isDeliveryTypeFix() {
        return deliverFeeType.equals(ProductDeliverFeeType.FIX);
    }

    public boolean isDeliveryTypeFreeIfOver() {
        return deliverFeeType.equals(ProductDeliverFeeType.FREE_IF_OVER);
    }

    public boolean isDeliveryTypeFree() {
        return deliverFeeType.equals(ProductDeliverFeeType.FREE);
    }

    public boolean isPromotionEnd() {
        if (promotionEndAt == null && promotionEndAt == null) {
            return false;
        }
        if (promotionStartAt.after(Timestamp.valueOf(LocalDateTime.now())) ||
                promotionEndAt.before(Timestamp.valueOf(LocalDateTime.now()))) {
            return true;
        }
        return false;
    }

    public void validateState() {
        if (!state.equals(ProductState.ACTIVE)) {
            if (state.equals(ProductState.SOLD_OUT))
                throw new BusinessException("품절된 상품입니다.");
            if (state.equals(ProductState.DELETED))
                throw new BusinessException("주문 불가능한 상품입니다.");
            if (state.equals(ProductState.INACTIVE))
                throw new BusinessException("주문 불가능한 상품입니다.");
        }
    }

    public int getMaxDeliveryFee(int deliverFee) {
        if (deliverFee > this.deliverFee) {
            return deliverFee;
        }
        return this.deliverFee;
    }

    public boolean isSConditional() {
        return this.deliverFeeType.equals(ProductDeliverFeeType.S_CONDITIONAL);
    }

    @Override
    public Boolean meetConditions(int totalPrice) {
        return totalPrice >= this.minOrderPrice;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public boolean needTaxation() {
        return this.needTaxation == true;
    }

    public String getFirstImageUrl() {
        if (this.images == null) {
            return null;
        }
        return this.images.substring(1, this.images.length() - 1).split(",")[0];
    }

    public Integer getRepresentOptionItemOriginPrice() {
        return this.representOptionItem != null ? this.representOptionItem.getOriginPrice() : null;
    }

    public Integer getRepresentOptionItemDiscountPrice() {
        return this.representOptionItem != null ? this.representOptionItem.getDiscountPrice() : null;
    }

    public String getStoreName() {
        return this.storeInfo != null ? this.storeInfo.getName() : null;
    }

    public String getStoreImageUrl() {
        return this.storeInfo != null ? this.storeInfo.getProfileImage() : null;
    }
}
