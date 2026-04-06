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

    @Column(name = "visit_number", nullable = false)
    private Integer visitNumber;

    public Review(Long userId, Long restaurantId, Integer rating, String comment, Integer visitNumber) {
        if (userId == null || restaurantId == null || rating == null || comment == null || visitNumber == null) {
            throw new BusinessException(INVALID_INPUT);
        }

        if (!validRating(rating)) {
            throw new BusinessException(INVALID_INPUT);
        }

        this.userId = userId;
        this.restaurantId = restaurantId;
        this.rating = rating;
        this.comment = comment;
        this.visitNumber = visitNumber;
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

    public Integer getVisitNumber() {
        return visitNumber;
    }

    public void update(Integer rating, String comment) {
        if (rating == null || comment == null) {
            throw new BusinessException(INVALID_INPUT);
        }

        if (!validRating(rating)) {
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
