package com.edurican.enchelinbe.controller;

import com.edurican.enchelinbe.common.response.ApiResponse;
import com.edurican.enchelinbe.dto.RestaurantResponse;
import com.edurican.enchelinbe.dto.RestaurantSearchResponse;
import com.edurican.enchelinbe.dto.ReviewSummaryResponse;
import com.edurican.enchelinbe.service.Restaurant;
import com.edurican.enchelinbe.service.RestaurantService;
import com.edurican.enchelinbe.service.ReviewSummaryService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final ReviewSummaryService reviewSummaryService;

    // ---------------------------------------------------------------------------
    // GET /restaurants (bounds - 리뷰 있는 식당만)
    // ---------------------------------------------------------------------------
    @GetMapping("/restaurants")
    public ApiResponse<List<RestaurantResponse>> getReviewedRestaurantsInBounds(
            @RequestParam("swLng") Double swLng,
            @RequestParam("swLat") Double swLat,
            @RequestParam("neLng") Double neLng,
            @RequestParam("neLat") Double neLat
    ) {
        List<RestaurantResponse> restaurants =
                restaurantService.getReviewedRestaurantsInBounds(swLng, swLat, neLng, neLat);
        return ApiResponse.success(restaurants);
    }

    // ---------------------------------------------------------------------------
    // GET /restaurants/{id}
    // ---------------------------------------------------------------------------
    @GetMapping("/restaurants/{id}")
    public ApiResponse<RestaurantResponse> getRestaurantById(@PathVariable Long id) {
        return ApiResponse.success(restaurantService.getRestaurantById(id));
    }

    // ---------------------------------------------------------------------------
    // GET /restaurants/search (Kakao 단일 키워드 프록시 + rate limit)
    // ---------------------------------------------------------------------------
    @GetMapping("/restaurants/search")
    public ApiResponse<List<RestaurantSearchResponse>> search(
            @RequestParam @NotBlank String query,
            @RequestParam(required = false) Double x,
            @RequestParam(required = false) Double y,
            @RequestParam(defaultValue = "1") int page
    ) {
        List<RestaurantSearchResponse> results = restaurantService.searchByKeyword(query, x, y, page);
        return ApiResponse.success(results);
    }

    // ---------------------------------------------------------------------------
    // GET /restaurants/{id}/review-summary
    // ---------------------------------------------------------------------------
    @GetMapping("/restaurants/{id}/review-summary")
    public ApiResponse<ReviewSummaryResponse> getReviewSummary(@PathVariable Long id) {
        return ApiResponse.success(reviewSummaryService.getSummaryForRestaurant(id));
    }

    // ---------------------------------------------------------------------------
    // GET /restaurant/nearby (기존 엔드포인트 유지 - 구 RestaurentController 대체)
    // ---------------------------------------------------------------------------
    @GetMapping("/restaurant/nearby")
    public ApiResponse<List<Restaurant>> saveNearbyRestaurant(
            @RequestParam("x") Double x,
            @RequestParam("y") Double y,
            @RequestParam(value = "radius", defaultValue = "1000") int radius
    ) {
        List<Restaurant> restaurants = restaurantService.saveAndGetRestaurantsAround(x, y, radius);
        return ApiResponse.success(restaurants);
    }
}
