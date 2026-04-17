package com.edurican.enchelinbe.controller;

import com.edurican.enchelinbe.common.response.ApiResponse;
import com.edurican.enchelinbe.dto.UserProfileResponse;
import com.edurican.enchelinbe.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users/{userId}")
    public ApiResponse<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        return ApiResponse.success(userService.getUserProfile(userId));
    }
}
