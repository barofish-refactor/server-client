package com.matsinger.barofishserver.domain.category.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.matsinger.barofishserver.domain.category.dto.CategoryDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Table(name = "category", schema = "barofish_dev", catalog = "")
public class Category {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "parent_category_id", nullable = true)
    private Integer categoryId;
    @Basic
    @Column(name = "image", nullable = true, length = -1)
    private String image;
    @Basic
    @Column(name = "name", nullable = false, length = 20)
    private String name;
    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Category parentCategory;

    @Builder.Default
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parentCategory")
    private List<Category> categoryList = new ArrayList<>();

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategoryList(List<Category> categoryList) {
        this.categoryList = categoryList;
    }

    public CategoryDto convert2Dto() {
        List<CategoryDto> categories = new ArrayList<>();
        String parentName = null;
        if (this.categoryId == null) {
            for (Category category : this.categoryList) {
                categories.add(category.convert2Dto());
            }
        } else {
            parentName = this.parentCategory.getName();
        }
        return CategoryDto.builder().id(this.id).parentId(this.categoryId).name(this.name).image(this.image).ParentCategoryName(
                parentName).categories(categories).build();
    }

    public boolean isParent() {
        return categoryId == null;
    }
}
