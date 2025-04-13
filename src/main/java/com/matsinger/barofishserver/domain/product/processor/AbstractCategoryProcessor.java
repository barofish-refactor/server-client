package com.matsinger.barofishserver.domain.product.processor;

import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.category.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public abstract class AbstractCategoryProcessor {
    @Autowired
    protected CategoryRepository categoryRepository;

    public void processCategories() {
        try {
            List<Category> pCategories = categoryRepository.findAllByCategoryIdIsNull();
            for (Category pCategory : pCategories) {
                for (Category cCategory : pCategory.getCategoryList()) {
                    processCategory(cCategory);
                }
                processCategory(pCategory);
            }
        } catch (Exception e) {
            log.error("카테고리 처리 중 오류 발생", e);
            throw e;
        }
    }

    protected abstract void processCategory(Category category);
} 