package com.edurican.enchelinbe.controller;

import com.edurican.enchelinbe.auth.AuthenticatedUser;
import com.edurican.enchelinbe.common.OffsetLimit;
import com.edurican.enchelinbe.common.Page;
import com.edurican.enchelinbe.common.response.ApiResponse;
import com.edurican.enchelinbe.dto.CreateReviewRequest;
import com.edurican.enchelinbe.dto.ReviewResponse;
import com.edurican.enchelinbe.dto.UpdateReviewRequest;
import com.edurican.enchelinbe.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/reviews")
    public ApiResponse<?> createReview(
            @AuthenticatedUser Long userId,
            @Valid @RequestBody CreateReviewRequest request) {
        reviewService.createReview(userId, request);
        return ApiResponse.success();
    }

    @GetMapping("/restaurants/{restaurantId}/reviews")
    public ApiResponse<Page<ReviewResponse>> getRestaurantReview(
            @PathVariable Long restaurantId,
            @ModelAttribute OffsetLimit offsetLimit,
            @RequestParam(value = "sort", defaultValue = "latest") String sort
    ) {
        Page<ReviewResponse> response = reviewService.getRestaurantReview(restaurantId, offsetLimit, sort);
        return ApiResponse.success(response);
    }

    @GetMapping("/users/{userId}/reviews")
    public ApiResponse<Page<ReviewResponse>> getUserReview(
            @PathVariable Long userId,
            @ModelAttribute OffsetLimit offsetLimit
    ) {
        Page<ReviewResponse> response = reviewService.getUserReview(userId, offsetLimit);
        return ApiResponse.success(response);
    }

    @PutMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(
            @AuthenticatedUser Long userId,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {
        ReviewResponse response = reviewService.updateReview(userId, reviewId, request.rating(), request.comment());
        return ApiResponse.success(response);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ApiResponse<?> deleteReview(
            @AuthenticatedUser Long userId,
            @PathVariable Long reviewId) {
        reviewService.deleteReview(userId, reviewId);
        return ApiResponse.success();
    }
}
