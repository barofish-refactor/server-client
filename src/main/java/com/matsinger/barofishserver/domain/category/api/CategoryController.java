package com.matsinger.barofishserver.domain.category.api;

import com.matsinger.barofishserver.domain.category.application.CategoryCommandService;
import com.matsinger.barofishserver.domain.category.application.CategoryQueryService;
import com.matsinger.barofishserver.domain.category.domain.Category;
import com.matsinger.barofishserver.domain.category.dto.AddCategoryCompareFilterReq;
import com.matsinger.barofishserver.domain.category.dto.CategoryDto;
import com.matsinger.barofishserver.domain.category.filter.application.CategoryFilterCommandService;
import com.matsinger.barofishserver.domain.category.filter.application.CategoryFilterQueryService;
import com.matsinger.barofishserver.domain.category.filter.domain.CategoryFilterId;
import com.matsinger.barofishserver.domain.category.filter.domain.CategoryFilterMap;
import com.matsinger.barofishserver.domain.compare.filter.application.CompareFilterQueryService;
import com.matsinger.barofishserver.domain.compare.filter.domain.CompareFilter;
import com.matsinger.barofishserver.domain.compare.filter.dto.CompareFilterDto;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.domain.ProductState;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.jwt.JwtService;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.CustomResponse;
import com.matsinger.barofishserver.utils.S3.S3Uploader;
import com.matsinger.barofishserver.utils.S3.Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/category")
public class CategoryController {

    private final CategoryQueryService categoryQueryService;
    private final CategoryCommandService categoryCommandService;
    private final CompareFilterQueryService compareFilterQueryService;
    private final CategoryFilterQueryService categoryFilterQueryService;
    private final CategoryFilterCommandService categoryFilterCommandService;
    private final ProductService productService;
    private final Common util;

    private final Uploader s3;
    private final JwtService jwt;

