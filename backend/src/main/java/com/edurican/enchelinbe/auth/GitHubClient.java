package com.edurican.enchelinbe.auth;

import com.edurican.enchelinbe.common.exception.BusinessException;
import com.edurican.enchelinbe.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class GitHubClient {

    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;

    public GitHubClient(
            @Value("${github.client-id}") String clientId,
            @Value("${github.client-secret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = RestClient.create();
    }

    public String exchangeCode(String code) {
        try {
            TokenResponse response = restClient.post()
                    .uri("https://github.com/login/oauth/access_token")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .body(new TokenRequest(clientId, clientSecret, code))
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new BusinessException(ErrorCode.GITHUB_AUTH_FAILED);
            }
            return response.accessToken();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("GitHub code exchange 실패", e);
            throw new BusinessException(ErrorCode.GITHUB_AUTH_FAILED);
        }
    }

    public GitHubUserInfo getUserInfo(String accessToken) {
        try {
            GitHubUserInfo userInfo = restClient.get()
                    .uri("https://api.github.com/user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(GitHubUserInfo.class);

            if (userInfo == null) {
                throw new BusinessException(ErrorCode.GITHUB_AUTH_FAILED);
            }
            return userInfo;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("GitHub 유저 정보 조회 실패", e);
            throw new BusinessException(ErrorCode.GITHUB_AUTH_FAILED);
        }
    }

    record TokenRequest(
            @JsonProperty("client_id") String clientId,
            @JsonProperty("client_secret") String clientSecret,
            String code) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }
}
