package com.edurican.enchelinbe.service;

import com.edurican.enchelinbe.common.OffsetLimit;
import com.edurican.enchelinbe.common.Page;
import com.edurican.enchelinbe.common.exception.BusinessException;
import com.edurican.enchelinbe.common.exception.ErrorCode;
import com.edurican.enchelinbe.dto.ReviewResponse;
import com.edurican.enchelinbe.repository.RestaurantRepository;
import com.edurican.enchelinbe.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final RestaurantRepository restaurantRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public void createReview(Long userId, Long restaurantId, Integer rating, String comment) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND);
        }
        reviewRepository.save(new Review(userId, restaurantId, rating, comment));
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getRestaurantReview(Long restaurantId, OffsetLimit offsetLimit) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        Slice<Review> reviewSlice = reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId, offsetLimit.toPageable());

        List<ReviewResponse> reviewResponseList = reviewSlice.getContent().stream()
                .map(review -> new ReviewResponse(
                        review.getId(),
                        "temp name",
                        restaurant.getName(),
                        review.getRating(),
                        review.getComment(),
                        review.getCreatedAt()
                ))
                .toList();

        return new Page<>(reviewResponseList, reviewSlice.hasNext());
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getUserReview(Long userId, OffsetLimit offsetLimit) {
        // 유저 추가되면 진짜 있는 유저인지 확인해야함

        Slice<Review> reviewSlice = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, offsetLimit.toPageable());
        List<Review> content = reviewSlice.getContent();

        List<Long> restaurantIds = content.stream().map(Review::getRestaurantId).toList();
        List<Restaurant> restaurants = restaurantRepository.findAllById(restaurantIds);
        Map<Long, String> restaurantNames = restaurants.stream()
                .collect(Collectors.toMap(Restaurant::getId, Restaurant::getName));

        List<ReviewResponse> reviewResponseList = content.stream()
                .map(review -> new ReviewResponse(
                        review.getId(),
                        "temp name",
                        restaurantNames.get(review.getRestaurantId()),
                        review.getRating(),
                        review.getComment(),
                        review.getCreatedAt()
                ))
                .toList();

        return new Page<>(reviewResponseList, reviewSlice.hasNext());
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, Integer rating, String comment) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        review.update(rating, comment);

        Restaurant restaurant = restaurantRepository.findById(review.getRestaurantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        return new ReviewResponse(
                review.getId(),
                "temp name",
                restaurant.getName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        review.deleted();
    }
}
