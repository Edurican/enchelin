package com.edurican.enchelinbe.repository;

import com.edurican.enchelinbe.service.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Restaurant findByKakaoApiId(String kakaoMapId);
}
