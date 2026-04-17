package com.edurican.enchelinbe.api.review.sort;

import static org.assertj.core.api.Assertions.assertThat;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.AuthFixture;
import com.edurican.enchelinbe.api.fixture.RestaurantFixture;
import com.edurican.enchelinbe.api.fixture.ReviewFixture;
import com.edurican.enchelinbe.entity.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Spec: reviews-sort.md
 * GET /restaurants/{id}/reviews?sort=latest|rating_desc|rating_asc
 */
@EnchelinApiTest
@DisplayName("리뷰 정렬")
public class Sort_specs {

  @Test
  void sort_rating_desc이면_높은_평점_순으로_반환된다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("sort-1", "정렬테스트식당");
    reviewFixture.createReview(token, restaurant.getId(), 5, "별다섯");
    reviewFixture.createReview(token, restaurant.getId(), 1, "별하나");
    reviewFixture.createReview(token, restaurant.getId(), 3, "별셋");

    // Act
    ResponseEntity<String> response =
        reviewFixture.getRestaurantReviewsSorted(restaurant.getId(), 0, 10, "rating_desc");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    String body = response.getBody();
    // 별다섯(5)이 별셋(3)보다 앞에 있어야 함
    assertThat(body.indexOf("별다섯")).isLessThan(body.indexOf("별셋"));
    assertThat(body.indexOf("별셋")).isLessThan(body.indexOf("별하나"));
  }

  @Test
  void sort_rating_asc이면_낮은_평점_순으로_반환된다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("sort-2", "오름차순식당");
    reviewFixture.createReview(token, restaurant.getId(), 5, "높음");
    reviewFixture.createReview(token, restaurant.getId(), 1, "낮음");
    reviewFixture.createReview(token, restaurant.getId(), 3, "중간");

    // Act
    ResponseEntity<String> response =
        reviewFixture.getRestaurantReviewsSorted(restaurant.getId(), 0, 10, "rating_asc");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    String body = response.getBody();
    assertThat(body.indexOf("낮음")).isLessThan(body.indexOf("중간"));
    assertThat(body.indexOf("중간")).isLessThan(body.indexOf("높음"));
  }

  @Test
  void sort_미지정이면_latest로_동작한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("sort-3", "기본정렬식당");
    reviewFixture.createReview(token, restaurant.getId(), 3, "먼저작성");
    reviewFixture.createReview(token, restaurant.getId(), 5, "나중작성");

    // Act — sort 파라미터 없음
    ResponseEntity<String> response =
        reviewFixture.getRestaurantReviews(restaurant.getId(), 0, 10);

    // Assert: 200 OK, 나중 작성이 먼저 나옴 (createdAt DESC)
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    String body = response.getBody();
    assertThat(body.indexOf("나중작성")).isLessThan(body.indexOf("먼저작성"));
  }

  @Test
  void 잘못된_sort_값이면_400을_반환한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture) {
    // Arrange
    Restaurant restaurant = restaurantFixture.createRestaurant("sort-4", "잘못된정렬식당");

    // Act
    ResponseEntity<String> response =
        reviewFixture.getRestaurantReviewsSorted(restaurant.getId(), 0, 10, "invalid");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
