package com.matsinger.barofishserver.domain.main.api;

import com.matsinger.barofishserver.domain.banner.application.BannerQueryService;
import com.matsinger.barofishserver.domain.banner.domain.Banner;
import com.matsinger.barofishserver.domain.data.curation.application.CurationQueryService;
import com.matsinger.barofishserver.domain.data.curation.domain.Curation;
import com.matsinger.barofishserver.domain.data.curation.domain.CurationState;
import com.matsinger.barofishserver.domain.data.curation.dto.CurationDto;
import com.matsinger.barofishserver.domain.data.topbar.application.TopBarQueryService;
import com.matsinger.barofishserver.domain.data.topbar.domain.TopBar;
import com.matsinger.barofishserver.domain.main.dto.Main;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.domain.Product;
import com.matsinger.barofishserver.domain.store.application.DailyStoreService;
import com.matsinger.barofishserver.domain.store.application.StoreApplicationService;
import com.matsinger.barofishserver.domain.store.application.StoreService;
import com.matsinger.barofishserver.domain.store.dto.SimpleStore;
import com.matsinger.barofishserver.domain.store.repository.StoreInfoRepository;
import com.matsinger.barofishserver.jwt.JwtService;
import com.matsinger.barofishserver.jwt.TokenAuthType;
import com.matsinger.barofishserver.jwt.TokenInfo;
import com.matsinger.barofishserver.utils.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/main")
public class MainController {

    private final TopBarQueryService topBarQueryService;
    private final BannerQueryService bannerService;
    private final CurationQueryService curationQueryService;
    private final ProductService productService;
    private final JwtService jwt;
    private final BannerQueryService bannerQueryService;
    private final StoreApplicationService storeApplicationService;

    @GetMapping("")
    public ResponseEntity<CustomResponse<Main>> selectMainItems(@RequestHeader(value = "Authorization") Optional<String> auth) {
        CustomResponse<Main> res = new CustomResponse<>();

        Main data = new Main();
        List<TopBar> topBars = topBarQueryService.selectTopBarList();
        List<Banner> banners = bannerQueryService.selectBannersOrderBySortNum();
        List<Curation> curations = curationQueryService.selectCurations();
        List<Banner> subBanners = bannerService.selectMainBanner();
        data.setSubBanner(subBanners);
        data.setBanners(banners);
        data.setTopBars(topBars);
        res.setData(Optional.of(data));

        return ResponseEntity.ok(res);
    }

    @GetMapping("/curation")
    public ResponseEntity<CustomResponse<List<CurationDto>>> selectMainCurationList(@RequestHeader(value = "Authorization", required = false) Optional<String> auth) {
        CustomResponse<List<CurationDto>> res = new CustomResponse<>();

        Integer userId = null;
        jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ALLOW), auth);

        List<Curation> curations = curationQueryService.selectCurationState(CurationState.ACTIVE);
        List<CurationDto> curationDtos = new ArrayList<>();
        for (Curation curation : curations) {
            List<Product>
                    products =
                    curationQueryService.selectCurationProducts(curation.getId(), PageRequest.of(0, 10));
            CurationDto curationDto = curation.convert2Dto();

            Integer finalUserId = userId;
            curationDto.setProducts(products.stream().map(v -> productService.convert2ListDto(v, finalUserId)).toList());
            curationDtos.add(curationDto);
        }
        res.setData(Optional.of(curationDtos));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/store")
    public ResponseEntity<CustomResponse<List<SimpleStore>>> selectMainStoreList(@RequestHeader(value = "Authorization") Optional<String> auth,
                                                                                 @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                                                 @RequestParam(value = "take", required = false, defaultValue = "10") Integer take) {
        CustomResponse<List<SimpleStore>> res = new CustomResponse<>();

        TokenInfo tokenInfo = jwt.validateAndGetTokenInfo(Set.of(TokenAuthType.ALLOW, TokenAuthType.USER), auth);
        Integer userId = tokenInfo.getId();
        
        PageRequest pageRequest = PageRequest.of(page, take);
        List<SimpleStore> response = storeApplicationService.selectRandomReliableStores(userId, pageRequest);

        res.setData(Optional.of(response));
        return ResponseEntity.ok(res);
    }
}
