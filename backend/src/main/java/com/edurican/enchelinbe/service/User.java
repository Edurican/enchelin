package com.edurican.enchelinbe.service;

import com.edurican.enchelinbe.enums.RoleEnum;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String githubId;

    // 깃허브 이메일 비공개 처리 유저를 위한 필드
    // 추후 비즈니스 로직에서 null 처리
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nickname;

    // 깃허브 프로필 사진
    private String avatarUrl;

    // 깃허브 링크
    private String htmlUrl;

    @Enumerated(EnumType.STRING)
    private RoleEnum role; // GOURMAND, NORMAL

    @Builder
    public User(String githubId, String email, String nickname, String avatarUrl, String htmlUrl, RoleEnum role) {
        this.githubId = githubId;
        this.email = email;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.htmlUrl = htmlUrl;
        this.role = role;
    }

    // 깃허브 정보 업데이트용
    public void updateProfile(String nickname, String avatarUrl) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }
}
