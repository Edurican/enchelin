package com.edurican.enchelinbe.service;

import com.edurican.enchelinbe.client.ClaudeClient;
import com.edurican.enchelinbe.client.ReviewForSummary;
import com.edurican.enchelinbe.common.exception.BusinessException;
import com.edurican.enchelinbe.common.exception.ErrorCode;
import com.edurican.enchelinbe.dto.ReviewSummaryResponse;
import com.edurican.enchelinbe.repository.RestaurantRepository;
import com.edurican.enchelinbe.repository.RestaurantReviewSummaryRepository;
import com.edurican.enchelinbe.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${summary.model-version}")
    private String modelVersion;

    @Value("${summary.prompt-version}")
    private String promptVersion;

    @Transactional
    public void generateSummaryIfStale(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        List<Review> reviews = reviewRepository.findActiveReviewsByRestaurantId(restaurantId);
        if (reviews.size() < 3) {
            log.debug("식당 {} 리뷰 {}개 — 최소 3개 미달, 스킵", restaurantId, reviews.size());
            return;
        }

        String currentHash = hashCalculator.compute(reviews);
        Optional<RestaurantReviewSummary> existing = summaryRepository.findById(restaurantId);

        if (existing.isPresent()) {
            RestaurantReviewSummary ex = existing.get();
            if (currentHash.equals(ex.getSourceHash())
                    && modelVersion.equals(ex.getModelVersion())
                    && promptVersion.equals(ex.getPromptVersion())) {
                log.debug("식당 {} 요약 최신 상태 — 스킵", restaurantId);
                return;
            }
        }

        List<ReviewForSummary> reviewDtos = reviews.stream()
                .map(r -> new ReviewForSummary(r.getId(), r.getRating(), r.getComment()))
                .toList();

        SummaryJson raw = claudeClient.generate(restaurant.getName(), restaurant.getCategory(), reviewDtos);
        SummaryValidator.ValidationResult result = summaryValidator.validate(raw, reviews);

        Long[] reviewIds = reviews.stream().map(Review::getId).toArray(Long[]::new);
        OffsetDateTime now = OffsetDateTime.now();

        RestaurantReviewSummary summary = new RestaurantReviewSummary(
                restaurantId, result.validatedSummary(), currentHash, reviews.size(),
                reviewIds, modelVersion, promptVersion, result.status(), now);

        summaryRepository.save(summary);
        log.info("식당 {} AI 요약 저장 완료 — status={}", restaurantId, result.status());
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummaryForRestaurant(Long restaurantId) {
        return summaryRepository.findById(restaurantId)
                .filter(s -> "passed".equals(s.getValidationStatus()))
                .map(s -> ReviewSummaryResponse.available(
                        s.getSummaryJson(), s.getGeneratedAt(), s.getSourceReviewCount()))
                .orElseGet(ReviewSummaryResponse::insufficient);
    }
}
