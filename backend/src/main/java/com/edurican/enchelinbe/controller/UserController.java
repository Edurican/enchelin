package com.edurican.enchelinbe.controller;

import com.edurican.enchelinbe.common.exception.BusinessException;
import com.edurican.enchelinbe.common.exception.ErrorCode;
import com.edurican.enchelinbe.common.response.ApiResponse;
import com.edurican.enchelinbe.dto.UserProfileResponse;
import com.edurican.enchelinbe.repository.UserRepository;
import com.edurican.enchelinbe.service.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/users/{userId}")
    public ApiResponse<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return ApiResponse.success(new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getHtmlUrl()
        ));
    }
}
