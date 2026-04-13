package com.edurican.enchelinbe.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SummaryValidator {

    public record ValidationResult(String status, SummaryJson validatedSummary) {}

    public ValidationResult validate(SummaryJson raw, List<Review> sourceReviews) {
        Set<Long> validIds = sourceReviews.stream()
                .map(Review::getId)
                .collect(Collectors.toSet());

        List<SummaryJson.TagItem> atmosphere    = filterTags(raw.atmosphere(), validIds, null);
        List<SummaryJson.TagItem> parking       = filterTags(raw.parking(), validIds, null);
        List<SummaryJson.TagItem> visitPurpose  = filterTags(raw.visitPurpose(), validIds, null);
        List<SummaryJson.TagItem> taste         = filterTags(raw.taste(), validIds, null);
        List<SummaryJson.TagItem> signatureMenu = filterTags(raw.signatureMenu(), validIds, sourceReviews);
        List<SummaryJson.TagItem> companion     = filterTags(raw.companion(), validIds, null);
        List<SummaryJson.TagItem> service       = filterTags(raw.service(), validIds, null);
        List<SummaryJson.TagItem> costPerf      = filterTags(raw.costPerformance(), validIds, null);
        List<SummaryJson.TagItem> portion       = filterTags(raw.portion(), validIds, null);

        int totalOriginal = count(raw.atmosphere()) + count(raw.parking()) + count(raw.visitPurpose())
                + count(raw.taste()) + count(raw.signatureMenu()) + count(raw.companion())
                + count(raw.service()) + count(raw.costPerformance()) + count(raw.portion());

        int totalValid = count(atmosphere) + count(parking) + count(visitPurpose)
                + count(taste) + count(signatureMenu) + count(companion)
                + count(service) + count(costPerf) + count(portion);

        String status;
        if (totalValid == 0) {
            status = "failed";
        } else if (totalValid < totalOriginal) {
            status = "degraded";
        } else {
            status = "passed";
        }

        return new ValidationResult(status, new SummaryJson(
                nullIfEmpty(atmosphere), nullIfEmpty(parking), nullIfEmpty(visitPurpose),
                nullIfEmpty(taste), nullIfEmpty(signatureMenu), nullIfEmpty(companion),
                nullIfEmpty(service), nullIfEmpty(costPerf), nullIfEmpty(portion)));
    }

    /**
     * @param menuReviews null이면 signatureMenu substring 검증 스킵
     */
    private List<SummaryJson.TagItem> filterTags(
            List<SummaryJson.TagItem> items, Set<Long> validIds,
            List<Review> menuReviews) {
        if (items == null) return List.of();
        return items.stream()
                .filter(t -> isTagValid(t.tag()))
                .filter(t -> isEvidenceValid(t.evidenceReviewIds(), validIds))
                .filter(t -> menuReviews == null || tagAppearsInReview(t.tag(), menuReviews))
                .toList();
    }

    private boolean isTagValid(String tag) {
        return tag != null && !tag.isBlank() && tag.length() <= 15;
    }

    private boolean isEvidenceValid(List<Long> ids, Set<Long> validIds) {
        return ids != null && !ids.isEmpty() && validIds.containsAll(ids);
    }

    private boolean tagAppearsInReview(String tag, List<Review> reviews) {
        return reviews.stream()
                .map(Review::getComment)
                .filter(c -> c != null)
                .anyMatch(c -> c.contains(tag));
    }

    private int count(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private List<SummaryJson.TagItem> nullIfEmpty(List<SummaryJson.TagItem> list) {
        return (list == null || list.isEmpty()) ? null : list;
    }
}
