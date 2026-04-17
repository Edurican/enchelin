package com.edurican.enchelinbe.api.restaurant.getById;

import static org.assertj.core.api.Assertions.assertThat;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.AuthFixture;
import com.edurican.enchelinbe.api.fixture.BaseFixture;
import com.edurican.enchelinbe.api.fixture.RestaurantFixture;
import com.edurican.enchelinbe.api.fixture.ReviewFixture;
import com.edurican.enchelinbe.entity.Restaurant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Spec: restaurants-get-by-id.md
 * GET /restaurants/{id}
 */
@EnchelinApiTest
@DisplayName("GET /restaurants/{id}")
public class GET_specs {

  @Test
  void 존재하는_식당_리뷰_2개의_reviewCount와_avgRating을_반환한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture,
      @Autowired Environment environment,
      @Autowired ObjectMapper objectMapper) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Restaurant restaurant = restaurantFixture.createRestaurant("getbyid-1", "단건조회식당");
    reviewFixture.createReview(token, restaurant.getId(), 4, "첫번째");
    reviewFixture.createReview(token, restaurant.getId(), 2, "두번째");
    BaseFixture base = BaseFixture.create(environment, objectMapper);

    // Act
    ResponseEntity<String> response = base.client().getForEntity(
        "/restaurants/{id}", String.class, restaurant.getId());

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"reviewCount\":2");
    assertThat(response.getBody()).contains("\"avgRating\":3.0");
    assertThat(response.getBody()).contains("단건조회식당");
  }

  @Test
  void 존재하지_않는_id는_404를_반환한다(
      @Autowired Environment environment,
      @Autowired ObjectMapper objectMapper) {
    // Arrange
    BaseFixture base = BaseFixture.create(environment, objectMapper);

    // Act
    ResponseEntity<String> response = base.client().getForEntity(
        "/restaurants/{id}", String.class, 99999L);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void 리뷰가_없는_식당은_reviewCount_0_avgRating_0을_반환한다(
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired Environment environment,
      @Autowired ObjectMapper objectMapper) {
    // Arrange
    Restaurant restaurant = restaurantFixture.createRestaurant("getbyid-2", "리뷰없는식당");
    BaseFixture base = BaseFixture.create(environment, objectMapper);

    // Act
    ResponseEntity<String> response = base.client().getForEntity(
        "/restaurants/{id}", String.class, restaurant.getId());

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"reviewCount\":0");
    assertThat(response.getBody()).contains("\"avgRating\":0.0");
  }
}
