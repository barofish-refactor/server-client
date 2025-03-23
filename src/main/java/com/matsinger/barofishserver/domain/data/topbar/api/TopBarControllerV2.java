package com.matsinger.barofishserver.domain.data.topbar.api;

import com.matsinger.barofishserver.domain.data.topbar.application.TopBarCommandService;
import com.matsinger.barofishserver.domain.data.topbar.application.TopBarQueryService;
import com.matsinger.barofishserver.domain.data.topbar.domain.TopBar;
import com.matsinger.barofishserver.domain.data.topbar.domain.TopBarProductMap;
import com.matsinger.barofishserver.domain.product.application.ProductQueryService;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.product.dto.ProductListDto;
import com.matsinger.barofishserver.jwt.JwtService;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/topbar")
public class TopBarControllerV2 {

    private final JwtService jwt;
    private final Common utils;
    private final TopBarCommandService topBarCommandService;
    private final TopBarQueryService topBarQueryService;
    private final ProductService productService;
    private final ProductQueryService productQueryService;

    @GetMapping("")
    public ResponseEntity<CustomResponse<List<TopBar>>> selectTopBarListV2() {
        CustomResponse<List<TopBar>> response = new CustomResponse<>();

        List<TopBar> topBarList = topBarQueryService.selectTopBarList();
        response.setData(Optional.ofNullable(topBarList));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/count")
    public ResponseEntity<CustomResponse<Long>> selectTopBarCountV2(@PathVariable("id") Integer id,
                                                                  @RequestParam(value = "categoryIds", required = false) String categoryIds,
                                                                  @RequestParam(value = "filterFieldIds", required = false) String filterFieldIds) {
        CustomResponse<Long> response = new CustomResponse<>();

        Long count = (long) productQueryService.countTopBarProduct(
                id,
                utils.str2IntList(filterFieldIds),
                utils.str2IntList(categoryIds)
        );
        response.setIsSuccess(true);
        response.setData(Optional.of(count));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<Page<ProductListDto>>> selectTopBarV2(@PathVariable("id") Integer id,
                                                                             @RequestParam(value = "page", defaultValue = "1", required = false) Integer page,
                                                                             @RequestParam(value = "take", defaultValue = "10", required = false) Integer take,
                                                                             @RequestParam(value = "filterFieldIds", required = false) String filterFieldIds) {
        CustomResponse<Page<ProductListDto>> res = new CustomResponse<>();
        Page<ProductListDto> productListDtos = productQueryService.selectTopBarProductList(
                id,
                PageRequest.of(page - 1, take),
                utils.str2IntList(filterFieldIds));
        res.setIsSuccess(true);
        res.setData(Optional.of(productListDtos));

        return ResponseEntity.ok(res);
    }

    @PostMapping("/add")
    public ResponseEntity<CustomResponse<TopBar>> addTopBarV2(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                            @RequestPart(value = "name") String name) {
        CustomResponse<TopBar> res = new CustomResponse<>();

        jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        name = utils.validateString(name, 20L, "이름");
        TopBar topBar = new TopBar();
        topBar.setName(name);
        TopBar result = topBarCommandService.add(topBar);
        res.setData(Optional.ofNullable(result));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<CustomResponse<TopBar>> updateTopBarV2(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                               @PathVariable("id") Integer id,
                                                               @RequestPart(value = "name") String name) throws Exception {
        CustomResponse<TopBar> res = new CustomResponse<>();

        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        TopBar topbar = topBarQueryService.selectTopBar(id);
        name = utils.validateString(name, 20L, "이름");
        topbar.setName(name);
        TopBar result = topBarCommandService.update(id, topbar);
        res.setData(Optional.ofNullable(result));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/add-product")
    public ResponseEntity<CustomResponse<TopBarProductMap>> addProductToTopBarV2(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                               @RequestPart(value = "topBarId") Integer topBarId,
                                                                               @RequestPart(value = "productId") Integer productId) {
        CustomResponse<TopBarProductMap> res = new CustomResponse<>();

        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        TopBar topBar = topBarQueryService.selectTopBar(topBarId);
        Product product = productService.findById(productId);
        TopBarProductMap data = new TopBarProductMap();
        data.setTopBar(topBar);
        data.setProduct(product);
        TopBarProductMap result = topBarCommandService.addProduct(data);
        res.setData(Optional.ofNullable(result));
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CustomResponse<Boolean>> deleteTopBarV2(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                @PathVariable("id") Integer id) {
        CustomResponse<Boolean> res = new CustomResponse<>();

        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);

        TopBar topBar = topBarQueryService.selectTopBar(id);
        Boolean result = topBarCommandService.delete(id);
        res.setData(Optional.ofNullable(result));
        res.setIsSuccess(result);
        return ResponseEntity.ok(res);
    }
}
