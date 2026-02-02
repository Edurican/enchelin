package com.edurican.enchelinbe.controller;

import com.edurican.enchelinbe.common.response.ApiResponse;
import com.edurican.enchelinbe.service.Restaurant;
import com.edurican.enchelinbe.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RestaurentController {

    private final RestaurantService restaurantService;

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
