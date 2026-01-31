package com.edurican.enchelinbe.dto;

import jakarta.validation.constraints.*;

public record CreateReviewRequest(

        @NotNull(message = "유저의 아이디는 필수값입니다.")
        Long userId,

        @NotNull(message = "레스토랑의 아이디는 필수값입니다.")
        Long restaurantId,

        @NotNull(message = "레스토랑에 대한 평점은 필수값입니다.")
        @Min(0)
        @Max(5)
        Integer rating,

        @NotBlank(message = "레스토랑에 대한 코멘트는 필수값입니다.")
        String comment
) {

}
