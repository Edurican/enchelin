package com.edurican.enchelinbe.dto;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        String userName,
        String restaurantName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
}
