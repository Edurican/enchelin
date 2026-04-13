package com.edurican.enchelinbe.service;

import com.edurican.enchelinbe.repository.RestaurantReviewSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "summary.scheduler.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class ReviewSummaryScheduler {

    private final RestaurantReviewSummaryRepository summaryRepository;
    private final ReviewSummaryService reviewSummaryService;

    @Value("${summary.model-version}")
    private String modelVersion;

    @Value("${summary.prompt-version}")
    private String promptVersion;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void runDailySummaryBatch() {
        log.info("AI 리뷰 요약 배치 시작");
        List<Long> targets = summaryRepository.findTopStaleRestaurantIds(modelVersion, promptVersion);
        log.info("재생성 대상 식당 {}개", targets.size());

        for (Long restaurantId : targets) {
            try {
                reviewSummaryService.generateSummaryIfStale(restaurantId);
            } catch (Exception e) {
                log.error("식당 {} 요약 생성 실패 — 다음 건으로 계속", restaurantId, e);
            }
        }

        log.info("AI 리뷰 요약 배치 완료");
    }
}
