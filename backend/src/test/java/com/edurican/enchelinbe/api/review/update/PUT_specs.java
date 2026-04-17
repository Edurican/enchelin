package com.edurican.enchelinbe.api.review.update;

import static org.assertj.core.api.Assertions.assertThat;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.AuthFixture;
import com.edurican.enchelinbe.api.fixture.RestaurantFixture;
import com.edurican.enchelinbe.api.fixture.ReviewFixture;
import com.edurican.enchelinbe.auth.JwtProvider;
import com.edurican.enchelinbe.repository.ReviewRepository;
import com.edurican.enchelinbe.entity.Restaurant;
import com.edurican.enchelinbe.entity.Review;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@EnchelinApiTest
@DisplayName("PUT /reviews/{reviewId}")
public class PUT_specs {

  @Test
  void 리뷰를_수정하면_변경된_데이터가_반환된다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired ReviewRepository reviewRepository,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-30", "수정맛집");
    reviewFixture.createReview(token, restaurant.getId(), 3, "보통");
    Review review = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userId) && r.getComment().equals("보통"))
        .findFirst().orElseThrow();

    // Act
    ResponseEntity<String> response =
        reviewFixture.updateReview(token, review.getId(), 5, "수정후최고");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"success\":true");
    assertThat(response.getBody()).contains("수정후최고");
    assertThat(response.getBody()).contains("5");
  }

  @Test
  void 존재하지_않는_리뷰를_수정하면_실패한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();

    // Act
    ResponseEntity<String> response = reviewFixture.updateReview(token, 99999L, 5, "없는리뷰");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void rating_누락_시_실패한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired ReviewRepository reviewRepository,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-31", "식당31");
    reviewFixture.createReview(token, restaurant.getId(), 3, "보통31");
    Review review = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userId) && r.getComment().equals("보통31"))
        .findFirst().orElseThrow();
    Map<String, Object> body = new HashMap<>();
    body.put("comment", "코멘트만");

    // Act
    ResponseEntity<String> response = reviewFixture.updateReviewRaw(token, review.getId(), body);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void comment_누락_시_실패한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired ReviewRepository reviewRepository,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-32", "식당32");
    reviewFixture.createReview(token, restaurant.getId(), 3, "보통32");
    Review review = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userId) && r.getComment().equals("보통32"))
        .findFirst().orElseThrow();
    Map<String, Object> body = new HashMap<>();
    body.put("rating", 4);

    // Act
    ResponseEntity<String> response = reviewFixture.updateReviewRaw(token, review.getId(), body);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void rating이_범위를_벗어나면_실패한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired ReviewRepository reviewRepository,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-33", "식당33");
    reviewFixture.createReview(token, restaurant.getId(), 3, "보통33");
    Review review = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userId) && r.getComment().equals("보통33"))
        .findFirst().orElseThrow();

    // Act
    ResponseEntity<String> response = reviewFixture.updateReview(token, review.getId(), 6, "범위초과");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void 인증_없이_수정하면_401을_반환한다(
      @Autowired ReviewFixture reviewFixture) {
    // Arrange
    // Act
    ResponseEntity<String> response = reviewFixture.updateReview(null, 1L, 5, "인증없음");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
