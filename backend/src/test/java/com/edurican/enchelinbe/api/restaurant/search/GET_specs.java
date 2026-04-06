package com.edurican.enchelinbe.api.restaurant.search;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.BaseFixture;
import com.edurican.enchelinbe.config.RateLimitInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Spec: restaurants-search.md
 * GET /restaurants/search?query=&x=&y=&page=
 * Kakao Local Keyword Search API 단일 키워드 프록시.
 */
@EnchelinApiTest
@DisplayName("GET /restaurants/search")
public class GET_specs {

  static WireMockServer wireMock =
      new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

  @DynamicPropertySource
  static void overrideKakaoUrl(DynamicPropertyRegistry registry) {
    wireMock.start();
    registry.add("kakao.api.base-url", wireMock::baseUrl);
  }

  @AfterAll
  static void tearDown() {
    wireMock.stop();
  }

  @BeforeEach
  void resetStubs(@Autowired RateLimitInterceptor rateLimitInterceptor) {
    wireMock.resetAll();
    // 이전 테스트가 소진한 rate limit 카운터 초기화
    rateLimitInterceptor.clearAll();
  }

  @Test
  void Kakao_API_정상_응답_시_식당_목록을_반환한다(
      @Autowired Environment environment,
      @Autowired ObjectMapper objectMapper) {
    // Arrange
    wireMock.stubFor(get(urlPathEqualTo("/v2/local/search/keyword.json"))
        .withQueryParam("query", equalTo("스타벅스"))
        .willReturn(okJson(kakaoResponse("kakao-s1", "스타벅스강남점", "음식점 > 카페", 127.0, 37.5, "100"))));
    BaseFixture base = BaseFixture.create(environment, objectMapper);

    // Act
    ResponseEntity<String> response = base.client().getForEntity(
        "/restaurants/search?query=스타벅스&x=127.0&y=37.5", String.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"success\":true");
    assertThat(response.getBody()).contains("스타벅스강남점");
    assertThat(response.getBody()).contains("\"distance\":100.0");
  }

  @Test
  void query_누락_시_400을_반환한다(
      @Autowired Environment environment,
      @Autowired ObjectMapper objectMapper) {
    // Arrange
    BaseFixture base = BaseFixture.create(environment, objectMapper);

    // Act
    ResponseEntity<String> response = base.client().getForEntity(
        "/restaurants/search", String.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void query가_빈_문자열이면_400을_반환한다(
      @Autowired Environment environment,
      @Autowired ObjectMapper objectMapper) {
    // Arrange
    BaseFixture base = BaseFixture.create(environment, objectMapper);

    // Act
    ResponseEntity<String> response = base.client().getForEntity(
        "/restaurants/search?query=", String.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void Kakao_빈_결과면_빈_배열을_반환한다(
      @Autowired Environment environment,
      @Autowired ObjectMapper objectMapper) {
    // Arrange
    wireMock.stubFor(get(urlPathEqualTo("/v2/local/search/keyword.json"))
        .willReturn(okJson(emptyKakaoResponse())));
    BaseFixture base = BaseFixture.create(environment, objectMapper);

    // Act
    ResponseEntity<String> response = base.client().getForEntity(
        "/restaurants/search?query=없는가게", String.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"data\":[]");
  }

  @Test
  void 초당_5회_초과_호출_시_429를_반환한다(
      @Autowired Environment environment,
      @Autowired ObjectMapper objectMapper) {
    // Arrange
    wireMock.stubFor(get(urlPathEqualTo("/v2/local/search/keyword.json"))
        .willReturn(okJson(emptyKakaoResponse())));
    BaseFixture base = BaseFixture.create(environment, objectMapper);

    // Act: 6번 연속 호출 (rate limit은 @BeforeEach에서 초기화됨)
    ResponseEntity<String> lastResponse = null;
    for (int i = 0; i < 6; i++) {
      lastResponse = base.client().getForEntity(
          "/restaurants/search?query=테스트", String.class);
    }

    // Assert: 6번째 호출에서 429
    assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }

  // ==================== Helpers ====================

  private String kakaoResponse(String id, String name, String category,
      double x, double y, String distance) {
    return """
        {
          "meta": {"is_end": true, "pageable_count": 1},
          "documents": [{
            "id": "%s",
            "place_name": "%s",
            "category_name": "%s",
            "road_address_name": "서울시 강남구 테스트로 1",
            "address_name": "서울시 강남구",
            "phone": "",
            "x": "%s",
            "y": "%s",
            "place_url": "https://place.map.kakao.com/%s",
            "distance": "%s"
          }]
        }
        """.formatted(id, name, category, x, y, id, distance);
  }

  private String emptyKakaoResponse() {
    return """
        {
          "meta": {"is_end": true, "pageable_count": 0},
          "documents": []
        }
        """;
  }
}
