package com.edurican.enchelin.entity;

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

    @Column(name = "kakao_api_id", nullable = false)
    private String kakaoApiId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String address;

    @Builder
    public Restaurant(String kakaoApiId, String name, String category, String address) {
        this.kakaoApiId = kakaoApiId;
        this.name = name;
        this.category = category;
        this.address = address;
    }
}
