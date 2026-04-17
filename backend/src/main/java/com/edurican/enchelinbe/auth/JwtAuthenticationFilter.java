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
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 인증 없이 접근 가능한 경로 목록.
     * 첫 번째 원소: HTTP 메서드 ("*" = 전체), 두 번째 원소: Ant 패턴
     */
    private static final List<String[]> PUBLIC_PATHS = List.of(
            new String[]{"*",    "/api/auth/**"},
            new String[]{HttpMethod.GET.name(), "/restaurants/**"},
            new String[]{HttpMethod.GET.name(), "/restaurant/**"},
            new String[]{HttpMethod.GET.name(), "/users/**"}
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        return PUBLIC_PATHS.stream().anyMatch(entry ->
                (entry[0].equals("*") || entry[0].equals(method)) &&
                PATH_MATCHER.match(entry[1], path)
        );
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
