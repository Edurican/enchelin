package com.edurican.enchelinbe.api.fixture;

import com.edurican.enchelinbe.repository.RestaurantReviewSummaryRepository;
import com.edurican.enchelinbe.service.ReviewSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.core.env.Environment;
import com.fasterxml.jackson.databind.ObjectMapper;

public record ReviewSummaryFixture(BaseFixture base, ReviewSummaryService reviewSummaryService) {

    public static ReviewSummaryFixture create(
            Environment environment, ObjectMapper objectMapper,
            ReviewSummaryService reviewSummaryService) {
        return new ReviewSummaryFixture(
                BaseFixture.create(environment, objectMapper), reviewSummaryService);
    }

    public void generateSummary(Long restaurantId) {
        reviewSummaryService.generateSummaryIfStale(restaurantId);
    }

    public ResponseEntity<String> getReviewSummary(Long restaurantId) {
        return base.client().getForEntity(
                "/restaurants/{id}/review-summary", String.class, restaurantId);
    }
}
