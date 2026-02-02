package com.edurican.enchelinbe.service;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kakao_api_id", nullable = false, unique = true)
    private String kakaoApiId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String placeUrl;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double x;

    @Column(nullable = false)
    private Double y;

    @Builder
    public Restaurant(String kakaoApiId, String name, String category, String placeUrl, String address, Double x, Double y) {
        this.kakaoApiId = kakaoApiId;
        this.name = name;
        this.category = category;
        this.placeUrl = placeUrl;
        this.address = address;
        this.x = x;
        this.y = y;
    }

    public void updateCategory(String newCategory) {
        this.category = newCategory;
    }
}
