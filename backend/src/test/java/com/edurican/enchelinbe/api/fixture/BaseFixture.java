package com.edurican.enchelinbe.api.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.http.*;

public record BaseFixture(TestRestTemplate client, ObjectMapper objectMapper) {

  public static BaseFixture create(Environment environment, ObjectMapper objectMapper) {
    TestRestTemplate client = new TestRestTemplate(new RestTemplateBuilder());
    LocalHostUriTemplateHandler uriTemplateHandler = new LocalHostUriTemplateHandler(environment);
    client.setUriTemplateHandler(uriTemplateHandler);
    return new BaseFixture(client, objectMapper);
  }

  // ==================== GET ====================

  public <T> ResponseEntity<T> get(String url, Class<T> responseType, Object... urlVariables) {
    return get(url, null, responseType, urlVariables);
  }

  public <T> ResponseEntity<T> get(
      String url, String token, Class<T> responseType, Object... urlVariables) {
    return exchange(url, HttpMethod.GET, null, token, responseType, urlVariables);
  }

  // ==================== POST ====================

  public <T> ResponseEntity<T> post(
      String url, Object request, Class<T> responseType, Object... urlVariables) {
    return post(url, request, null, responseType, urlVariables);
  }

  public <T> ResponseEntity<T> post(
      String url, Object request, String token, Class<T> responseType, Object... urlVariables) {
    return exchange(url, HttpMethod.POST, request, token, responseType, urlVariables);
  }

  // ==================== POST (void - 204) ====================

  public ResponseEntity<Void> postForVoid(String url, Object request) {
    return postForVoid(url, request, null);
  }

  public ResponseEntity<Void> postForVoid(String url, Object request, String token) {
    return exchange(url, HttpMethod.POST, request, token, Void.class);
  }

  // ==================== PUT ====================

  public <T> ResponseEntity<T> put(
      String url, Object request, String token, Class<T> responseType, Object... urlVariables) {
    return exchange(url, HttpMethod.PUT, request, token, responseType, urlVariables);
  }

  // ==================== DELETE ====================

  public <T> ResponseEntity<T> delete(
      String url, String token, Class<T> responseType, Object... urlVariables) {
    return exchange(url, HttpMethod.DELETE, null, token, responseType, urlVariables);
  }

  // ==================== Raw (Map body) ====================

  public ResponseEntity<String> postRaw(String url, Map<String, Object> body) {
    return postRaw(url, body, null);
  }

  public ResponseEntity<String> postRaw(String url, Map<String, Object> body, String token) {
    HttpHeaders headers = createHeaders(token);
    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
    return client.exchange(url, HttpMethod.POST, entity, String.class);
  }

  // ==================== Core Exchange Method ====================

  private <T> ResponseEntity<T> exchange(
      String url,
      HttpMethod method,
      Object request,
      String token,
      Class<T> responseType,
      Object... urlVariables) {
    HttpHeaders headers = createHeaders(token);
    HttpEntity<?> entity =
        (request != null) ? new HttpEntity<>(request, headers) : new HttpEntity<>(headers);
    return client.exchange(url, method, entity, responseType, urlVariables);
  }

  private HttpHeaders createHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null && !token.isBlank()) {
      headers.setBearerAuth(token);
    }
    return headers;
  }
}
