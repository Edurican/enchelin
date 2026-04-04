package com.edurican.enchelinbe.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubUserInfo(
        @JsonProperty("id") String id,
        @JsonProperty("login") String login,
        @JsonProperty("email") String email,
        @JsonProperty("avatar_url") String avatarUrl,
        @JsonProperty("html_url") String htmlUrl
) {
}
