package com.edurican.enchelinbe.dto;

import java.util.List;

public record ReviewListResponse(
        List<ReviewResponse> reviews
) {
}
