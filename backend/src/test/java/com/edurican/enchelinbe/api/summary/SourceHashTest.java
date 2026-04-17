package com.edurican.enchelinbe.api.summary;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.AuthFixture;
import com.edurican.enchelinbe.api.fixture.RestaurantFixture;
import com.edurican.enchelinbe.api.fixture.ReviewFixture;
import com.edurican.enchelinbe.repository.ReviewRepository;
import com.edurican.enchelinbe.entity.Review;
import com.edurican.enchelinbe.service.SourceHashCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnchelinApiTest
@DisplayName("SourceHashCalculator")
class SourceHashTest {

    @Autowired
    SourceHashCalculator hashCalculator;

    @Autowired
    ReviewRepository reviewRepository;

    @Test
    void 동일한_리뷰_목록은_같은_해시를_반환한다(
            @Autowired ReviewFixture reviewFixture,
            @Autowired RestaurantFixture restaurantFixture,
            @Autowired AuthFixture authFixture) {
        // Arrange
        String token = authFixture.createUserAndGetToken();
        var restaurant = restaurantFixture.createRestaurant("hash-same-1", "해시테스트식당1");
        reviewFixture.createReview(token, restaurant.getId(), 5, "맛있어요");
        reviewFixture.createReview(token, restaurant.getId(), 3, "보통이에요");

        List<Review> reviews = reviewRepository.findActiveReviewsByRestaurantId(restaurant.getId());

        // Act
        String hash1 = hashCalculator.compute(reviews);
        String hash2 = hashCalculator.compute(reviews);

        // Assert
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
    }

    @Test
    void 리뷰_목록이_다르면_해시가_달라진다(
            @Autowired ReviewFixture reviewFixture,
            @Autowired RestaurantFixture restaurantFixture,
            @Autowired AuthFixture authFixture) {
        // Arrange
        String token = authFixture.createUserAndGetToken();
        var r1 = restaurantFixture.createRestaurant("hash-diff-1", "해시테스트식당2");
        var r2 = restaurantFixture.createRestaurant("hash-diff-2", "해시테스트식당3");
        reviewFixture.createReview(token, r1.getId(), 5, "첫번째식당리뷰");
        reviewFixture.createReview(token, r2.getId(), 3, "두번째식당리뷰");

        List<Review> reviews1 = reviewRepository.findActiveReviewsByRestaurantId(r1.getId());
        List<Review> reviews2 = reviewRepository.findActiveReviewsByRestaurantId(r2.getId());

        // Act & Assert
        assertThat(hashCalculator.compute(reviews1))
                .isNotEqualTo(hashCalculator.compute(reviews2));
    }
}
