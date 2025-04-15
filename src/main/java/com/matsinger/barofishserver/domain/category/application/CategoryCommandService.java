package com.matsinger.barofishserver.domain.category.application;

import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.category.dto.CategoryDto;
import com.matsinger.barofishserver.domain.category.filter.application.CategoryFilterQueryService;
import com.matsinger.barofishserver.domain.category.filter.repository.CategoryFilterMapRepository;
import com.matsinger.barofishserver.domain.category.repository.CategoryRepository;
import com.matsinger.barofishserver.domain.compare.filter.application.CompareFilterQueryService;
import com.matsinger.barofishserver.domain.compare.filter.dto.CompareFilterDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class CategoryCommandService {
    private final CategoryRepository categoryRepository;
    private final CategoryFilterQueryService categoryFilterQueryService;
    private final CategoryFilterMapRepository categoryFilterRepository;
    private final CompareFilterQueryService compareFilterQueryService;

    public Category add(Category category) {
        return categoryRepository.save(category);
    }

    public Category update(Integer id, Category data) {
        Category category = categoryRepository.findById(id).get();
        if (data.getName() != null) category.setName(data.getName());
        if (data.getImage() != null) category.setImage(data.getImage());
        categoryRepository.save(category);
        return category;
    }

    @Transactional
    public Boolean delete(Integer id) {
        try {
            categoryFilterRepository.deleteAllByCategoryId(id);
            categoryRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public CategoryDto convert2Dto(Category data) {
        List<CategoryDto> categories = new ArrayList<>();
        String parentName = null;
        if (data.getCategoryId() == null) {
            for (Category category : data.getCategoryList()) {
                categories.add(convert2Dto(category));
            }
            List<CompareFilterDto>
                    compareFilterDtos =
                    categoryFilterQueryService.selectCompareFilterIdList(data.getId()).stream().map(v -> compareFilterQueryService.selectCompareFilter(
                            v).convert2Dto()).toList();
        } else {
            parentName = data.getParentCategory().getName();
        }
        return CategoryDto.builder().id(data.getId()).parentId(data.getCategoryId()).name(data.getName()).image(data.getImage()).ParentCategoryName(
                parentName).categories(categories).build();
    }
}
