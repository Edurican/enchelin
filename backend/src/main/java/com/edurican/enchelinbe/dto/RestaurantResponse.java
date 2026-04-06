package com.edurican.enchelinbe.dto;

public record RestaurantResponse(
        Long id,
        String kakaoApiId,
        String name,
        String category,
        String address,
        Double x,
        Double y,
        String placeUrl,
        Long reviewCount,
        Double avgRating
) {
}
