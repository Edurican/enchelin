package com.edurican.enchelinbe.api.restaurant.bounds;

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

/**
 * Spec: restaurants-bounds.md
 * GET /restaurants?swLng=&swLat=&neLng=&neLat=
 * 지도 viewport(bounds) 안에서 리뷰가 1개 이상 존재하는 식당만 반환.
 */
@EnchelinApiTest
@DisplayName("GET /restaurants (bounds)")
public class GET_specs {

  private static final String BOUNDS = "/restaurants?swLng=126.9&swLat=37.4&neLng=127.1&neLat=37.6";

  @Test
  void 리뷰_있는_식당만_반환한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant withReview = restaurantFixture.createRestaurant("bounds-1", "리뷰있는식당");
    restaurantFixture.createRestaurant("bounds-2", "리뷰없는식당");
    reviewFixture.createReview(token, withReview.getId(), 4, "맛있어요");

    // Act
    ResponseEntity<String> response =
        reviewFixture.base().client().getForEntity(BOUNDS, String.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("리뷰있는식당");
    assertThat(response.getBody()).doesNotContain("리뷰없는식당");
  }

  @Test
  void bounds_밖_식당은_결과에_포함되지_않는다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange: bounds[126.9~127.1] 밖인 x=130.0에 식당 생성 후 리뷰 추가
    String token = authFixture.createUserAndGetToken();
    Restaurant outside = restaurantFixture.createRestaurantAt("bounds-out-1", "bounds밖식당130", 130.0, 37.5);
    reviewFixture.createReview(token, outside.getId(), 3, "먼곳리뷰");

    // Act: bounds[126.9~127.1] 범위 조회
    ResponseEntity<String> response =
        reviewFixture.base().client().getForEntity(BOUNDS, String.class);

    // Assert: x=130.0 식당은 bounds 밖이므로 응답에 포함되지 않아야 함
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).doesNotContain("bounds밖식당130");
  }

  @Test
  void reviewCount와_avgRating이_정확히_계산된다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("bounds-3", "평점계산식당");
    reviewFixture.createReview(token, restaurant.getId(), 4, "첫번째");
    reviewFixture.createReview(token, restaurant.getId(), 2, "두번째");

    // Act
    ResponseEntity<String> response =
        reviewFixture.base().client().getForEntity(BOUNDS, String.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"reviewCount\":2");
    assertThat(response.getBody()).contains("\"avgRating\":3.0");
  }

  @Test
  void 삭제된_리뷰만_있는_식당은_응답에서_제외된다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("bounds-4", "삭제리뷰식당");
    reviewFixture.createReview(token, restaurant.getId(), 5, "삭제될리뷰");

    // 리뷰 ID 조회 후 삭제
    ResponseEntity<String> listResponse =
        reviewFixture.getRestaurantReviews(restaurant.getId(), 0, 10);
    long reviewId = extractFirstReviewId(listResponse.getBody());
    reviewFixture.deleteReview(token, reviewId);

    // Act
    ResponseEntity<String> response =
        reviewFixture.base().client().getForEntity(BOUNDS, String.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).doesNotContain("삭제리뷰식당");
  }

  private long extractFirstReviewId(String json) {
    java.util.regex.Matcher m =
        java.util.regex.Pattern.compile("\"reviewId\":(\\d+)").matcher(json);
    if (m.find()) {
      return Long.parseLong(m.group(1));
    }
    throw new IllegalStateException("reviewId not found in: " + json);
  }
}
