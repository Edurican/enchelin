package com.edurican.enchelinbe.dto;

public record UserStatsResponse(
        int reviewCount,
        double averageRating
) {
}
