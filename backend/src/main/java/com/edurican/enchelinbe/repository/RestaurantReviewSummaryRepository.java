package com.edurican.enchelinbe.repository;

import com.edurican.enchelinbe.service.RestaurantReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantReviewSummaryRepository extends JpaRepository<RestaurantReviewSummary, Long> {

    @Query(value = """
            SELECT r.id FROM restaurants r
            LEFT JOIN restaurant_review_summary s ON r.id = s.restaurant_id
            WHERE s.restaurant_id IS NULL
               OR s.model_version != :modelVersion
               OR s.prompt_version != :promptVersion
               OR EXISTS (
                   SELECT 1 FROM reviews rv
                   WHERE rv.restaurant_id = r.id
                     AND rv.status = 'ACTIVE'
                     AND rv.updated_at > s.generated_at
               )
            ORDER BY s.generated_at ASC NULLS FIRST
            LIMIT 10
            """, nativeQuery = true)
    List<Long> findTopStaleRestaurantIds(
            @Param("modelVersion") String modelVersion,
            @Param("promptVersion") String promptVersion
    );
}
