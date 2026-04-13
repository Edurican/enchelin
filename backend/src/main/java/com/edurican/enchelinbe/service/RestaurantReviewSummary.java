package com.edurican.enchelinbe.service;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "restaurant_review_summary")
public class RestaurantReviewSummary {

    @Id
    @Column(name = "restaurant_id")
    private Long restaurantId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_json", nullable = false, columnDefinition = "jsonb")
    private SummaryJson summaryJson;

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @Column(name = "source_review_count", nullable = false)
    private int sourceReviewCount;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "source_review_ids", nullable = false, columnDefinition = "bigint[]")
    private Long[] sourceReviewIds;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "prompt_version", nullable = false, length = 20)
    private String promptVersion;

    @Column(name = "validation_status", nullable = false, length = 20)
    private String validationStatus;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public RestaurantReviewSummary(
            Long restaurantId,
            SummaryJson summaryJson,
            String sourceHash,
            int sourceReviewCount,
            Long[] sourceReviewIds,
            String modelVersion,
            String promptVersion,
            String validationStatus,
            OffsetDateTime generatedAt) {
        this.restaurantId = restaurantId;
        this.summaryJson = summaryJson;
        this.sourceHash = sourceHash;
        this.sourceReviewCount = sourceReviewCount;
        this.sourceReviewIds = sourceReviewIds;
        this.modelVersion = modelVersion;
        this.promptVersion = promptVersion;
        this.validationStatus = validationStatus;
        this.generatedAt = generatedAt;
        this.updatedAt = generatedAt;
    }

    public void update(
            SummaryJson summaryJson, String sourceHash, int sourceReviewCount,
            Long[] sourceReviewIds, String modelVersion, String promptVersion,
            String validationStatus, OffsetDateTime generatedAt) {
        this.summaryJson = summaryJson;
        this.sourceHash = sourceHash;
        this.sourceReviewCount = sourceReviewCount;
        this.sourceReviewIds = sourceReviewIds;
        this.modelVersion = modelVersion;
        this.promptVersion = promptVersion;
        this.validationStatus = validationStatus;
        this.generatedAt = generatedAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
