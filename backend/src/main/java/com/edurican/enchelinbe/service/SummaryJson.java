package com.edurican.enchelinbe.service;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SummaryJson(
        List<TagItem> atmosphere,
        List<TagItem> parking,
        @JsonProperty("visit_purpose") List<TagItem> visitPurpose,
        List<TagItem> taste,
        @JsonProperty("signature_menu") List<TagItem> signatureMenu,
        List<TagItem> companion,
        List<TagItem> service,
        @JsonProperty("cost_performance") List<TagItem> costPerformance,
        List<TagItem> portion
) {

    public record TagItem(
            String tag,
            @JsonProperty("evidence_review_ids") List<Long> evidenceReviewIds
    ) {}
}
