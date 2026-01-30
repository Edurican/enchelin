package com.edurican.enchelin.common.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ErrorResponse {
    private final LocalDateTime timestamp;
    private final int status;
    private final String code;
    private final String message;

    @Builder
    public ErrorResponse(int status, String message, String code) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.message = message;
        this.code = code;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }
}
