package com.matsinger.barofishserver.searchFilter;

import com.matsinger.barofishserver.jwt.JwtService;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.product.ProductService;
import com.matsinger.barofishserver.searchFilter.object.SearchFilter;
import com.matsinger.barofishserver.searchFilter.object.SearchFilterDto;
import com.matsinger.barofishserver.searchFilter.object.SearchFilterField;
import com.matsinger.barofishserver.searchFilter.object.SearchFilterFieldDto;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.CustomResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search-filter")
public class SearchFilterController {

    private final SearchFilterService searchFilterService;
    private final ProductService productService;
    private final JwtService jwt;
    private final Common utils;

    @GetMapping("/list")
    public ResponseEntity<CustomResponse<List<SearchFilterDto>>> selectSearchFilterList() {
        CustomResponse<List<SearchFilterDto>> res = new CustomResponse<>();
        try {
            List<SearchFilterDto>
                    searchFilterDtos =
                    searchFilterService.selectSearchFilterList().stream().map(searchFilterService::convertFilterDto).toList();
            res.setData(Optional.ofNullable(searchFilterDtos));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<SearchFilterDto>> selectSearchFilter(@PathVariable("id") Integer id) {
        CustomResponse<SearchFilterDto> res = new CustomResponse<>();
        try {
            if (id == null) return res.throwError("검색 필터 아이디를 입력해주세요.", "INPUT_CHECK_REQUIRED");
            SearchFilterDto
                    searchFilterDto =
                    searchFilterService.convertFilterDto(searchFilterService.selectSearchFilter(id));
            res.setData(Optional.ofNullable(searchFilterDto));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @Getter
    @NoArgsConstructor
    private static class AddSearchFilterReq {
        String name;
    }

    @PostMapping("/add")
    public ResponseEntity<CustomResponse<SearchFilterDto>> addSearchFilter(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                           @RequestPart(value = "data") AddSearchFilterReq data) {
        CustomResponse<SearchFilterDto> res = new CustomResponse<>();
        Optional<TokenInfo> tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);
        if (tokenInfo == null) return res.throwError("인증이 필요합니다.", "FORBIDDEN");
        try {
            String name = utils.validateString(data.name, 20L, "이름");
            SearchFilter searchFilter = SearchFilter.builder().name(name).build();
            SearchFilterDto
                    searchFilterDto =
                    searchFilterService.convertFilterDto(searchFilterService.addSearchFilter(searchFilter));
            res.setData(Optional.ofNullable(searchFilterDto));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @PostMapping("/update/{id}")
    public ResponseEntity<CustomResponse<SearchFilterDto>> addSearchFilter(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                           @PathVariable("id") Integer id,
                                                                           @RequestPart(value = "data") AddSearchFilterReq data) {
        CustomResponse<SearchFilterDto> res = new CustomResponse<>();
        Optional<TokenInfo> tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);
        if (tokenInfo == null) return res.throwError("인증이 필요합니다.", "FORBIDDEN");
        try {
            SearchFilter searchFilter = searchFilterService.selectSearchFilter(id);
            if (data.name != null) {
                String name = utils.validateString(data.name, 20L, "이름");
                searchFilter.setName(name);
            }
            SearchFilterDto
                    searchFilterDto =
                    searchFilterService.convertFilterDto(searchFilterService.updateSearchFilter(searchFilter));
            res.setData(Optional.ofNullable(searchFilterDto));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CustomResponse<Boolean>> deleteSearchFilter(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                      @PathVariable("id") Integer id) {
        CustomResponse<Boolean> res = new CustomResponse<>();
        Optional<TokenInfo> tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);
        if (tokenInfo == null) return res.throwError("인증이 필요합니다.", "FORBIDDEN");
        try {
            SearchFilter searchFilter = searchFilterService.selectSearchFilter(id);
            searchFilterService.deleteSearchFilter(id);
            res.setData(Optional.of(true));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    // SearchFilterField
    @GetMapping("/{filterId}/list")
    public ResponseEntity<CustomResponse<List<SearchFilterFieldDto>>> selectSearchFilterFieldList(@PathVariable("filterId") Integer filterId) {
        CustomResponse<List<SearchFilterFieldDto>> res = new CustomResponse<>();
        try {
            List<SearchFilterField>
                    searchFilterFields =
                    searchFilterService.selectSearchFilterFieldWithFilterId(filterId);
            res.setData(Optional.of(searchFilterFields.stream().map(SearchFilterField::convert2Dto).toList()));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @Getter
    @NoArgsConstructor
    private static class AddSearchFilterFiledReq {
        Integer searchFilterId;
        String field;
    }

    @PostMapping("/field/add")
    public ResponseEntity<CustomResponse<SearchFilterFieldDto>> addSearchFilterField(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                                     @RequestPart(value = "data") AddSearchFilterFiledReq data) {
        CustomResponse<SearchFilterFieldDto> res = new CustomResponse<>();
        Optional<TokenInfo> tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);
        if (tokenInfo == null) return res.throwError("인증이 필요합니다.", "FORBIDDEN");
        try {
            if (data.searchFilterId == null) return res.throwError("필터 아이디를 입력해주세요.", "INPUT_CHECK_REQRUIED");
            if (data.field == null) return res.throwError("필드를 입력해주세요.", "INPUT_CHECK_REQUIRED");
            String field = utils.validateString(data.field, 20L, "필드명");
            SearchFilterField
                    searchFilterField =
                    SearchFilterField.builder().searchFilterId(data.searchFilterId).field(field).build();
            SearchFilterFieldDto
                    searchFilterFieldDto =
                    searchFilterService.addSearchFilterField(searchFilterField).convert2Dto();
            res.setData(Optional.ofNullable(searchFilterFieldDto));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @Getter
    @NoArgsConstructor
    private static class UpdateSearchFilterFiledReq {
        String field;
    }

    @PostMapping("/field/update/{id}")
    public ResponseEntity<CustomResponse<SearchFilterFieldDto>> updateSearchFilterField(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                                        @PathVariable("id") Integer id,
                                                                                        @RequestPart(value = "data") UpdateSearchFilterFiledReq data) {
        CustomResponse<SearchFilterFieldDto> res = new CustomResponse<>();
        Optional<TokenInfo> tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);
        if (tokenInfo == null) return res.throwError("인증이 필요합니다.", "FORBIDDEN");
        try {
            SearchFilterField searchFilterField = searchFilterService.selectSearchFilterField(id);
            if (data.field != null) {
                String field = utils.validateString(data.field, 20L, "필드명");
                searchFilterField.setField(field);
            }
            SearchFilterField result = searchFilterService.updateSearchFilterField(searchFilterField);
            res.setData(Optional.ofNullable(result.convert2Dto()));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }

    @DeleteMapping("/field/{id}")
    public ResponseEntity<CustomResponse<Boolean>> deleteSearchFilterField(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                           @PathVariable("id") Integer id) {
        CustomResponse<Boolean> res = new CustomResponse<>();
        Optional<TokenInfo> tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ADMIN), auth);
        if (tokenInfo == null) return res.throwError("인증이 필요합니다.", "FORBIDDEN");
        try {
            SearchFilterField searchFilterField = searchFilterService.selectSearchFilterField(id);
            searchFilterService.deleteSearchFilterField(id);
            res.setData(Optional.of(true));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return res.defaultError(e);
        }
    }
}
