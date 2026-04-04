package com.edurican.enchelinbe.api.review.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.AuthFixture;
import com.edurican.enchelinbe.api.fixture.RestaurantFixture;
import com.edurican.enchelinbe.api.fixture.ReviewFixture;
import com.edurican.enchelinbe.service.Restaurant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@EnchelinApiTest
@DisplayName("POST /reviews")
public class POST_specs {

  @Test
  void 올바르게_요청하면_성공을_반환한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-create-1", "생성테스트식당");

    // Act
    ResponseEntity<String> response =
        reviewFixture.createReview(token, restaurant.getId(), 4, "맛있어요");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"success\":true");
  }

  @Test
  void 인증_없이_요청하면_401을_반환한다(@Autowired ReviewFixture reviewFixture) {
    // Arrange
    Map<String, Object> body = Map.of(
        "restaurantId", 1L,
        "rating", 4,
        "comment", "맛있어요");

    // Act
    ResponseEntity<String> response = reviewFixture.createReviewRaw(null, body);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void restaurantId_누락_시_실패한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Map<String, Object> body = new HashMap<>();
    body.put("rating", 3);
    body.put("comment", "괜찮아요");

    // Act
    ResponseEntity<String> response = reviewFixture.createReviewRaw(token, body);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void rating_누락_시_실패한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-3", "식당3");
    Map<String, Object> body = new HashMap<>();
    body.put("restaurantId", restaurant.getId());
    body.put("comment", "괜찮아요");

    // Act
    ResponseEntity<String> response = reviewFixture.createReviewRaw(token, body);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void comment_누락_시_실패한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-4", "식당4");
    Map<String, Object> body = new HashMap<>();
    body.put("restaurantId", restaurant.getId());
    body.put("rating", 3);

    // Act
    ResponseEntity<String> response = reviewFixture.createReviewRaw(token, body);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void 존재하지_않는_restaurantId로_요청하면_실패한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();

    // Act
    ResponseEntity<String> response = reviewFixture.createReview(token, 99999L, 4, "맛있어요");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void rating이_0미만이면_실패한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-5", "식당5");

    // Act
    ResponseEntity<String> response =
        reviewFixture.createReview(token, restaurant.getId(), -1, "별로예요");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void rating이_5초과이면_실패한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("kakao-6", "식당6");

    // Act
    ResponseEntity<String> response =
        reviewFixture.createReview(token, restaurant.getId(), 6, "최고예요");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
