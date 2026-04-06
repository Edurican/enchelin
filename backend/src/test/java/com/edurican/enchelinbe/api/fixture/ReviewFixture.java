package com.edurican.enchelinbe.api.fixture;

import com.edurican.enchelinbe.repository.RestaurantRepository;
import com.edurican.enchelinbe.service.Restaurant;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.http.*;

public record ReviewFixture(BaseFixture base, RestaurantRepository restaurantRepository) {

  public static ReviewFixture create(Environment environment, ObjectMapper objectMapper,
      RestaurantRepository restaurantRepository) {
    return new ReviewFixture(BaseFixture.create(environment, objectMapper), restaurantRepository);
  }

  // ==================== API Calls ====================

  /**
   * POST /reviews — upsert 방식. restaurantId로 DB에서 kakaoApiId를 조회하여 요청.
   */
  public ResponseEntity<String> createReview(
      String token, Long restaurantId, Integer rating, String comment) {
    Restaurant restaurant = restaurantRepository.findById(restaurantId)
        .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));
    return createReviewWithKakaoId(token, restaurant.getKakaoApiId(), restaurant.getName(), rating, comment);
  }

  public ResponseEntity<String> createReviewWithKakaoId(
      String token, String kakaoApiId, String restaurantName, Integer rating, String comment) {
    Map<String, Object> body = Map.of(
        "kakaoApiId", kakaoApiId,
        "name", restaurantName,
        "category", "음식점",
        "address", "서울시 강남구 테스트로 1",
        "placeUrl", "https://place.map.kakao.com/" + kakaoApiId,
        "x", 127.0,
        "y", 37.5,
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

  public ResponseEntity<String> getRestaurantReviewsSorted(
      Long restaurantId, int offset, int limit, String sort) {
    return base.client().getForEntity(
        "/restaurants/{restaurantId}/reviews?offset={offset}&limit={limit}&sort={sort}",
        String.class, restaurantId, offset, limit, sort);
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
