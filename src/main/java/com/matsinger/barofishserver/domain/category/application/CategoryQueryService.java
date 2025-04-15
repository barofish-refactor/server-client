package com.matsinger.barofishserver.domain.category.application;

import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class CategoryQueryService {
    private final CategoryRepository categoryRepository;

    public List<Category> findParentCategories() {
        return categoryRepository.findAllByCategoryIdIsNull();
    }

    public List<Category> findAll(Integer id) {
        if (id != null) return categoryRepository.findAllByCategoryId(id);
        return categoryRepository.findAll();
    }

    public Category findById(Integer id) {
        return categoryRepository.findById(id).orElseThrow(() -> new Error("카테고리 정보를 찾을 수 없습니다."));
    }

    public Optional<Category> findOptionalCategoryWithName(String name) {
        return categoryRepository.findFirstByName(name);
    }

    public Optional<Category> findOptionalCategoryWithName(String name, Integer parentCategoryId) {
        return categoryRepository.findFirstByNameAndCategoryId(name, parentCategoryId);
    }

    public List<Category> findALLParent() {
        return categoryRepository.findAllParentCategoriesWithChildren();
    }
}
