package com.matsinger.barofishserver.domain.compare.filter.application;

import com.matsinger.barofishserver.domain.category.filter.repository.CategoryFilterMapRepository;
import com.matsinger.barofishserver.domain.compare.filter.domain.CompareFilter;
import com.matsinger.barofishserver.domain.compare.filter.repository.CompareFilterRepository;
import com.matsinger.barofishserver.domain.product.productfilter.repository.ProductFilterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class CompareFilterCommandService {
    private final CompareFilterRepository compareFilterRepository;
    private final ProductFilterRepository productFilterRepository;
    private final CategoryFilterMapRepository categoryFilterRepository;
    public CompareFilter addCompareFilter(CompareFilter compareFilter) {
        return compareFilterRepository.save(compareFilter);
    }

    public CompareFilter updateCompareFilter(CompareFilter compareFilter) {
        return compareFilterRepository.save(compareFilter);
    }

    public void deleteCompareFilter(Integer filterId) {
        productFilterRepository.deleteAllByCompareFilterId(filterId);
        categoryFilterRepository.deleteAllByCompareFilterId(filterId);
        compareFilterRepository.deleteById(filterId);
    }
}
