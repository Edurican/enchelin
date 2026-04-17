package com.edurican.enchelinbe.repository;

import com.edurican.enchelinbe.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Restaurant findByKakaoApiId(String kakaoApiId);

    Optional<Restaurant> findOptionalByKakaoApiId(String kakaoApiId);

    @Query("""
            SELECT r FROM Restaurant r
            WHERE r.x BETWEEN :swLng AND :neLng
              AND r.y BETWEEN :swLat AND :neLat
              AND EXISTS (
                  SELECT 1 FROM Review rv
                  WHERE rv.restaurantId = r.id
                    AND rv.status = com.edurican.enchelinbe.enums.EntityStatus.ACTIVE
              )
            ORDER BY r.id DESC
            """)
    List<Restaurant> findReviewedInBounds(
            @Param("swLng") Double swLng,
            @Param("swLat") Double swLat,
            @Param("neLng") Double neLng,
            @Param("neLat") Double neLat
    );
}
