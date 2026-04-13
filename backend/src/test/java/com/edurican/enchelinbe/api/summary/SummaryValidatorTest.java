package com.edurican.enchelinbe.api.summary;

import com.edurican.enchelinbe.service.Review;
import com.edurican.enchelinbe.service.SummaryJson;
import com.edurican.enchelinbe.service.SummaryValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("SummaryValidator")
class SummaryValidatorTest {

    SummaryValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SummaryValidator();
    }

    private Review mockReview(Long id, int rating, String comment) {
        Review r = mock(Review.class);
        when(r.getId()).thenReturn(id);
        when(r.getRating()).thenReturn(rating);
        when(r.getComment()).thenReturn(comment);
        return r;
    }

    private SummaryJson onlySignatureMenu(List<SummaryJson.TagItem> items) {
        return new SummaryJson(null, null, null, null, items, null, null, null, null);
    }

    private SummaryJson onlyAtmosphere(List<SummaryJson.TagItem> items) {
        return new SummaryJson(items, null, null, null, null, null, null, null, null);
    }

    @Test
    void 모든_태그가_유효하면_passed를_반환한다() {
        // Arrange
        Review r1 = mockReview(1L, 5, "삼겹살이 맛있어요");
        Review r2 = mockReview(2L, 4, "분위기가 좋아요");

        SummaryJson raw = new SummaryJson(
                List.of(new SummaryJson.TagItem("편안한 분위기", List.of(1L, 2L))),
                null, null,
                List.of(new SummaryJson.TagItem("고소한 맛", List.of(1L))),
                List.of(new SummaryJson.TagItem("삼겹살", List.of(1L))),
                null, null, null, null
        );

        // Act
        SummaryValidator.ValidationResult result = validator.validate(raw, List.of(r1, r2));

        // Assert
        assertThat(result.status()).isEqualTo("passed");
        assertThat(result.validatedSummary().atmosphere()).hasSize(1);
        assertThat(result.validatedSummary().taste()).hasSize(1);
        assertThat(result.validatedSummary().signatureMenu()).hasSize(1);
    }

    @Test
    void 존재하지_않는_evidenceReviewId는_태그를_제거한다() {
        // Arrange
        Review r1 = mockReview(1L, 5, "맛있어요");
        SummaryJson raw = onlyAtmosphere(
                List.of(new SummaryJson.TagItem("편안한 분위기", List.of(999L))) // 없는 ID
        );

        // Act
        SummaryValidator.ValidationResult result = validator.validate(raw, List.of(r1));

        // Assert
        assertThat(result.status()).isEqualTo("failed");
        assertThat(result.validatedSummary().atmosphere()).isNull();
    }

    @Test
    void evidenceReviewIds가_비어있으면_태그를_제거한다() {
        // Arrange
        Review r1 = mockReview(1L, 5, "맛있어요");
        SummaryJson raw = onlyAtmosphere(
                List.of(new SummaryJson.TagItem("편안한 분위기", List.of()))
        );

        // Act
        SummaryValidator.ValidationResult result = validator.validate(raw, List.of(r1));

        // Assert
        assertThat(result.status()).isEqualTo("failed");
    }

    @Test
    void signatureMenu_tag가_리뷰에_없으면_제거한다() {
        // Arrange
        Review r1 = mockReview(1L, 5, "파스타가 맛있어요");
        SummaryJson raw = onlySignatureMenu(
                List.of(new SummaryJson.TagItem("삼겹살", List.of(1L))) // 리뷰에 없는 단어
        );

        // Act
        SummaryValidator.ValidationResult result = validator.validate(raw, List.of(r1));

        // Assert
        assertThat(result.status()).isEqualTo("failed");
        assertThat(result.validatedSummary().signatureMenu()).isNull();
    }

    @Test
    void tag가_null이거나_공백이면_제거한다() {
        // Arrange
        Review r1 = mockReview(1L, 5, "맛있어요");
        SummaryJson raw = onlyAtmosphere(
                List.of(new SummaryJson.TagItem("   ", List.of(1L)))
        );

        // Act
        SummaryValidator.ValidationResult result = validator.validate(raw, List.of(r1));

        // Assert
        assertThat(result.status()).isEqualTo("failed");
    }

    @Test
    void 일부_태그_제거시_degraded를_반환한다() {
        // Arrange
        Review r1 = mockReview(1L, 5, "삼겹살 맛있어요");
        Review r2 = mockReview(2L, 4, "분위기 좋아요");

        SummaryJson raw = new SummaryJson(
                List.of(new SummaryJson.TagItem("편안한 분위기", List.of(1L, 2L))),
                null, null, null,
                List.of(
                        new SummaryJson.TagItem("삼겹살", List.of(1L)),    // 통과
                        new SummaryJson.TagItem("치킨", List.of(1L))       // 리뷰에 없음 → 제거
                ),
                null, null, null, null
        );

        // Act
        SummaryValidator.ValidationResult result = validator.validate(raw, List.of(r1, r2));

        // Assert
        assertThat(result.status()).isEqualTo("degraded");
        assertThat(result.validatedSummary().atmosphere()).hasSize(1);
        assertThat(result.validatedSummary().signatureMenu()).hasSize(1);
        assertThat(result.validatedSummary().signatureMenu().get(0).tag()).isEqualTo("삼겹살");
    }

    @Test
    void signatureMenu_tag가_리뷰에_있으면_통과한다() {
        // Arrange
        Review r1 = mockReview(1L, 5, "크림파스타가 진짜 맛있어요");
        SummaryJson raw = onlySignatureMenu(
                List.of(new SummaryJson.TagItem("크림파스타", List.of(1L)))
        );

        // Act
        SummaryValidator.ValidationResult result = validator.validate(raw, List.of(r1));

        // Assert
        assertThat(result.status()).isEqualTo("passed");
        assertThat(result.validatedSummary().signatureMenu()).hasSize(1);
    }
}
