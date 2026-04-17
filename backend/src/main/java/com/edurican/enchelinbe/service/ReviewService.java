package com.edurican.enchelinbe.service;

import com.edurican.enchelinbe.common.OffsetLimit;
import com.edurican.enchelinbe.common.Page;
import com.edurican.enchelinbe.common.exception.BusinessException;
import com.edurican.enchelinbe.common.exception.ErrorCode;
import com.edurican.enchelinbe.dto.CreateReviewRequest;
import com.edurican.enchelinbe.dto.ReviewResponse;
import com.edurican.enchelinbe.dto.UserStatsResponse;
import com.edurican.enchelinbe.enums.EntityStatus;
import com.edurican.enchelinbe.repository.RestaurantRepository;
import com.edurican.enchelinbe.repository.ReviewRepository;
import com.edurican.enchelinbe.entity.Restaurant;
import com.edurican.enchelinbe.entity.Review;
import com.edurican.enchelinbe.entity.User;
import com.edurican.enchelinbe.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Transactional
    public void createReview(Long userId, CreateReviewRequest request) {
        // 식당 upsert: kakaoApiId 기준으로 DB에 없으면 스냅샷 저장
        Restaurant restaurant = restaurantRepository.findOptionalByKakaoApiId(request.kakaoApiId())
                .orElseGet(() -> restaurantRepository.save(
                        Restaurant.builder()
                                .kakaoApiId(request.kakaoApiId())
                                .name(request.name())
                                .category(request.category())
                                .address(request.address())
                                .placeUrl(request.placeUrl())
                                .x(request.x())
                                .y(request.y())
                                .build()
                ));

        // visitNumber: 해당 (user, restaurant) 쌍의 현재 최대 ACTIVE visitNumber + 1
        int nextVisitNumber = reviewRepository.findMaxActiveVisitNumber(userId, restaurant.getId()) + 1;

        reviewRepository.save(new Review(userId, restaurant.getId(), request.rating(), request.comment(), nextVisitNumber));
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getRestaurantReview(Long restaurantId, OffsetLimit offsetLimit, String sort) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        Slice<Review> reviewSlice = switch (sort) {
            case "latest" -> reviewRepository.findByRestaurantIdAndStatusOrderByCreatedAtDesc(
                    restaurantId, EntityStatus.ACTIVE, offsetLimit.toPageable());
            case "rating_desc" -> reviewRepository.findByRestaurantIdAndStatusOrderByRatingDescCreatedAtDesc(
                    restaurantId, EntityStatus.ACTIVE, offsetLimit.toPageable());
            case "rating_asc" -> reviewRepository.findByRestaurantIdAndStatusOrderByRatingAscCreatedAtDesc(
                    restaurantId, EntityStatus.ACTIVE, offsetLimit.toPageable());
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        };

        List<Review> content = reviewSlice.getContent();
        List<Long> userIds = content.stream().map(Review::getUserId).distinct().toList();
        Map<Long, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        List<ReviewResponse> reviewResponseList = content.stream()
                .map(review -> toReviewResponse(review, restaurant.getName(), userNames))
                .toList();

        return new Page<>(reviewResponseList, reviewSlice.hasNext());
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getUserReview(Long userId, OffsetLimit offsetLimit, String sort) {
        Slice<Review> reviewSlice = switch (sort) {
            case "latest" -> reviewRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                    userId, EntityStatus.ACTIVE, offsetLimit.toPageable());
            case "rating" -> reviewRepository.findByUserIdAndStatusOrderByRatingDescCreatedAtDesc(
                    userId, EntityStatus.ACTIVE, offsetLimit.toPageable());
            case "name" -> reviewRepository.findByUserIdActiveOrderByRestaurantName(
                    userId, offsetLimit.toPageable());
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
        };
        List<Review> content = reviewSlice.getContent();

        List<Long> restaurantIds = content.stream().map(Review::getRestaurantId).toList();
        List<Restaurant> restaurants = restaurantRepository.findAllById(restaurantIds);
        Map<Long, String> restaurantNames = restaurants.stream()
                .collect(Collectors.toMap(Restaurant::getId, Restaurant::getName));

        List<Long> userIds = content.stream().map(Review::getUserId).distinct().toList();
        Map<Long, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        List<ReviewResponse> reviewResponseList = content.stream()
                .map(review -> toReviewResponse(review, restaurantNames.get(review.getRestaurantId()), userNames))
                .toList();

        return new Page<>(reviewResponseList, reviewSlice.hasNext());
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        ReviewRepository.UserStatsProjection stats = reviewRepository.findUserStats(userId);
        return new UserStatsResponse(
                stats.getReviewCount().intValue(),
                stats.getAverageRating()
        );
    }

    @Transactional
    public ReviewResponse updateReview(Long requestingUserId, Long reviewId, Integer rating, String comment) {
        Review review = reviewRepository.findByIdAndStatus(reviewId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUserId().equals(requestingUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.update(rating, comment);

        Restaurant restaurant = restaurantRepository.findById(review.getRestaurantId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        String userName = userRepository.findById(review.getUserId())
                .map(User::getNickname)
                .orElse(null);

        return toReviewResponse(review, restaurant.getName(), Map.of(review.getUserId(), userName != null ? userName : ""));
    }

    @Transactional
    public void deleteReview(Long requestingUserId, Long reviewId) {
        Review review = reviewRepository.findByIdAndStatus(reviewId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUserId().equals(requestingUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.deleted();
    }

    private ReviewResponse toReviewResponse(Review review, String restaurantName, Map<Long, String> userNames) {
        return new ReviewResponse(
                review.getId(),
                review.getUserId(),
                userNames.getOrDefault(review.getUserId(), null),
                review.getRestaurantId(),
                restaurantName,
                review.getRating(),
                review.getComment(),
                review.getVisitNumber(),
                review.getCreatedAt()
        );
    }
}
