package com.edurican.enchelinbe.api.fixture;

import com.edurican.enchelinbe.auth.JwtProvider;
import com.edurican.enchelinbe.enums.RoleEnum;
import com.edurican.enchelinbe.repository.UserRepository;
import com.edurican.enchelinbe.entity.User;

import java.util.concurrent.atomic.AtomicLong;

public record AuthFixture(UserRepository userRepository, JwtProvider jwtProvider) {

    private static final AtomicLong COUNTER = new AtomicLong(System.nanoTime());

    // ==================== Convenience Methods ====================

    public String createUserAndGetToken() {
        long seq = COUNTER.incrementAndGet();
        return createUserAndGetToken("gh-" + seq, "user-" + seq, "user-" + seq + "@test.com");
    }

    public String createUserAndGetToken(String githubId, String nickname, String email) {
        User user = userRepository.save(User.builder()
                .githubId(githubId)
                .nickname(nickname)
                .email(email)
                .avatarUrl("https://avatars.githubusercontent.com/u/" + githubId)
                .htmlUrl("https://github.com/" + nickname)
                .role(RoleEnum.NORMAL)
                .build());
        return jwtProvider.createToken(user.getId());
    }

    public Long createUserAndGetId() {
        long seq = COUNTER.incrementAndGet();
        return createUserAndGetId("gh-" + seq, "user-" + seq, "user-" + seq + "@test.com");
    }

    public Long createUserAndGetId(String githubId, String nickname, String email) {
        User user = userRepository.save(User.builder()
                .githubId(githubId)
                .nickname(nickname)
                .email(email)
                .avatarUrl("https://avatars.githubusercontent.com/u/" + githubId)
                .htmlUrl("https://github.com/" + nickname)
                .role(RoleEnum.NORMAL)
                .build());
        return user.getId();
    }
}
