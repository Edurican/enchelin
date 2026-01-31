package com.edurican.enchelinbe.service;

import com.edurican.enchelinbe.common.exception.BusinessException;
import com.edurican.enchelinbe.repository.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static com.edurican.enchelinbe.common.exception.ErrorCode.INVALID_INPUT;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "reviews",
        indexes = {
                @Index(name = "idx_review_user_latest", columnList = "user_id, created_at"),
                @Index(name = "idx_review_restaurant_latest", columnList = "restaurant_id, created_at")
        }
        // 만약에 같은 레스토랑에 리뷰를 또 달 수 없도록 하고 싶다면 유니크 만들기
//        uniqueConstraints = {
//                @UniqueConstraint(name = "uk_review_user_restaurant", columnNames = {"user_id", "restaurant_id"})
//        }
)
public class Review extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 100, nullable = false)
    private String comment;

    public Review(Long userId, Long restaurantId, Integer rating, String comment) {
        if (userId == null || restaurantId == null || rating == null || comment == null) {
            throw new BusinessException(INVALID_INPUT);
        }

        if(!validRating(rating)) {
            throw new BusinessException(INVALID_INPUT);
        }

        this.userId = userId;
        this.restaurantId = restaurantId;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public Integer getRating() {
        return rating;
    }

    public void update(Integer rating, String comment) {
        if (rating == null || comment == null) {
            throw new BusinessException(INVALID_INPUT);
        }

        if(!validRating(rating)) {
            throw new BusinessException(INVALID_INPUT);
        }

        this.rating = rating;
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    private boolean validRating(Integer rating) {
        return (0 <= rating && rating <= 5);
    }
}
