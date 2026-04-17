package com.edurican.enchelinbe.api.review.getByUser;

import static org.assertj.core.api.Assertions.assertThat;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.AuthFixture;
import com.edurican.enchelinbe.api.fixture.RestaurantFixture;
import com.edurican.enchelinbe.api.fixture.ReviewFixture;
import com.edurican.enchelinbe.auth.JwtProvider;
import com.edurican.enchelinbe.entity.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@EnchelinApiTest
@DisplayName("GET /users/{userId}/reviews")
public class GET_specs {

  @Test
  void 유저의_리뷰_목록을_조회한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-20", "유저맛집");
    reviewFixture.createReview(token, restaurant.getId(), 5, "좋아요");
    reviewFixture.createReview(token, restaurant.getId(), 3, "보통이에요");

    // Act
    ResponseEntity<String> response = reviewFixture.getUserReviews(userId, 0, 10);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"success\":true");
    assertThat(response.getBody()).contains("좋아요");
    assertThat(response.getBody()).contains("보통이에요");
  }

  @Test
  void 페이징이_동작한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-21", "페이징식당");
    for (int i = 0; i < 5; i++) {
      reviewFixture.createReview(token, restaurant.getId(), i, "리뷰" + i);
    }

    // Act
    ResponseEntity<String> response = reviewFixture.getUserReviews(userId, 0, 2);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"hasNext\":true");
  }

  @Test
  void 다른_유저의_리뷰는_포함되지_않는다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider) {
    // Arrange
    String tokenA = authFixture.createUserAndGetToken();
    String tokenB = authFixture.createUserAndGetToken();
    Long userIdA = jwtProvider.getUserId(tokenA);
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-22", "격리식당");
    reviewFixture.createReview(tokenA, restaurant.getId(), 5, "유저A리뷰");
    reviewFixture.createReview(tokenB, restaurant.getId(), 3, "유저B리뷰");

    // Act
    ResponseEntity<String> response = reviewFixture.getUserReviews(userIdA, 0, 10);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("유저A리뷰");
    assertThat(response.getBody()).doesNotContain("유저B리뷰");
  }
}
