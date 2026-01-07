package com.ziminpro.ums.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GitHubOAuthService {

    private final WebClient webClient;

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    public GitHubOAuthService() {
        this.webClient = WebClient.builder().build();
    }

    public Mono<GitHubAccessTokenResponse> exchangeCodeForToken(String code) {
        return webClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("code", code))
                .retrieve()
                .bodyToMono(GitHubAccessTokenResponse.class);
    }

    public Mono<GitHubUser> getGitHubUser(String accessToken) {
        return webClient.get()
                .uri("https://api.github.com/user")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .bodyToMono(GitHubUser.class);
    }

    public Mono<String> getGitHubUserEmail(String accessToken) {
        return webClient.get()
                .uri("https://api.github.com/user/emails")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .bodyToFlux(GitHubEmail.class)
                .filter(GitHubEmail::isPrimary)
                .next()
                .map(GitHubEmail::getEmail);
    }

    @Data
    public static class GitHubAccessTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        private String scope;
        private String error;

        @JsonProperty("error_description")
        private String errorDescription;
    }

    @Data
    public static class GitHubUser {
        private Long id;
        private String login;
        private String name;
        private String email;

        @JsonProperty("avatar_url")
        private String avatarUrl;
    }

    @Data
    public static class GitHubEmail {
        private String email;
        private boolean primary;
        private boolean verified;
    }
}
