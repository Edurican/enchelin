package com.edurican.enchelinbe.dto;

import jakarta.validation.constraints.*;

public record CreateReviewRequest(

        @NotBlank(message = "kakaoApiId는 필수값입니다.")
        String kakaoApiId,

        @NotBlank(message = "name은 필수값입니다.")
        String name,

        @NotBlank(message = "category는 필수값입니다.")
        String category,

        @NotBlank(message = "address는 필수값입니다.")
        String address,

        @NotBlank(message = "placeUrl은 필수값입니다.")
        String placeUrl,

        @NotNull(message = "x(경도)는 필수값입니다.")
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double x,

        @NotNull(message = "y(위도)는 필수값입니다.")
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double y,

        @NotNull(message = "레스토랑에 대한 평점은 필수값입니다.")
        @Min(0)
        @Max(5)
        Integer rating,

        @NotBlank(message = "레스토랑에 대한 코멘트는 필수값입니다.")
        @Size(max = 100)
        String comment
) {
}
