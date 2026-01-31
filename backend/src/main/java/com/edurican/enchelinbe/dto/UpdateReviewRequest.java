package com.edurican.enchelinbe.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateReviewRequest(
        @NotNull(message = "레스토랑에 대한 평점은 필수값입니다.")
        @Min(0)
        @Max(5)
        Integer rating,

        @NotBlank(message = "레스토랑에 대한 코멘트는 필수값입니다.")
        String comment
) {
}
