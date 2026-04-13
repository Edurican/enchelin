package com.edurican.enchelinbe.client;

import com.edurican.enchelinbe.common.exception.BusinessException;
import com.edurican.enchelinbe.common.exception.ErrorCode;
import com.edurican.enchelinbe.service.SummaryJson;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Profile("!test")
@Slf4j
public class ClaudeRestClient implements ClaudeClient {

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 2048;
    private static final double TEMPERATURE = 0.2;

    private static final String SYSTEM_PROMPT = """
            You are a restaurant review summarizer. Analyze the provided Korean restaurant reviews and output ONLY a JSON object with this exact structure:
            {
              "atmosphere":       [{"tag": "...", "evidence_review_ids": [id1, id2]}] or null,
              "parking":          [{"tag": "...", "evidence_review_ids": [id1, id2]}] or null,
              "visit_purpose":    [{"tag": "...", "evidence_review_ids": [id1, id2]}] or null,
              "taste":            [{"tag": "...", "evidence_review_ids": [id1, id2]}] or null,
              "signature_menu":   [{"tag": "...", "evidence_review_ids": [id1, id2]}] or null,
              "companion":        [{"tag": "...", "evidence_review_ids": [id1, id2]}] or null,
              "service":          [{"tag": "...", "evidence_review_ids": [id1, id2]}] or null,
              "cost_performance": [{"tag": "...", "evidence_review_ids": [id1, id2]}] or null,
              "portion":          [{"tag": "...", "evidence_review_ids": [id1, id2]}] or null
            }

            Category descriptions:
            - atmosphere: 식당 분위기 (예: 편안한 분위기, 아늑한 인테리어)
            - parking: 주차 관련 (예: 건물 지하 주차 가능, 주차 공간 부족)
            - visit_purpose: 방문 목적/상황 (예: 점심식사, 기념일 식사, 회식)
            - taste: 맛 묘사 (예: 고소한 맛, 짭조름한 양념, 달콤한 소스)
            - signature_menu: 자주 언급된 대표 메뉴 이름 (예: 크림파스타, 삼겹살)
            - companion: 동반인 유형 (예: 연인과 방문, 가족 모임)
            - service: 서비스/직원 (예: 친절한 직원, 빠른 응대)
            - cost_performance: 가격 및 가성비 (예: 가성비 좋은 편, 가격이 비싼 편)
            - portion: 음식 양 (예: 양이 넉넉한 편, 양이 적은 편)

            Rules (strictly enforce):
            1. evidence_review_ids must only contain IDs from the provided reviews.
            2. evidence_review_ids must not be empty.
            3. tag must be written in Korean, 15 characters or less.
            4. signature_menu tag must appear as a substring in at least one review comment.
            5. If no reviews mention a category, set that field to null (do not include empty arrays).
            6. Output ONLY the JSON object. No explanation, no markdown, no extra text.
            """;

    private final String apiKey;
    private final String modelVersion;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ClaudeRestClient(@Value("${anthropic.api-key}") String apiKey,
                            @Value("${summary.model-version}") String modelVersion,
                            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.modelVersion = modelVersion;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    @Override
    public SummaryJson generate(String restaurantName, String category, List<ReviewForSummary> reviews) {
        String userMessage = buildUserMessage(restaurantName, category, reviews);

        Exception lastException = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            if (attempt > 0) {
                try {
                    Thread.sleep(1000L * (1 << (attempt - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(ErrorCode.CLAUDE_API_ERROR);
                }
            }
            try {
                return callApi(userMessage);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (status == 429 || status >= 500) {
                    log.warn("Claude API {}번째 시도 실패(재시도 예정) — status={}", attempt + 1, status);
                    lastException = e;
                } else {
                    log.error("Claude API 호출 실패 — status={}", status, e);
                    throw new BusinessException(ErrorCode.CLAUDE_API_ERROR);
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Claude API {}번째 시도 중 예외(재시도 예정)", attempt + 1, e);
                lastException = e;
            }
        }
        log.error("Claude API 3회 재시도 모두 실패", lastException);
        throw new BusinessException(ErrorCode.CLAUDE_API_ERROR);
    }

    private SummaryJson callApi(String userMessage) {
        MessageRequest request = new MessageRequest(
                modelVersion, MAX_TOKENS, TEMPERATURE, SYSTEM_PROMPT,
                List.of(new Message("user", userMessage))
        );

        MessageResponse response = restClient.post()
                .uri(ANTHROPIC_API_URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .retrieve()
                .body(MessageResponse.class);

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new BusinessException(ErrorCode.CLAUDE_API_ERROR);
        }

        String json = response.content().get(0).text();
        try {
            return objectMapper.readValue(json, SummaryJson.class);
        } catch (Exception e) {
            log.error("Claude 응답 JSON 파싱 실패: {}", json, e);
            throw new BusinessException(ErrorCode.CLAUDE_API_ERROR);
        }
    }

    private String buildUserMessage(String restaurantName, String category, List<ReviewForSummary> reviews) {
        String reviewsText = reviews.stream()
                .map(r -> "- id=%d rating=%d comment=\"%s\"".formatted(
                        r.reviewId(), r.rating(), r.comment()))
                .collect(Collectors.joining("\n"));

        return """
                Restaurant: %s
                Category: %s
                Reviews (%d total):
                %s
                """.formatted(restaurantName, category, reviews.size(), reviewsText);
    }

    record MessageRequest(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            double temperature,
            String system,
            List<Message> messages
    ) {}

    record Message(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessageResponse(List<ContentBlock> content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContentBlock(String type, String text) {}
}
