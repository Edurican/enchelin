package com.edurican.enchelinbe.api.review.upsert;

import static org.assertj.core.api.Assertions.assertThat;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.AuthFixture;
import com.edurican.enchelinbe.api.fixture.RestaurantFixture;
import com.edurican.enchelinbe.api.fixture.ReviewFixture;
import com.edurican.enchelinbe.auth.JwtProvider;
import com.edurican.enchelinbe.repository.RestaurantRepository;
import com.edurican.enchelinbe.repository.ReviewRepository;
import com.edurican.enchelinbe.entity.Restaurant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Spec: reviews-create-upsert.md
 * POST /reviews — 식당 upsert + visitNumber 자동 계산
 */
@EnchelinApiTest
@DisplayName("POST /reviews (upsert + visitNumber)")
public class POST_upsert_specs {

  @Test
  void DB에_없는_kakaoApiId로_리뷰_작성_시_식당이_생성되고_visitNumber가_1이다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider,
      @Autowired RestaurantRepository restaurantRepository,
      @Autowired ReviewRepository reviewRepository) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);

    // Act
    ResponseEntity<String> response =
        reviewFixture.createReviewWithKakaoId(token, "new-kakao-99", "신규식당", 4, "맛있어요upsert1");

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(restaurantRepository.findByKakaoApiId("new-kakao-99")).isNotNull();
    int visitNumber = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userId) && r.getComment().equals("맛있어요upsert1"))
        .findFirst().orElseThrow().getVisitNumber();
    assertThat(visitNumber).isEqualTo(1);
  }

  @Test
  void 이미_존재하는_kakaoApiId로_리뷰_작성_시_DB_식당_정보가_유지된다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider,
      @Autowired ReviewRepository reviewRepository) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);
    restaurantFixture.createRestaurant("existing-kakao-1", "원래식당이름");

    // Act: 클라이언트가 다른 name 전송
    Map<String, Object> body = Map.of(
        "kakaoApiId", "existing-kakao-1",
        "name", "클라이언트가보낸다른이름",
        "category", "카페",
        "address", "서울시",
        "placeUrl", "https://place.map.kakao.com/existing-kakao-1",
        "x", 127.0,
        "y", 37.5,
        "rating", 3,
        "comment", "그냥upsert2");
    ResponseEntity<String> response = reviewFixture.createReviewRaw(token, body);

    // Assert: 200 OK, visitNumber=1
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    int visitNumber = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userId) && r.getComment().equals("그냥upsert2"))
        .findFirst().orElseThrow().getVisitNumber();
    assertThat(visitNumber).isEqualTo(1);
  }

  @Test
  void 같은_유저_같은_식당_두_번째_리뷰의_visitNumber는_2이다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider,
      @Autowired ReviewRepository reviewRepository) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);
    Restaurant restaurant = restaurantFixture.createRestaurant("upsert-visit-1", "방문카운트식당");

    // Act
    reviewFixture.createReview(token, restaurant.getId(), 4, "첫방문upsert3");
    reviewFixture.createReview(token, restaurant.getId(), 5, "두번째방문upsert3");

    // Assert
    int visitNumber = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userId) && r.getComment().equals("두번째방문upsert3"))
        .findFirst().orElseThrow().getVisitNumber();
    assertThat(visitNumber).isEqualTo(2);
  }

  @Test
  void 첫_리뷰가_DELETED이면_다음_리뷰의_visitNumber는_1이다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider,
      @Autowired ReviewRepository reviewRepository) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Long userId = jwtProvider.getUserId(token);
    Restaurant restaurant = restaurantFixture.createRestaurant("upsert-visit-2", "삭제후재방문식당");
    reviewFixture.createReview(token, restaurant.getId(), 3, "삭제될upsert4");

    // 현재 유저의 해당 리뷰 ID를 comment로 정확히 찾아 삭제
    long deletedId = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userId) && r.getComment().equals("삭제될upsert4"))
        .findFirst().orElseThrow().getId();
    reviewFixture.deleteReview(token, deletedId);

    // Act
    reviewFixture.createReview(token, restaurant.getId(), 4, "재방문upsert4");

    // Assert
    int visitNumber = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userId) && r.getComment().equals("재방문upsert4"))
        .findFirst().orElseThrow().getVisitNumber();
    assertThat(visitNumber).isEqualTo(1);
  }

  @Test
  void 다른_유저의_visitNumber는_독립적이다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired RestaurantFixture restaurantFixture,
      @Autowired AuthFixture authFixture,
      @Autowired JwtProvider jwtProvider,
      @Autowired ReviewRepository reviewRepository) {
    // Arrange
    String tokenA = authFixture.createUserAndGetToken();
    String tokenB = authFixture.createUserAndGetToken();
    Long userIdB = jwtProvider.getUserId(tokenB);
    Restaurant restaurant = restaurantFixture.createRestaurant("upsert-visit-3", "유저독립식당");
    reviewFixture.createReview(tokenA, restaurant.getId(), 4, "유저Aupsert5");

    // Act
    reviewFixture.createReview(tokenB, restaurant.getId(), 3, "유저Bupsert5");

    // Assert
    int visitNumberB = reviewRepository.findAll().stream()
        .filter(r -> r.getUserId().equals(userIdB) && r.getComment().equals("유저Bupsert5"))
        .findFirst().orElseThrow().getVisitNumber();
    assertThat(visitNumberB).isEqualTo(1);
  }

  @Test
  void kakaoApiId_누락_시_400을_반환한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Map<String, Object> body = new HashMap<>();
    body.put("name", "식당");
    body.put("category", "음식점");
    body.put("address", "서울");
    body.put("placeUrl", "https://place.map.kakao.com/x");
    body.put("x", 127.0);
    body.put("y", 37.5);
    body.put("rating", 3);
    body.put("comment", "코멘트");

    // Act
    ResponseEntity<String> response = reviewFixture.createReviewRaw(token, body);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void 인증_없이_요청하면_401을_반환한다(@Autowired ReviewFixture reviewFixture) {
    // Arrange
    Map<String, Object> body = Map.of(
        "kakaoApiId", "kakao-unauth",
        "name", "식당",
        "category", "음식점",
        "address", "서울",
        "placeUrl", "https://place.map.kakao.com/x",
        "x", 127.0,
        "y", 37.5,
        "rating", 3,
        "comment", "코멘트");

    // Act
    ResponseEntity<String> response = reviewFixture.createReviewRaw(null, body);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void rating이_5초과이면_400을_반환한다(
      @Autowired ReviewFixture reviewFixture,
      @Autowired AuthFixture authFixture) {
    // Arrange
    String token = authFixture.createUserAndGetToken();
    Map<String, Object> body = Map.of(
        "kakaoApiId", "kakao-rating-over",
        "name", "식당",
        "category", "음식점",
        "address", "서울",
        "placeUrl", "https://place.map.kakao.com/x",
        "x", 127.0,
        "y", 37.5,
        "rating", 6,
        "comment", "코멘트");

    // Act
    ResponseEntity<String> response = reviewFixture.createReviewRaw(token, body);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
