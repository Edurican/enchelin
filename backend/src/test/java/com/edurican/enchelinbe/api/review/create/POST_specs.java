package com.edurican.enchelinbe.api.review.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.AuthFixture;
import com.edurican.enchelinbe.api.fixture.RestaurantFixture;
import com.edurican.enchelinbe.api.fixture.ReviewFixture;
import com.edurican.enchelinbe.entity.Restaurant;
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
        "kakaoApiId", "kakao-unauth",
        "name", "식당",
        "category", "음식점",
        "address", "서울시 강남구",
        "placeUrl", "https://place.map.kakao.com/x",
        "x", 127.0,
        "y", 37.5,
        "rating", 4,
        "comment", "맛있어요");

    // Act
    ResponseEntity<String> response = reviewFixture.createReviewRaw(null, body);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void kakaoApiId_누락_시_실패한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Map<String, Object> body = new HashMap<>();
    body.put("name", "식당");
    body.put("category", "음식점");
    body.put("address", "서울시 강남구");
    body.put("placeUrl", "https://place.map.kakao.com/x");
    body.put("x", 127.0);
    body.put("y", 37.5);
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
    body.put("kakaoApiId", restaurant.getKakaoApiId());
    body.put("name", restaurant.getName());
    body.put("category", "음식점");
    body.put("address", "서울시 강남구");
    body.put("placeUrl", "https://place.map.kakao.com/kakao-3");
    body.put("x", 127.0);
    body.put("y", 37.5);
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
    body.put("kakaoApiId", restaurant.getKakaoApiId());
    body.put("name", restaurant.getName());
    body.put("category", "음식점");
    body.put("address", "서울시 강남구");
    body.put("placeUrl", "https://place.map.kakao.com/kakao-4");
    body.put("x", 127.0);
    body.put("y", 37.5);
    body.put("rating", 3);

    // Act
    ResponseEntity<String> response = reviewFixture.createReviewRaw(token, body);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void 존재하지_않는_kakaoApiId로_요청하면_식당이_신규_생성되고_성공한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();

    // Act — upsert이므로 없는 식당도 성공
    ResponseEntity<String> response =
        reviewFixture.createReviewWithKakaoId(token, "brand-new-kakao", "새식당", 4, "맛있어요");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
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
