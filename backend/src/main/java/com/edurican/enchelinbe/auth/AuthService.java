package com.edurican.enchelinbe.auth;

import com.edurican.enchelinbe.enums.RoleEnum;
import com.edurican.enchelinbe.repository.UserRepository;
import com.edurican.enchelinbe.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GitHubClient gitHubClient;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public String login(String code) {
        String accessToken = gitHubClient.exchangeCode(code);
        GitHubUserInfo userInfo = gitHubClient.getUserInfo(accessToken);

        User user = userRepository.findByGithubId(userInfo.id())
                .map(existing -> {
                    existing.updateProfile(userInfo.login(), userInfo.avatarUrl());
                    return existing;
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .githubId(userInfo.id())
                        .email(userInfo.email() != null ? userInfo.email() : "")
                        .nickname(userInfo.login())
                        .avatarUrl(userInfo.avatarUrl())
                        .htmlUrl(userInfo.htmlUrl())
                        .role(RoleEnum.NORMAL)
                        .build()));

        return jwtProvider.createToken(user);
    }
}
