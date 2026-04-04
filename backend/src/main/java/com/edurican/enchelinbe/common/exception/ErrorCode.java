package com.edurican.enchelinbe.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    GITHUB_AUTH_FAILED(HttpStatus.BAD_REQUEST, "A002", "GitHub 인증에 실패했습니다."),

    RESTAURANT_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "레스토랑이 존재하지 않습니다."),

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "RV001", "리뷰가 존재하지 않습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
