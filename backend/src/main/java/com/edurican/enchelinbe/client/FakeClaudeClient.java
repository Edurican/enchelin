package com.edurican.enchelinbe.client;

import com.edurican.enchelinbe.service.SummaryJson;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("test")
public class FakeClaudeClient implements ClaudeClient {

    @Override
    public SummaryJson generate(String restaurantName, String category, List<ReviewForSummary> reviews) {
        // 리뷰 코멘트 첫 단어를 대표 메뉴 태그로 사용 (substring 검증 통과)
        String menuName = reviews.stream()
                .map(ReviewForSummary::comment)
                .filter(c -> c != null && !c.isBlank())
                .map(c -> c.split("\\s+")[0])
                .findFirst()
                .orElse("대표메뉴");

        List<Long> menuEvidence = reviews.stream()
                .filter(r -> r.comment() != null && r.comment().contains(menuName))
                .map(ReviewForSummary::reviewId)
                .limit(2)
                .toList();

        List<Long> allIds = reviews.stream()
                .map(ReviewForSummary::reviewId)
                .limit(3)
                .toList();

        List<Long> highRatedIds = reviews.stream()
                .filter(r -> r.rating() >= 4)
                .map(ReviewForSummary::reviewId)
                .limit(2)
                .toList();

        return new SummaryJson(
                List.of(new SummaryJson.TagItem("편안한 분위기", allIds)),   // atmosphere
                null,                                                          // parking
                null,                                                          // visitPurpose
                highRatedIds.isEmpty() ? null :
                        List.of(new SummaryJson.TagItem("맛이 좋음", highRatedIds)), // taste
                menuEvidence.isEmpty() ? null :
                        List.of(new SummaryJson.TagItem(menuName, menuEvidence)),    // signatureMenu
                null,                                                          // companion
                List.of(new SummaryJson.TagItem("친절한 직원", allIds)),     // service
                null,                                                          // costPerformance
                null                                                           // portion
        );
    }
}
