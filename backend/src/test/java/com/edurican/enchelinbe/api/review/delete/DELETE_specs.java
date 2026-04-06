package com.edurican.enchelinbe.api.review.delete;

import static org.assertj.core.api.Assertions.assertThat;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.AuthFixture;
import com.edurican.enchelinbe.api.fixture.RestaurantFixture;
import com.edurican.enchelinbe.api.fixture.ReviewFixture;
import com.edurican.enchelinbe.auth.JwtProvider;
import com.edurican.enchelinbe.repository.ReviewRepository;
import com.edurican.enchelinbe.service.Restaurant;
import com.edurican.enchelinbe.service.Review;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@EnchelinApiTest
@DisplayName("DELETE /reviews/{reviewId}")
public class DELETE_specs {

  @Test
  void 리뷰를_삭제하면_성공한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired ReviewRepository reviewRepository,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-40", "삭제맛집");
    reviewFixture.createReview(token, restaurant.getId(), 4, "삭제될리뷰");
    Review review = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userId) && r.getComment().equals("삭제될리뷰"))
        .findFirst().orElseThrow();

    // Act
    ResponseEntity<String> response = reviewFixture.deleteReview(token, review.getId());

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"success\":true");
  }

  @Test
  void 존재하지_않는_리뷰를_삭제하면_실패한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();

    // Act
    ResponseEntity<String> response = reviewFixture.deleteReview(token, 99999L);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void 삭제된_리뷰는_조회_목록에_포함되지_않는다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired ReviewRepository reviewRepository,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-41", "삭제확인식당");
    reviewFixture.createReview(token, restaurant.getId(), 5, "남을리뷰");
    reviewFixture.createReview(token, restaurant.getId(), 1, "삭제될리뷰");
    Review toDelete = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userId) && r.getComment().equals("삭제될리뷰"))
        .findFirst().orElseThrow();
    reviewFixture.deleteReview(token, toDelete.getId());

    // Act
    ResponseEntity<String> response =
        reviewFixture.getRestaurantReviews(restaurant.getId(), 0, 10);

    // Assert
    assertThat(response.getBody()).contains("남을리뷰");
    assertThat(response.getBody()).doesNotContain("삭제될리뷰");
  }

  @Test
  void 인증_없이_삭제하면_401을_반환한다(
      @Autowired ReviewFixture reviewFixture) {
    // Arrange
    // Act
    ResponseEntity<String> response = reviewFixture.deleteReview(null, 1L);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
