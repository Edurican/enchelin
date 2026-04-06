package com.edurican.enchelinbe.config;

import com.edurican.enchelinbe.common.exception.BusinessException;
import com.edurican.enchelinbe.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 간이 슬라이딩 윈도우 rate limiter.
 * GET /restaurants/search 에 한해 IP당 초당 5회 제한.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_SECOND = 5;
    final Map<String, Deque<Long>> requestTimestamps = new ConcurrentHashMap<>();

    /** 테스트에서 카운터 초기화용 */
    public void clearAll() {
        requestTimestamps.clear();
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String ip = resolveClientIp(request);
        long now = Instant.now().toEpochMilli();
        long windowStart = now - 1000;

        requestTimestamps.compute(ip, (key, deque) -> {
            if (deque == null) {
                deque = new LinkedList<>();
            }
            // 1초 밖의 항목 제거
            while (!deque.isEmpty() && deque.peekFirst() < windowStart) {
                deque.pollFirst();
            }
            if (deque.size() >= MAX_REQUESTS_PER_SECOND) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
            }
            deque.addLast(now);
            return deque;
        });

        return true;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
