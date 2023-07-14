package com.matsinger.barofishserver.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    public List<Category> findAllByCategoryId(Integer id);

    public List<Category> findAllByCategoryIdIsNull();

    Optional<Category> findFirstByName(String categoryName);

    Optional<Category> findFirstByNameAndCategoryId(String categoryName, Integer categoryId);
}
