package com.edurican.enchelinbe.api.review.ownership;

import static org.assertj.core.api.Assertions.assertThat;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.AuthFixture;
import com.edurican.enchelinbe.api.fixture.RestaurantFixture;
import com.edurican.enchelinbe.api.fixture.ReviewFixture;
import com.edurican.enchelinbe.auth.JwtProvider;
import com.edurican.enchelinbe.repository.ReviewRepository;
import com.edurican.enchelinbe.entity.Restaurant;
import com.edurican.enchelinbe.entity.Review;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Spec: reviews-ownership.md
 * PUT/DELETE /reviews/{id} 소유권 검증 — 작성자만 수정/삭제 가능, 타인 시도 시 403.
 */
@EnchelinApiTest
@DisplayName("리뷰 소유권 검증")
public class Ownership_specs {

  @Test
  void 작성자가_수정하면_성공한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired ReviewRepository reviewRepository,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);
    Restaurant restaurant = restaurantFixture.createRestaurant("own-1", "소유자수정식당");
    reviewFixture.createReview(token, restaurant.getId(), 3, "원본own1");
    Review review = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userId) && r.getComment().equals("원본own1"))
        .findFirst().orElseThrow();

    // Act
    ResponseEntity<String> response =
        reviewFixture.updateReview(token, review.getId(), 5, "수정됨");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void 작성자가_삭제하면_성공한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired ReviewRepository reviewRepository,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);
    Restaurant restaurant = restaurantFixture.createRestaurant("own-2", "소유자삭제식당");
    reviewFixture.createReview(token, restaurant.getId(), 3, "삭제될own2");
    Review review = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userId) && r.getComment().equals("삭제될own2"))
        .findFirst().orElseThrow();

    // Act
    ResponseEntity<String> response = reviewFixture.deleteReview(token, review.getId());

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void 다른_사용자가_수정하면_403을_반환한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired ReviewRepository reviewRepository,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider) {
    // Arrange
    String ownerToken = authFixture.createUserAndGetToken();
    Long ownerId = jwtProvider.getUserId(ownerToken);
    String otherToken = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("own-3", "타인수정시도식당");
    reviewFixture.createReview(ownerToken, restaurant.getId(), 3, "작성자리뷰own3");
    Review review = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(ownerId) && r.getComment().equals("작성자리뷰own3"))
        .findFirst().orElseThrow();

    // Act
    ResponseEntity<String> response =
        reviewFixture.updateReview(otherToken, review.getId(), 1, "타인수정시도");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void 다른_사용자가_삭제하면_403을_반환한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired ReviewRepository reviewRepository,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider) {
    // Arrange
    String ownerToken = authFixture.createUserAndGetToken();
    Long ownerId = jwtProvider.getUserId(ownerToken);
    String otherToken = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("own-4", "타인삭제시도식당");
    reviewFixture.createReview(ownerToken, restaurant.getId(), 4, "작성자리뷰own4");
    Review review = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(ownerId) && r.getComment().equals("작성자리뷰own4"))
        .findFirst().orElseThrow();

    // Act
    ResponseEntity<String> response = reviewFixture.deleteReview(otherToken, review.getId());

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
}
