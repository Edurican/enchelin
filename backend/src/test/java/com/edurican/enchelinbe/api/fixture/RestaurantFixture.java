package com.edurican.enchelinbe.api.fixture;

import com.edurican.enchelinbe.repository.RestaurantRepository;
import com.edurican.enchelinbe.entity.Restaurant;

public record RestaurantFixture(RestaurantRepository restaurantRepository) {

  // ==================== Data Setup ====================

  public Restaurant createRestaurant(String kakaoApiId, String name) {
    return createRestaurantAt(kakaoApiId, name, 127.0, 37.5);
  }

  public Restaurant createRestaurantAt(String kakaoApiId, String name, double x, double y) {
    return restaurantRepository.save(
        Restaurant.builder()
            .kakaoApiId(kakaoApiId)
            .name(name)
            .category("음식점")
            .placeUrl("https://place.map.kakao.com/" + kakaoApiId)
            .address("서울시 강남구")
            .x(x)
            .y(y)
            .build());
  }

  public Restaurant createRestaurant() {
    return createRestaurant("kakao-1", "테스트식당");
  }
}
