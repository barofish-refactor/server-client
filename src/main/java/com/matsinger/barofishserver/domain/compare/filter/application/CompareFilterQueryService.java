package com.matsinger.barofishserver.domain.compare.filter.application;

import com.matsinger.barofishserver.domain.category.filter.repository.CategoryFilterMapRepository;
import com.matsinger.barofishserver.domain.compare.filter.domain.CompareFilter;
import com.matsinger.barofishserver.domain.compare.filter.repository.CompareFilterRepository;
import com.matsinger.barofishserver.domain.product.productfilter.repository.ProductFilterRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class CompareFilterQueryService {
    private final CompareFilterRepository compareFilterRepository;
    private final ProductFilterRepository productFilterRepository;
    private final CategoryFilterMapRepository categoryFilterRepository;

    public CompareFilter selectCompareFilter(Integer filterId) {
        return compareFilterRepository.findById(filterId).orElseThrow(() -> {
            throw new BusinessException("비교하기 항목 정보를 찾을 수 없습니다.");
        });
    }

    public List<CompareFilter> selectCompareFilterList() {
        return compareFilterRepository.findAll();
    }

    public List<CompareFilter> selectCompareFilterListWithIds(List<Integer> ids) {
        return compareFilterRepository.findAllByIdIn(ids);
    }
}
