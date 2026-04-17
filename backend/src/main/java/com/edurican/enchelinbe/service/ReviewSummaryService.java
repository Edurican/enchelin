package com.edurican.enchelinbe.service;

import com.edurican.enchelinbe.client.ClaudeClient;
import com.edurican.enchelinbe.client.ReviewForSummary;
import com.edurican.enchelinbe.common.exception.BusinessException;
import com.edurican.enchelinbe.common.exception.ErrorCode;
import com.edurican.enchelinbe.dto.ReviewSummaryResponse;
import com.edurican.enchelinbe.repository.RestaurantRepository;
import com.edurican.enchelinbe.repository.RestaurantReviewSummaryRepository;
import com.edurican.enchelinbe.entity.Restaurant;
import com.edurican.enchelinbe.entity.RestaurantReviewSummary;
import com.edurican.enchelinbe.entity.Review;
import com.edurican.enchelinbe.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewSummaryService {

    private final RestaurantRepository restaurantRepository;
    private final ReviewRepository reviewRepository;
    private final RestaurantReviewSummaryRepository summaryRepository;
    private final ClaudeClient claudeClient;
    private final SourceHashCalculator hashCalculator;
    private final SummaryValidator summaryValidator;

    @Lazy
    @Autowired
    private ReviewSummaryService self;

    @Value("${summary.model-version}")
    private String modelVersion;

    @Value("${summary.prompt-version}")
    private String promptVersion;

    // @Transactional 없음 — LLM 호출 중 DB 커넥션을 점유하지 않도록 의도적으로 제외.
    // 각 리포지토리 메서드는 SimpleJpaRepository의 @Transactional로 자체 트랜잭션을 가짐.
    public void generateSummaryIfStale(Long restaurantId) {
        generateSummary(restaurantId, false);
    }

    public void forceGenerateSummary(Long restaurantId) {
        generateSummary(restaurantId, true);
    }

    private void generateSummary(Long restaurantId, boolean force) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        List<Review> reviews = reviewRepository.findActiveReviewsByRestaurantId(restaurantId);
        if (reviews.size() < 3) {
            log.debug("식당 {} 리뷰 {}개 — 최소 3개 미달, 스킵", restaurantId, reviews.size());
            return;
        }

        String currentHash = hashCalculator.compute(reviews);

        if (!force && !isStale(restaurantId, currentHash)) {
            log.debug("식당 {} 요약 최신 상태 — 스킵", restaurantId);
            return;
        }

        List<ReviewForSummary> reviewDtos = reviews.stream()
                .map(r -> new ReviewForSummary(r.getId(), r.getRating(), r.getComment()))
                .toList();

        // LLM 호출 — 트랜잭션 밖에서 실행되어 커넥션 점유 없음
        SummaryJson raw = claudeClient.generate(restaurant.getName(), restaurant.getCategory(), reviewDtos);
        SummaryValidator.ValidationResult result = summaryValidator.validate(raw, reviews);

        Long[] reviewIds = reviews.stream().map(Review::getId).toArray(Long[]::new);

        // self 호출로 프록시를 통한 @Transactional 보장
        self.persistSummary(restaurantId, result, currentHash, reviews.size(), reviewIds);
        log.info("식당 {} AI 요약 저장 완료 — status={}", restaurantId, result.status());
    }

    @Transactional
    public void persistSummary(Long restaurantId, SummaryValidator.ValidationResult result,
                               String currentHash, int reviewCount, Long[] reviewIds) {
        OffsetDateTime now = OffsetDateTime.now();
        Optional<RestaurantReviewSummary> existing = summaryRepository.findById(restaurantId);
        if (existing.isPresent()) {
            existing.get().update(result.validatedSummary(), currentHash, reviewCount,
                    reviewIds, modelVersion, promptVersion, result.status(), now);
        } else {
            summaryRepository.save(new RestaurantReviewSummary(
                    restaurantId, result.validatedSummary(), currentHash, reviewCount,
                    reviewIds, modelVersion, promptVersion, result.status(), now));
        }
    }

    private boolean isStale(Long restaurantId, String currentHash) {
        return summaryRepository.findById(restaurantId)
                .map(ex -> !currentHash.equals(ex.getSourceHash())
                        || !modelVersion.equals(ex.getModelVersion())
                        || !promptVersion.equals(ex.getPromptVersion()))
                .orElse(true);
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummaryForRestaurant(Long restaurantId) {
        return summaryRepository.findById(restaurantId)
                .map(s -> switch (s.getValidationStatus()) {
                    case "passed" -> ReviewSummaryResponse.available(
                            s.getSummaryJson(), s.getGeneratedAt(), s.getSourceReviewCount());
                    default -> ReviewSummaryResponse.unavailable();
                })
                .orElseGet(ReviewSummaryResponse::insufficient);
    }
}
