package com.edurican.enchelinbe.repository;

import com.edurican.enchelinbe.enums.EntityStatus;
import com.edurican.enchelinbe.service.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByIdAndStatus(Long id, EntityStatus status);

    Slice<Review> findByRestaurantIdAndStatusOrderByCreatedAtDesc(Long restaurantId, EntityStatus status, Pageable pageable);

    Slice<Review> findByRestaurantIdAndStatusOrderByRatingDescCreatedAtDesc(Long restaurantId, EntityStatus status, Pageable pageable);

    Slice<Review> findByRestaurantIdAndStatusOrderByRatingAscCreatedAtDesc(Long restaurantId, EntityStatus status, Pageable pageable);

    Slice<Review> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, EntityStatus status, Pageable pageable);

    Slice<Review> findByUserIdAndStatusOrderByRatingDescCreatedAtDesc(Long userId, EntityStatus status, Pageable pageable);

    @Query("""
            SELECT r FROM Review r
            JOIN Restaurant rest ON r.restaurantId = rest.id
            WHERE r.userId = :userId
              AND r.status = com.edurican.enchelinbe.enums.EntityStatus.ACTIVE
            ORDER BY rest.name ASC, r.createdAt DESC
            """)
    Slice<Review> findByUserIdActiveOrderByRestaurantName(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT COUNT(r) AS reviewCount, COALESCE(AVG(r.rating), 0) AS averageRating
            FROM Review r
            WHERE r.userId = :userId
              AND r.status = com.edurican.enchelinbe.enums.EntityStatus.ACTIVE
            """)
    UserStatsProjection findUserStats(@Param("userId") Long userId);

    interface UserStatsProjection {
        Long getReviewCount();
        Double getAverageRating();
    }

    @Query("""
            SELECT COALESCE(MAX(r.visitNumber), 0)
            FROM Review r
            WHERE r.userId = :userId
              AND r.restaurantId = :restaurantId
              AND r.status = com.edurican.enchelinbe.enums.EntityStatus.ACTIVE
            """)
    Integer findMaxActiveVisitNumber(@Param("userId") Long userId, @Param("restaurantId") Long restaurantId);

    @Query("""
            SELECT r.restaurantId AS restaurantId, COUNT(r) AS cnt, AVG(r.rating) AS avg
            FROM Review r
            WHERE r.restaurantId IN :restaurantIds
              AND r.status = com.edurican.enchelinbe.enums.EntityStatus.ACTIVE
            GROUP BY r.restaurantId
            """)
    List<RestaurantReviewStats> findStatsByRestaurantIds(@Param("restaurantIds") List<Long> restaurantIds);

    @Query("""
            SELECT r FROM Review r
            WHERE r.restaurantId = :restaurantId
              AND r.status = com.edurican.enchelinbe.enums.EntityStatus.ACTIVE
              AND LENGTH(r.comment) >= 5
            ORDER BY r.createdAt DESC
            LIMIT 50
            """)
    List<Review> findActiveReviewsByRestaurantId(@Param("restaurantId") Long restaurantId);

    interface RestaurantReviewStats {
        Long getRestaurantId();
        Long getCnt();
        Double getAvg();
    }
}
