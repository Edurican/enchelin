package com.edurican.enchelinbe.api.restaurant.nearby;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.edurican.enchelinbe.api.EnchelinApiTest;
import com.edurican.enchelinbe.api.fixture.BaseFixture;
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

@EnchelinApiTest
@DisplayName("GET /restaurant/nearby")
public class GET_specs {

  static WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

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
  void resetStubs() {
    wireMock.resetAll();
  }

  private BaseFixture createFixture(Environment environment, ObjectMapper objectMapper) {
    return BaseFixture.create(environment, objectMapper);
  }

  @Test
  void 주변_식당을_검색하면_성공을_반환한다(
      @Autowired Environment environment,
      @Autowired ObjectMapper objectMapper) {
    // Arrange
    stubKakaoResponse("한식", kakaoResponse("kakao-100", "한식당", "음식점 > 한식", 127.0, 37.5));
    stubEmptyForOtherKeywords("한식");

    BaseFixture base = createFixture(environment, objectMapper);

    // Act
    ResponseEntity<String> response = base.client().getForEntity(
        "/restaurant/nearby?x=127.0&y=37.5&radius=1000", String.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"success\":true");
    assertThat(response.getBody()).contains("한식당");
  }

  @Test
  void 검색_결과가_없으면_빈_목록을_반환한다(
      @Autowired Environment environment,
      @Autowired ObjectMapper objectMapper) {
    // Arrange
    stubAllEmpty();
    BaseFixture base = createFixture(environment, objectMapper);

    // Act
    ResponseEntity<String> response = base.client().getForEntity(
        "/restaurant/nearby?x=127.0&y=37.5&radius=1000", String.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"success\":true");
    assertThat(response.getBody()).contains("[]");
  }

  @Test
  void 중복된_식당은_하나만_반환한다(
      @Autowired Environment environment,
      @Autowired ObjectMapper objectMapper) {
    // Arrange: 두 키워드에서 같은 kakaoApiId로 반환
    stubKakaoResponse("한식", kakaoResponse("kakao-200", "중복식당", "음식점 > 한식", 127.0, 37.5));
    stubKakaoResponse("양식", kakaoResponse("kakao-200", "중복식당", "음식점 > 양식", 127.0, 37.5));
    stubEmptyForOtherKeywords("한식", "양식");
    BaseFixture base = createFixture(environment, objectMapper);

    // Act
    ResponseEntity<String> response = base.client().getForEntity(
        "/restaurant/nearby?x=127.0&y=37.5&radius=1000", String.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    String body = response.getBody();
    int count = body.split("중복식당").length - 1;
    assertThat(count).isEqualTo(1);
  }

  @Test
  void Kakao_API_오류_시에도_수집된_결과를_반환한다(
      @Autowired Environment environment,
      @Autowired ObjectMapper objectMapper) {
    // Arrange: 한식은 정상, 양식은 500 에러
    stubKakaoResponse("한식", kakaoResponse("kakao-300", "정상식당", "음식점 > 한식", 127.0, 37.5));
    wireMock.stubFor(get(urlPathEqualTo("/v2/local/search/keyword.json"))
        .withQueryParam("query", equalTo("양식"))
        .willReturn(serverError()));
    stubEmptyForOtherKeywords("한식", "양식");
    BaseFixture base = createFixture(environment, objectMapper);

    // Act
    ResponseEntity<String> response = base.client().getForEntity(
        "/restaurant/nearby?x=127.0&y=37.5&radius=1000", String.class);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("정상식당");
  }

  // ==================== Helper Methods ====================

  private void stubKakaoResponse(String keyword, String responseBody) {
    wireMock.stubFor(get(urlPathEqualTo("/v2/local/search/keyword.json"))
        .withQueryParam("query", equalTo(keyword))
        .willReturn(okJson(responseBody)));
  }

  private void stubEmptyForOtherKeywords(String... excludeKeywords) {
    String[] allKeywords = {"한식", "양식", "일식", "중식", "분식", "태국음식", "베트남음식",
        "치킨", "피자", "패스트푸드", "카페", "고기", "술집", "베이커리"};
    java.util.Set<String> excluded = java.util.Set.of(excludeKeywords);
    for (String kw : allKeywords) {
      if (!excluded.contains(kw)) {
        wireMock.stubFor(get(urlPathEqualTo("/v2/local/search/keyword.json"))
            .withQueryParam("query", equalTo(kw))
            .willReturn(okJson(emptyKakaoResponse())));
      }
    }
  }

  private void stubAllEmpty() {
    wireMock.stubFor(get(urlPathEqualTo("/v2/local/search/keyword.json"))
        .willReturn(okJson(emptyKakaoResponse())));
  }

  private String emptyKakaoResponse() {
    return """
        {
          "meta": {"is_end": true, "pageable_count": 0},
          "documents": []
        }
        """;
  }

  private String kakaoResponse(String id, String name, String category, double x, double y) {
    return """
        {
          "meta": {"is_end": true, "pageable_count": 1},
          "documents": [{
            "id": "%s",
            "place_name": "%s",
            "category_name": "%s",
            "road_address_name": "서울시 강남구 테스트로 1",
            "address_name": "서울시 강남구",
            "phone": "02-1234-5678",
            "x": "%s",
            "y": "%s",
            "place_url": "https://place.map.kakao.com/%s"
          }]
        }
        """.formatted(id, name, category, x, y, id);
  }
}
