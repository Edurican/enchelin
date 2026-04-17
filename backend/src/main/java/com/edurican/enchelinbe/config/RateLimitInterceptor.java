package com.edurican.enchelinbe.config;

import com.edurican.enchelinbe.common.exception.BusinessException;
import com.edurican.enchelinbe.common.exception.ErrorCode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

/**
 * 간이 슬라이딩 윈도우 rate limiter.
 * GET /restaurants/search 에 한해 IP당 초당 5회 제한.
 * Caffeine 캐시를 사용해 비활성 IP 엔트리를 자동 만료한다 (메모리 누수 방지).
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_SECOND = 5;
    /* @VisibleForTesting */ final Cache<String, Deque<Long>> requestTimestamps = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    /** 테스트에서 카운터 초기화용 */
    public void clearAll() {
        requestTimestamps.invalidateAll();
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String ip = resolveClientIp(request);
        long now = Instant.now().toEpochMilli();
        long windowStart = now - 1000;

        requestTimestamps.asMap().compute(ip, (key, deque) -> {
            if (deque == null) {
                deque = new ArrayDeque<>();
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
        // X-Forwarded-For 파싱은 server.forward-headers-strategy=native 설정으로
        // Spring이 프레임워크 레벨에서 처리한다. request.getRemoteAddr()은
        // 이미 프록시를 통해 전달된 실제 클라이언트 IP를 반환한다.
        return request.getRemoteAddr();
    }
}
