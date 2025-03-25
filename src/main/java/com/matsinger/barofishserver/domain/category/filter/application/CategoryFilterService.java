package com.matsinger.barofishserver.domain.category.filter.application;

import com.matsinger.barofishserver.domain.category.filter.repository.CategoryFilterMapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class CategoryFilterService {
    private final CategoryFilterMapRepository categoryFilterRepository;
}
