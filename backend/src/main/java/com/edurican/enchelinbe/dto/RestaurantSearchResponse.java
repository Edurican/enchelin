package com.edurican.enchelinbe.dto;

public record RestaurantSearchResponse(
        String kakaoApiId,
        String name,
        String category,
        String address,
        Double x,
        Double y,
        String placeUrl,
        Double distance
) {
}
