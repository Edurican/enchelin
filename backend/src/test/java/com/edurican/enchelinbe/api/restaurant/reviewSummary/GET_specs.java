package com.edurican.enchelinbe.api.restaurant.reviewSummary;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.AuthFixture;
import com.edurican.enchelinbe.api.fixture.RestaurantFixture;
import com.edurican.enchelinbe.api.fixture.ReviewFixture;
import com.edurican.enchelinbe.api.fixture.ReviewSummaryFixture;
import com.edurican.enchelinbe.service.Restaurant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GET /restaurants/{id}/review-summary
 */
@EnchelinApiTest
@DisplayName("GET /restaurants/{id}/review-summary")
class GET_specs {

    @Test
    void passed_요약이_있으면_available과_요약_내용을_반환한다(
            @Autowired ReviewFixture reviewFixture,
            @Autowired RestaurantFixture restaurantFixture,
            @Autowired AuthFixture authFixture,
            @Autowired ReviewSummaryFixture summaryFixture) {
        // Arrange
        String token = authFixture.createUserAndGetToken();
        Restaurant restaurant = restaurantFixture.createRestaurant("summary-ok-1", "요약테스트식당1");
        reviewFixture.createReview(token, restaurant.getId(), 5, "삼겹살 정말 맛있어요");
        reviewFixture.createReview(token, restaurant.getId(), 4, "서비스가 친절해요");
        reviewFixture.createReview(token, restaurant.getId(), 5, "또 오고 싶어요");
        summaryFixture.generateSummary(restaurant.getId());

        // Act
        ResponseEntity<String> response = summaryFixture.getReviewSummary(restaurant.getId());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"available\"");
        assertThat(response.getBody()).contains("\"summary\"");
        assertThat(response.getBody()).contains("\"sourceReviewCount\"");
    }

    @Test
    void 리뷰가_3개_미만이면_insufficient를_반환한다(
            @Autowired ReviewFixture reviewFixture,
            @Autowired RestaurantFixture restaurantFixture,
            @Autowired AuthFixture authFixture,
            @Autowired ReviewSummaryFixture summaryFixture) {
        // Arrange
        String token = authFixture.createUserAndGetToken();
        Restaurant restaurant = restaurantFixture.createRestaurant("summary-few-1", "요약테스트식당2");
        reviewFixture.createReview(token, restaurant.getId(), 5, "맛있어요");
        reviewFixture.createReview(token, restaurant.getId(), 4, "좋아요");
        summaryFixture.generateSummary(restaurant.getId()); // skipped — <3 reviews

        // Act
        ResponseEntity<String> response = summaryFixture.getReviewSummary(restaurant.getId());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"insufficient\"");
    }

    @Test
    void 요약이_없는_식당은_insufficient를_반환한다(
            @Autowired RestaurantFixture restaurantFixture,
            @Autowired ReviewSummaryFixture summaryFixture) {
        // Arrange
        Restaurant restaurant = restaurantFixture.createRestaurant("summary-none-1", "요약없는식당");

        // Act
        ResponseEntity<String> response = summaryFixture.getReviewSummary(restaurant.getId());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"insufficient\"");
    }

    @Test
    void 동일_해시면_재생성하지_않고_기존_요약을_반환한다(
            @Autowired ReviewFixture reviewFixture,
            @Autowired RestaurantFixture restaurantFixture,
            @Autowired AuthFixture authFixture,
            @Autowired ReviewSummaryFixture summaryFixture) {
        // Arrange
        String token = authFixture.createUserAndGetToken();
        Restaurant restaurant = restaurantFixture.createRestaurant("summary-idempotent-1", "요약테스트식당3");
        reviewFixture.createReview(token, restaurant.getId(), 5, "삼겹살 최고");
        reviewFixture.createReview(token, restaurant.getId(), 4, "직원이 친절해요");
        reviewFixture.createReview(token, restaurant.getId(), 5, "분위기 좋아요");

        // Act — 두 번 호출해도 동일 결과
        summaryFixture.generateSummary(restaurant.getId());
        ResponseEntity<String> first = summaryFixture.getReviewSummary(restaurant.getId());
        summaryFixture.generateSummary(restaurant.getId()); // should skip (same hash)
        ResponseEntity<String> second = summaryFixture.getReviewSummary(restaurant.getId());

        // Assert
        assertThat(first.getBody()).isEqualTo(second.getBody());
    }
}
