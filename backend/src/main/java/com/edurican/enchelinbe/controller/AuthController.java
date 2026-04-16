package com.edurican.enchelinbe.controller;

import com.edurican.enchelinbe.auth.AuthService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    private final Cache<String, String> codeCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build();

    @GetMapping("/github")
    public ResponseEntity<Void> redirectToGitHub(HttpSession session) {
        String state = UUID.randomUUID().toString();
        session.setAttribute("oauth_state", state);

        String githubAuthUrl = UriComponentsBuilder
                .fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "read:user user:email")
                .queryParam("state", state)
                .build()
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, githubAuthUrl)
                .build();
    }

    @GetMapping("/github/callback")
    public ResponseEntity<?> githubCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpSession session) {

        String savedState = (String) session.getAttribute("oauth_state");
        session.removeAttribute("oauth_state");

        if (state == null || !state.equals(savedState)) {
            String errorUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl)
                    .path("/auth/callback")
                    .queryParam("error", "invalid_state")
                    .build()
                    .toUriString();
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, errorUrl)
                    .build();
        }

        try {
            String token = authService.login(code);
            String oneTimeCode = UUID.randomUUID().toString();
            codeCache.put(oneTimeCode, token);

            String redirectUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl)
                    .path("/auth/callback")
                    .queryParam("code", oneTimeCode)
                    .build()
                    .toUriString();

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, redirectUrl)
                    .build();
        } catch (Exception e) {
            String errorUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl)
                    .path("/auth/callback")
                    .queryParam("error", "auth_failed")
                    .build()
                    .toUriString();
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, errorUrl)
                    .build();
        }
    }

    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeCode(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String token = codeCache.getIfPresent(code);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired code");
        }
        codeCache.invalidate(code);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
