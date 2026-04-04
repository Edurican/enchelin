package com.edurican.enchelinbe.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 인증 경로는 스킵
        if (path.startsWith("/api/auth/")) {
            return true;
        }

        // GET 요청은 공개
        if (HttpMethod.GET.matches(method)) {
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response);
            return;
        }

        String token = header.substring(7);

        if (!jwtProvider.validateToken(token)) {
            writeUnauthorized(response);
            return;
        }

        Long userId = jwtProvider.getUserId(token);
        request.setAttribute(AuthenticatedUserArgumentResolver.USER_ID_ATTRIBUTE, userId);

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = Map.of(
                "success", false,
                "data", Map.of(),
                "error", Map.of(
                        "code", "A001",
                        "message", "인증이 필요합니다."
                )
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
