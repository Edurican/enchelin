package com.edurican.enchelinbe.dto;

public record UserProfileResponse(
        Long id,
        String nickname,
        String avatarUrl,
        String htmlUrl
) {
}
