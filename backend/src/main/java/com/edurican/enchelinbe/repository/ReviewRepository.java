package com.edurican.enchelinbe.repository;

import com.edurican.enchelinbe.service.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRestaurantId(Long restaurantId);

    List<Review> findByUserId(Long userId);
}
