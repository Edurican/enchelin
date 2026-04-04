package com.edurican.enchelinbe.api.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.http.*;

public record ReviewFixture(BaseFixture base) {

  public static ReviewFixture create(Environment environment, ObjectMapper objectMapper) {
    return new ReviewFixture(BaseFixture.create(environment, objectMapper));
  }

  // ==================== API Calls ====================

  public ResponseEntity<String> createReview(
      String token, Long restaurantId, Integer rating, String comment) {
    Map<String, Object> body =
        Map.of(
            "restaurantId", restaurantId,
            "rating", rating,
            "comment", comment);
    return base.postRaw("/reviews", body, token);
  }

  public ResponseEntity<String> createReviewRaw(String token, Map<String, Object> body) {
    return base.postRaw("/reviews", body, token);
  }

  public ResponseEntity<String> getRestaurantReviews(
      Long restaurantId, int offset, int limit) {
    return base.client().getForEntity(
        "/restaurants/{restaurantId}/reviews?offset={offset}&limit={limit}",
        String.class, restaurantId, offset, limit);
  }

  public ResponseEntity<String> getUserReviews(Long userId, int offset, int limit) {
    return base.client().getForEntity(
        "/users/{userId}/reviews?offset={offset}&limit={limit}",
        String.class, userId, offset, limit);
  }

  public ResponseEntity<String> updateReview(
      String token, Long reviewId, Integer rating, String comment) {
    Map<String, Object> body = Map.of("rating", rating, "comment", comment);
    return base.put("/reviews/" + reviewId, body, token, String.class);
  }

  public ResponseEntity<String> updateReviewRaw(
      String token, Long reviewId, Map<String, Object> body) {
    return base.put("/reviews/" + reviewId, body, token, String.class);
  }

  public ResponseEntity<String> deleteReview(String token, Long reviewId) {
    return base.delete("/reviews/" + reviewId, token, String.class);
  }
}
