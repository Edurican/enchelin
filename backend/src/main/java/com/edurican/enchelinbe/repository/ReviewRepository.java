package com.edurican.enchelinbe.repository;

import com.edurican.enchelinbe.enums.EntityStatus;
import com.edurican.enchelinbe.service.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Slice<Review> findByRestaurantIdAndStatusOrderByCreatedAtDesc(Long restaurantId, EntityStatus status, Pageable pageable);

    Slice<Review> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, EntityStatus status, Pageable pageable);
}
