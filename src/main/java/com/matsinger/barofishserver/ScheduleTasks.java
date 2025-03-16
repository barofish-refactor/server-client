package com.matsinger.barofishserver;

import com.matsinger.barofishserver.domain.deliver.application.DeliverService;
import com.matsinger.barofishserver.domain.store.application.DailyStoreService;
import com.matsinger.barofishserver.order.application.service.OrderService;
import com.matsinger.barofishserver.domain.product.application.ProductService;
import com.matsinger.barofishserver.domain.product.weeksdate.application.WeeksDateCommandService;
import com.matsinger.barofishserver.domain.search.application.SearchKeywordCommandService;
import com.matsinger.barofishserver.domain.user.application.UserCommandService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;

@Component
@AllArgsConstructor
public class ScheduleTasks {

    private final SearchKeywordCommandService searchKeywordCommandService;
    private final DeliverService deliverService;
    private final OrderService orderService;
    private final UserCommandService userCommandService;
    private final ProductService productService;
    private final WeeksDateCommandService weeksDateCommandService;
    private final DailyStoreService dailyStoreService;

    @Scheduled(cron = "0 0 0 * * 1")
    public void SearchKeywordSchedule() {
        searchKeywordCommandService.resetRank();
    }

    @Scheduled(cron = "0 0 */1 * * *")
    public void refreshDeliverState() {
        deliverService.refreshOrderDeliverState();
    }

    @Scheduled(cron = "0 0 */1 * * *")
    public void autoFinalConfirmOrder() {
        orderService.automaticFinalConfirm();
    }

    @Scheduled(cron = "0 0 */1 * * *")
    public void deleteWithdrawUserData() {
        userCommandService.deleteWithdrawUserList();
    }

    @Scheduled(cron = "0 0 */1 * * *")
    public void updatePassedPromotionProductInactive() {
        productService.updatePassedPromotionProductInactive();
    }

    @Scheduled(cron = "0 0 */1 * * *")
    public void updateProductStateActiveSupposedToStartPromotion() {
        productService.updateProductStateActiveSupposedToStartPromotion();
    }

    @Scheduled(cron = "0 0 * * * 1") // 매주 일요일 정각에 실행
    public void addDateInfoInTheNextTwoWeeks() throws IOException {
        weeksDateCommandService.saveThisAndNextWeeksDate(LocalDate.now());
    }

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시에 실행
    public void refreshRandomStoreList() {
        dailyStoreService.refreshDailyStores();
    }
}
