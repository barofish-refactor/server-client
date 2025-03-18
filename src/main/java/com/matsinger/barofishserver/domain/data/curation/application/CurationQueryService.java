package com.matsinger.barofishserver.domain.data.curation.application;

import com.matsinger.barofishserver.domain.data.curation.domain.Curation;
import com.matsinger.barofishserver.domain.data.curation.domain.CurationProductMap;
import com.matsinger.barofishserver.domain.data.curation.domain.CurationState;
import com.matsinger.barofishserver.domain.data.curation.dto.CurationDto;
import com.matsinger.barofishserver.domain.data.curation.repository.CurationProductMapRepository;
import com.matsinger.barofishserver.domain.data.curation.repository.CurationProductMapQueryRepository;
import com.matsinger.barofishserver.domain.data.curation.repository.CurationRepository;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.dto.ProductListDto;
import com.matsinger.barofishserver.domain.product.repository.ProductRepository;
import com.matsinger.barofishserver.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class CurationQueryService {
    private final CurationRepository curationRepository;
    private final CurationProductMapRepository curationProductRepository;
    private final CurationProductMapQueryRepository curationProductMapQueryRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public Page<Curation> selectCurationListByAdmin(PageRequest pageRequest) {
        return curationRepository.findAll(pageRequest);
    }

    public List<Curation> selectCurations() {
        return curationRepository.findAll(Sort.by(Sort.Direction.ASC, "sortNo"));
    }

    public List<Curation> selectCurationState(CurationState state) {
        return curationRepository.findAllByState(state, Sort.by(Sort.Direction.ASC, "sortNo"));
    }

    public List<Product> selectCurationProducts(Integer curationId) {
        List<CurationProductMap> curationProductMapList = curationProductRepository.findAllByCuration_Id(curationId);
        List<Product> products = new ArrayList<>();
        for (CurationProductMap item : curationProductMapList) {
            products.add(item.getProduct());
        }
        return products;
    }

    public List<Product> selectCurationProducts(Integer curationId, PageRequest pageRequest) {
        List<CurationProductMap>
                curationProductMapList =
                curationProductRepository.findAllByCuration_Id(curationId, pageRequest);
        List<Product> products = new ArrayList<>();
        for (CurationProductMap item : curationProductMapList) {
            products.add(item.getProduct());
        }
        return products;
    }
    public Curation selectCuration(Integer id) {
        return curationRepository.findById(id).orElseThrow(() -> {
            throw new BusinessException("큐레이션 정보를 찾을 수 없습니다.");
        });
    }
    public Integer selectMaxSortNo() {
        return Integer.valueOf(curationRepository.selectMaxSortNo().get("sortNo").toString());
    }

    public List<CurationDto> getCurations(Integer userId) {
        List<Curation> curations = selectCurationState(CurationState.ACTIVE);
        if (curations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> curationIds = curations.stream()
                .map(Curation::getId)
                .collect(Collectors.toList());

        Map<Integer, List<Product>> productsByCuration = selectProductsByCurationIds(curationIds, PageRequest.of(0, 10));
        List<Product> allProducts = productsByCuration.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, ProductListDto> productDtoMap = productService.convertProductsToListDtosMap(allProducts, userId);

        return curations.stream().map(curation -> {
            CurationDto curationDto = curation.convert2Dto();
            
            List<Product> products = productsByCuration.get(curation.getId());
            List<ProductListDto> productDtos = products.stream()
                    .map(product -> productDtoMap.get(product.getId()))
                    .collect(Collectors.toList());
            
            curationDto.setProducts(productDtos);
            return curationDto;
        }).collect(Collectors.toList());
    }

    public Map<Integer, List<Product>> selectProductsByCurationIds(List<Integer> curationIds, PageRequest pageRequest) {
        Map<Integer, List<CurationProductMap>> mapsByCuration = curationProductMapQueryRepository
                .findAllByCurationIdInWithPaging(curationIds, pageRequest.getPageSize());


        Map<Integer, List<Product>> result = new HashMap<>();
        for (Integer curationId : curationIds) {
            List<CurationProductMap> maps = mapsByCuration.get(curationId);
            List<Product> products = maps.stream()
                    .map(CurationProductMap::getProduct)
                    .collect(Collectors.toList());
            result.put(curationId, products);
        }
        
        return result;
    }
}
