package com.edurican.enchelinbe.dto;

import com.edurican.enchelinbe.service.SummaryJson;

import java.time.OffsetDateTime;

public record ReviewSummaryResponse(
        String status,
        SummaryJson summary,
        OffsetDateTime generatedAt,
        Integer sourceReviewCount,
        String message
) {

    public static ReviewSummaryResponse available(
            SummaryJson summary, OffsetDateTime generatedAt, int sourceReviewCount) {
        return new ReviewSummaryResponse("available", summary, generatedAt, sourceReviewCount, null);
    }

    public static ReviewSummaryResponse insufficient() {
        return new ReviewSummaryResponse("insufficient", null, null, null,
                "리뷰가 부족하여 AI 요약을 제공할 수 없습니다.");
    }

    public static ReviewSummaryResponse unavailable() {
        return new ReviewSummaryResponse("unavailable", null, null, null,
                "AI 요약을 제공할 수 없습니다.");
    }
}
