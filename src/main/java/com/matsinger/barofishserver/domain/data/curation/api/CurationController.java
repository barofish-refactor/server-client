package com.matsinger.barofishserver.domain.data.curation.api;

import com.matsinger.barofishserver.domain.data.curation.application.CurationCommandService;
import com.matsinger.barofishserver.domain.data.curation.application.CurationQueryService;
import com.matsinger.barofishserver.domain.data.curation.domain.*;
import com.matsinger.barofishserver.domain.data.curation.dto.CurationDeleteProductReq;
import com.matsinger.barofishserver.domain.data.curation.dto.CurationDto;
import com.matsinger.barofishserver.domain.data.curation.dto.SortCurationReq;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.dto.ProductListDto;
import com.matsinger.barofishserver.global.exception.BusinessException;
import com.matsinger.barofishserver.jwt.JwtService;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.CustomResponse;
import com.matsinger.barofishserver.utils.S3.Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Description;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/curation")
public class CurationController {

    private final CurationQueryService curationQueryService;
    private final CurationCommandService curationCommandService;


    private final ProductService productService;

    private final Common util;

    private final Uploader s3;
    private final JwtService jwt;

    // GET
    @Description("큐레이션 목록")
    @GetMapping("/")
    public ResponseEntity<CustomResponse<List<CurationDto>>> selectCurationList() {
        CustomResponse<List<CurationDto>> res = new CustomResponse<>();

        List<Curation> curations = curationQueryService.selectCurations();
        List<CurationDto> curationDtos = new ArrayList<>();
        for (Curation curation : curations) {
            List<Product> products = curationQueryService.selectCurationProducts(curation.getId());
            CurationDto curationDto = curation.convert2Dto();
            curationDto.setProducts(products.stream().map(productService::convert2ListDto).toList());
            curationDtos.add(curationDto);
        }
        res.setData(Optional.of(curationDtos));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/management")
    public ResponseEntity<CustomResponse<Page<CurationDto>>> selectCurationListByAdmin(@RequestHeader(value = "Authorization", required = false) Optional<String> auth,
                                                                                       @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                                       @RequestParam(value = "take", required = false, defaultValue = "10") Integer take,
                                                                                       @RequestParam(value = "orderby", defaultValue = "id") CurationOrderBy orderBy,
                                                                                       @RequestParam(value = "orderType", defaultValue = "DESC") Sort.Direction sort) {
        CustomResponse<Page<CurationDto>> res = new CustomResponse<>();

                jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        PageRequest pageRequest = PageRequest.of(page, take, Sort.by(sort, orderBy.label));
        Page<Curation> curations = curationQueryService.selectCurationListByAdmin(pageRequest);
        Page<CurationDto> curationDtos = curations.map(curation -> {
            List<Product> products = curationQueryService.selectCurationProducts(curation.getId());
            CurationDto curationDto = curation.convert2Dto();
            curationDto.setProducts(products.stream().map(productService::convert2ListDto).toList());
            return curationDto;
        });

        res.setData(Optional.of(curationDtos));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<CurationDto>> selectCuration(@PathVariable("id") Integer id) {
        CustomResponse<CurationDto> res = new CustomResponse<>();

        Curation curation = curationQueryService.selectCuration(id);
        List<Product> products = curationQueryService.selectCurationProducts(curation.getId());
        CurationDto curationDto = curation.convert2Dto();
        curationDto.setProducts(products.stream().map(productService::convert2ListDto).toList());
        res.setData(Optional.of(curationDto));
        return ResponseEntity.ok(res);
    }

    @Description("큐레이션 상품 목록")
    @GetMapping("/{id}/products")
    public ResponseEntity<CustomResponse<List<ProductListDto>>> selectCurationProducts(@PathVariable("id") Integer id) {
        CustomResponse<List<ProductListDto>> res = new CustomResponse<>();

        List<ProductListDto>
                products =
                curationQueryService.selectCurationProducts(id).stream().map(productService::convert2ListDto).toList();
        res.setData(Optional.of(products));
        return ResponseEntity.ok(res);
    }

    //POST
    @Description("큐레이션 추가")
    @PostMapping(value = "/add", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<CustomResponse<Curation>> createCuration(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                   @RequestPart(value = "image", required = false) MultipartFile file,
                                                                   @RequestPart(value = "shortName", required = false) String shortName,
                                                                   @RequestPart(value = "title", required = false) String title,
                                                                   @RequestPart(value = "description", required = false) String description,
                                                                   @RequestPart(value = "type", required = false) CurationType type,
                                                                   @RequestPart(value = "state", required = false) CurationState state) throws Exception {
        CustomResponse<Curation> res = new CustomResponse<>();

                jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Curation curation = new Curation();
        if (shortName == null && title == null)
            throw new BusinessException("큐레이션 명과 타이틀 둘 중 하나는 필수입니다.");
        if (shortName != null) {
            if (file == null) throw new BusinessException("이미지를 입력해주세요.");
            shortName = util.validateString(shortName, 20L, "약어");
            curation.setShortName(shortName);
            String image = s3.upload(file, new ArrayList<>(List.of("curation")));
            curation.setImage(image);
        }
        if (title != null) {
            if (type == null) throw new BusinessException("타입을 입력해주세요.");
            if (description == null) throw new BusinessException("설명을 입력해주세요.");
            title = util.validateString(title, 100L, "제목");
            curation.setTitle(title);
            description = util.validateString(description, 200L, "설명");
            curation.setDescription(description);
            curation.setType(type);
            if (state == null) {
                curation.setState(CurationState.INACTIVE);
            }
            if (state != null) {
            curation.setState(state);
            }
        }
        curation.setSortNo(curationQueryService.selectMaxSortNo());
        Curation data = curationCommandService.add(curation);
        res.setData(Optional.ofNullable(data));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<CustomResponse<Curation>> updateCuration(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                   @PathVariable("id") Integer id,
                                                                   @RequestPart(value = "image", required = false) MultipartFile file,
                                                                   @RequestPart(value = "shortName", required = false) String shortName,
                                                                   @RequestPart(value = "title", required = false) String title,
                                                                   @RequestPart(value = "description", required = false) String description,
                                                                   @RequestPart(value = "type", required = false) CurationType type,
                                                                   @RequestPart(value = "state", required = false)CurationState state) throws Exception {
        CustomResponse<Curation> res = new CustomResponse<>();

                jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);


        Curation curation = curationQueryService.selectCuration(id);
        if (file != null) {
            if (!s3.validateImageType(file)) throw new BusinessException("허용되지 않는 확장자입니다.");
            String imageUrl = s3.upload(file, new ArrayList<>(List.of("curation")));
            curation.setImage(imageUrl);
        }
        if (shortName != null) {
            shortName = util.validateString(shortName, 20L, "이름");
            curation.setShortName(shortName);
        }
        if (title != null) {
            title = util.validateString(title, 100L, "제목");
            curation.setTitle(title);
        }
        if (description != null) {
            description = util.validateString(description, 200L, "설명");
            curation.setDescription(description);
        }
        if (type != null) {
            curation.setType(type);
        }
        if (state != null) {
            curation.setState(state);
        }
        curationCommandService.update(curation);
        return ResponseEntity.ok(res);
    }

    @Description("큐레이션 목록 상품 추가")
    @PostMapping("/{id}/add-product")
    public ResponseEntity<CustomResponse<Boolean>> addProductToCuration(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                        @PathVariable("id") Integer id,
                                                                        @RequestPart(value = "data") List<Integer> productIds) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        Curation curation = curationQueryService.selectCuration(id);
        if (curation == null) throw new BusinessException("큐레이션 정보를 찾을 수 없습니다.");
        for (Integer productId : productIds) {
            Product product = productService.findById(productId);
            if (product == null) throw new BusinessException("상품 정보를 찾을 수 없습니다.");
        }
        List<CurationProductMap> result = curationCommandService.addProduct(id, productIds);
        res.setData(Optional.of(true));
        return ResponseEntity.ok(res);
    }

    //DELETE
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<CustomResponse<Curation>> deleteCuration(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                   @PathVariable("id") Integer id) {
        CustomResponse<Curation> res = new CustomResponse<>();

                jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        if (id == 0) throw new BusinessException("삭제 불가능한 큐레이션입니다.");
        Curation curation = curationQueryService.selectCuration(id);
        if (curation == null) throw new BusinessException("큐레이션 데이터를 찾을 수 없습니다.");
        curationCommandService.delete(id);
        List<Curation> curations = curationQueryService.selectCurations();
        for (int i = 0; i < curations.size(); i++) {
            curations.get(i).setSortNo(i + 1);
        }
        curationCommandService.updateAllCuration(curations);
        return ResponseEntity.ok(res);
    }


    @DeleteMapping("/delete-product")
    public ResponseEntity<CustomResponse<Boolean>> deleteProductList(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                     @RequestPart(value = "data") CurationDeleteProductReq data) {
        CustomResponse<Boolean> res = new CustomResponse<>();

                jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        if (data.getCurationId() == null) throw new BusinessException("큐레이션 아이디를 입력해주세요.");
        Curation curation = curationQueryService.selectCuration(data.getCurationId());
        curationCommandService.deleteProducts(data.getCurationId(), data.getProductIds());
        res.setData(Optional.of(true));
        return ResponseEntity.ok(res);
    }


    @PostMapping("/sort-curation")
    public ResponseEntity<CustomResponse<List<CurationDto>>> sortCuration(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                          @RequestPart(value = "data") SortCurationReq data) {
        CustomResponse<List<CurationDto>> res = new CustomResponse<>();

                jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        List<CurationDto> curationDtos = new ArrayList<>();
        List<Curation> curations = new ArrayList<>();
        for (int i = 0; i < data.getCurationIds().size(); i++) {
            Curation curation = curationQueryService.selectCuration(data.getCurationIds().get(i));
            curation.setSortNo(i + 1);
            curations.add(curation);
            curationDtos.add(curation.convert2Dto());
        }
        curationCommandService.updateAllCuration(curations);
        res.setData(Optional.of(curationDtos));
        return ResponseEntity.ok(res);
    }
}
