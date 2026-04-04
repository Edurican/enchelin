package com.edurican.enchelinbe.controller;

import com.edurican.enchelinbe.auth.AuthService;
import com.edurican.enchelinbe.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/github")
    public ResponseEntity<Void> redirectToGitHub() {
        String githubAuthUrl = UriComponentsBuilder
                .fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "read:user user:email")
                .build()
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, githubAuthUrl)
                .build();
    }

    @GetMapping("/github/callback")
    public ResponseEntity<Void> githubCallback(@RequestParam String code) {
        String token = authService.login(code);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/auth/callback")
                .queryParam("token", token)
                .build()
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .build();
    }

    @PostMapping("/github/token")
    public ApiResponse<Map<String, String>> githubToken(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String token = authService.login(code);
        return ApiResponse.success(Map.of("token", token));
    }
}