    @GetMapping("/")
    public ResponseEntity<CustomResponse<List<Category>>> selectCategories() {
        CustomResponse<List<Category>> res = new CustomResponse<>();

        List<Category> categories = categoryQueryService.findAll(null);
        res.setData(Optional.ofNullable(categories));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/add")
    public ResponseEntity<CustomResponse<Category>> addCategory(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                @RequestPart(value = "categoryId", required = false) Integer categoryId,
                                                                @RequestPart(value = "name") String name,
                                                                @RequestPart(value = "image", required = false) MultipartFile file) throws Exception {
        CustomResponse<Category> res = new CustomResponse<>();

        
        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Category category = new Category();
        name = util.validateString(name, 20L, "카테고리명");
        category.setName(name);
        String imageUrl = null;
        if (categoryId == null) {
            if (file == null) throw new BusinessException("상위 카테고리의 경우 이미지는 필수입니다.");
            imageUrl = s3.upload(file, new ArrayList<>(List.of("category")));
            category.setImage(imageUrl);
        } else {
            Category parentCategory = categoryQueryService.findById(categoryId);
            category.setCategoryId(parentCategory.getId());
            category.setCategoryId(categoryId);
        }
        categoryCommandService.add(category);
        res.setData(Optional.of(category));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<CustomResponse<Category>> updateCategory(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                   @PathVariable("id") Integer id,
                                                                   @RequestPart(value = "name", required = false) String name,
                                                                   @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {
        CustomResponse<Category> res = new CustomResponse<>();

        
        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);


        Category category = categoryQueryService.findById(id);
        String imageUrl = null;
        if (name != null) {
            name = util.validateString(name, 20L, "카테고리명");
            category.setName(name);
        }
        if (image != null) {
            imageUrl = s3.upload(image, new ArrayList<>(List.of("category")));
            category.setImage(imageUrl);
        }
        categoryCommandService.update(id, category);
        res.setData(Optional.of(category));
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CustomResponse<Category>> deleteCategory(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                   @PathVariable("id") Integer id) {
        CustomResponse<Category> res = new CustomResponse<>();
        
        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Category category = categoryQueryService.findById(id);
        if (category.getCategoryId() == null) {
            List<Category> categories = categoryQueryService.findAll(id);
            if (categories.size() != 0) throw new BusinessException("하위 카테고리가 존재합니다.");
        }
        List<Product> products = productService.selectProductWithCategoryId(category.getId());
        if (products.stream().anyMatch(v -> v.getState().equals(ProductState.ACTIVE)))
            throw new BusinessException("활성화 중인 상품이 있습니다.");
        productService.saveAllProduct(products.stream().peek(v -> v.setCategory(null)).toList());
        categoryCommandService.delete(id);
        return ResponseEntity.ok(res);
    }

    //비교하기 필터-------------------------
    @GetMapping("/compare-filter/{categoryId}")
    public ResponseEntity<CustomResponse<CategoryDto>> selectCategoryCompareFilter(@PathVariable("categoryId") Integer categoryId) {
        CustomResponse<CategoryDto> res = new CustomResponse<>();

        Category category = categoryQueryService.findById(categoryId);
        List<Integer> compareFilterIds = categoryFilterQueryService.selectCompareFilterIdList(categoryId);
        List<CompareFilterDto>
                compareFilterDtos =
                compareFilterQueryService.selectCompareFilterListWithIds(compareFilterIds).stream().map(
                        CompareFilter::convert2Dto).toList();
        CategoryDto
                categoryDto =
                CategoryDto.builder().filters(compareFilterDtos).id(categoryId).name(category.getName()).image(
                        category.getImage()).build();
        res.setData(Optional.ofNullable(categoryDto));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/compare-filter/list")
    public ResponseEntity<CustomResponse<List<CategoryDto>>> selectCategoryCompareFilter() {
        CustomResponse<List<CategoryDto>> res = new CustomResponse<>();

        List<Category> categories = categoryQueryService.findParentCategories();
        List<CategoryDto> categoryDtos = categories.stream().map(category -> {

            List<Integer> compareFilterIds = categoryFilterQueryService.selectCompareFilterIdList(category.getId());
            List<CompareFilterDto>
                    compareFilterDtos =
                    compareFilterQueryService.selectCompareFilterListWithIds(compareFilterIds).stream().map(
                            CompareFilter::convert2Dto).toList();
            return CategoryDto.builder().filters(compareFilterDtos).id(category.getId()).name(category.getName()).image(
                    category.getImage()).build();
        }).toList();
        res.setData(Optional.of(categoryDtos));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/compare-filter/add/")
    public ResponseEntity<CustomResponse<CompareFilterDto>> addCategoryCompareFilter(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                                     @RequestPart(value = "data") AddCategoryCompareFilterReq data) {
        CustomResponse<CompareFilterDto> res = new CustomResponse<>();

        
        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        if (data.getCategoryId() == null) throw new BusinessException("카테고리 아이디를 입력해주세요.");
        if (data.getCompareFilterId() == null)
            throw new BusinessException("비교하기 필터 아이디를 입력해주세요.");
        Category category = categoryQueryService.findById(data.getCategoryId());
        if (category.getCategoryId() != null) throw new BusinessException("1차 카테고리만 선택해주세요.");
        CompareFilter compareFilter = compareFilterQueryService.selectCompareFilter(data.getCompareFilterId());
        CategoryFilterMap
                categoryFilterMap =
                CategoryFilterMap.builder().compareFilterId(data.getCompareFilterId()).categoryId(data.getCategoryId()).build();
        categoryFilterCommandService.addCategoryFilterMap(categoryFilterMap);
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/compare-filter/delete/")
    public ResponseEntity<CustomResponse<CompareFilterDto>> deleteCategoryCompareFilter(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                                        @RequestPart(value = "data") AddCategoryCompareFilterReq data) {
        CustomResponse<CompareFilterDto> res = new CustomResponse<>();

        
        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);


        if (data.getCategoryId() == null) throw new BusinessException("카테고리 아이디를 입력해주세요.");
        if (data.getCompareFilterId() == null)
            throw new BusinessException("비교하기 필터 아이디를 입력해주세요.");
        Category category = categoryQueryService.findById(data.getCategoryId());
        CompareFilter compareFilter = compareFilterQueryService.selectCompareFilter(data.getCompareFilterId());
        CategoryFilterId categoryFilterId = new CategoryFilterId();
        categoryFilterId.setCategoryId(data.getCategoryId());
        categoryFilterId.setCompareFilterId(data.getCompareFilterId());
        categoryFilterCommandService.deleteCategoryFilterMap(categoryFilterId);
        return ResponseEntity.ok(res);
    }
}
