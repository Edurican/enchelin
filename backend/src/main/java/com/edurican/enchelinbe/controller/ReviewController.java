package com.edurican.enchelinbe.controller;

import com.edurican.enchelinbe.common.response.ApiResponse;
import com.edurican.enchelinbe.dto.CreateReviewRequest;
import com.edurican.enchelinbe.dto.ReviewListResponse;
import com.edurican.enchelinbe.dto.ReviewResponse;
import com.edurican.enchelinbe.dto.UpdateReviewRequest;
import com.edurican.enchelinbe.service.Review;
import com.edurican.enchelinbe.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/reviews")
    public ApiResponse<?> createReview(@Valid @RequestBody CreateReviewRequest request) {
        reviewService.createReview(request.userId(), request.restaurantId(), request.rating(), request.comment());
        return ApiResponse.success();
    }

    @PatchMapping("/restaurants/{restaurantId}/reviews")
    public ApiResponse<ReviewListResponse> getRestaurantReview(@PathVariable Long restaurantId) {
        ReviewListResponse response = reviewService.getRestaurantReview(restaurantId);
        return ApiResponse.success(response);
    }

    @GetMapping("/users/{userId}/reviews")
    public ApiResponse<ReviewListResponse> getUserReview(@PathVariable Long userId) {
        ReviewListResponse response = reviewService.getUserReview(userId);
        return ApiResponse.success(response);
    }

    // 유저 디테일 추가되면 변경하면 됨
    @PutMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(@PathVariable Long reviewId, @Valid @RequestBody UpdateReviewRequest request) {
        ReviewResponse response = reviewService.updateReview(reviewId, request.rating(), request.comment());
        return ApiResponse.success(response);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<?> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ApiResponse.success();
    }
}
