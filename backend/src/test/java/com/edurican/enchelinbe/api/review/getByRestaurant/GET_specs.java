package com.edurican.enchelinbe.api.review.getByRestaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.AuthFixture;
import com.edurican.enchelinbe.api.fixture.RestaurantFixture;
import com.edurican.enchelinbe.api.fixture.ReviewFixture;
import com.edurican.enchelinbe.service.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@EnchelinApiTest
@DisplayName("GET /restaurants/{restaurantId}/reviews")
public class GET_specs {

  @Test
  void 레스토랑의_리뷰_목록을_조회한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-10", "맛집");
    reviewFixture.createReview(token, restaurant.getId(), 5, "최고");
    reviewFixture.createReview(token, restaurant.getId(), 3, "보통");

    // Act
    ResponseEntity<String> response =
        reviewFixture.getRestaurantReviews(restaurant.getId(), 0, 10);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"success\":true");
    assertThat(response.getBody()).contains("최고");
    assertThat(response.getBody()).contains("보통");
  }

  @Test
  void 존재하지_않는_레스토랑이면_실패한다(@Autowired ReviewFixture reviewFixture) {
    // Arrange
    // Act
    ResponseEntity<String> response = reviewFixture.getRestaurantReviews(99999L, 0, 10);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void offset과_limit으로_페이징이_동작한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-11", "페이징맛집");
    for (int i = 0; i < 5; i++) {
      reviewFixture.createReview(token, restaurant.getId(), i, "리뷰" + i);
    }

    // Act
    ResponseEntity<String> response =
        reviewFixture.getRestaurantReviews(restaurant.getId(), 0, 2);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"hasNext\":true");
  }

  @Test
  void 다른_레스토랑의_리뷰는_포함되지_않는다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurantA = restaurantFixture.createRestaurant("kakao-12", "A식당");
    Restaurant restaurantB = restaurantFixture.createRestaurant("kakao-13", "B식당");
    reviewFixture.createReview(token, restaurantA.getId(), 5, "A식당리뷰");
    reviewFixture.createReview(token, restaurantB.getId(), 3, "B식당리뷰");

    // Act
    ResponseEntity<String> response =
        reviewFixture.getRestaurantReviews(restaurantA.getId(), 0, 10);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("A식당리뷰");
    assertThat(response.getBody()).doesNotContain("B식당리뷰");
  }
}
