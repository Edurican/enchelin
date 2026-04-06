package com.edurican.enchelinbe.dto;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        Long userId,
        String userName,
        Long restaurantId,
        String restaurantName,
        Integer rating,
        String comment,
        Integer visitNumber,
        LocalDateTime createdAt
) {
}
