# 테스트 컨벤션

## 파일 구조

```
backend/src/test/java/com/edurican/enchelinbe/
├── api/
│   ├── EnchelinApiTest.java          # 통합 테스트 어노테이션
│   ├── FixtureConfiguration.java     # Fixture Bean 등록
│   ├── fixture/
│   │   ├── BaseFixture.java          # HTTP 클라이언트 (TestRestTemplate)
│   │   ├── SellerFixture.java
│   │   ├── ShopperFixture.java
│   │   └── ProductFixture.java
│   ├── seller/
│   │   ├── signUp/
│   │   │   └── POST_specs.java
│   │   ├── issueToken/
│   │   │   └── POST_specs.java
│   │   ├── me/
│   │   │   └── GET_specs.java
│   │   └── products/
│   │       ├── create/
│   │       │   └── POST_specs.java
│   │       ├── get/
│   │       │   └── GET_specs.java
│   │       └── list/
│   │           └── GET_specs.java
│   └── shopper/
│       ├── signUp/
│       │   └── POST_specs.java
│       └── ...
└── resources/
    └── application-test.yml
```

### 규칙
- 경로: `api/{도메인}/{액션}/{HTTP_METHOD}_specs.java`
- 하나의 API 엔드포인트 = 하나의 테스트 클래스
- 액션 이름은 API의 논리적 동작 기준 (signUp, issueToken, create, list 등)

---

## 클래스 작성

```java
@EnchelinApiTest
@DisplayName("POST /seller/signUp")
public class POST_specs {

  @Test
  void 올바르게_요청하면_204_No_Content_상태코드를_반환한다(@Autowired SellerFixture fixture) {
    // Arrange
    // Act
    // Assert
  }
}
```

### 규칙
- `@EnchelinApiTest`로 통합 테스트 컨텍스트 로드
- `@DisplayName`에 HTTP 메서드 + 경로 명시
- 클래스명: `POST_specs`, `GET_specs`, `PUT_specs`, `DELETE_specs`
- 메서드명: 한글, 띄어쓰기는 `_`로 대체
- Fixture는 `@Autowired` 파라미터 주입

---

## AAA 패턴

모든 테스트는 Arrange → Act → Assert 3단계로 작성한다.

```java
@Test
void 올바르게_요청하면_204_No_Content_상태코드를_반환한다(@Autowired SellerFixture fixture) {
  // Arrange
  String email = "seller@example.com";

  // Act
  var response = fixture.signUp(email, "seller1", "password1!", "contact@example.com");

  // Assert
  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
}
```

### 규칙
- `// Arrange`, `// Act`, `// Assert` 주석 필수
- Arrange: 테스트에 필요한 사전 조건 준비. Fixture 메서드 활용
- Act: 테스트 대상 API를 **한 번만** 호출
- Assert: `assertThat` (AssertJ) 사용. 하나의 논리적 검증에 집중

---

## Fixture 작성

### BaseFixture
- `record` 타입, `TestRestTemplate` + `ObjectMapper` 보유
- `get()`, `post()`, `put()`, `delete()` 등 HTTP 메서드 제공
- 모든 응답을 프로젝트의 `ApiResponse<T>`로 변환
- 토큰이 `null`이면 Authorization 헤더 생략

### 도메인 Fixture
```java
public record SellerFixture(BaseFixture base) {

  public static SellerFixture create(Environment environment, ObjectMapper objectMapper) {
    return new SellerFixture(BaseFixture.create(environment, objectMapper));
  }

  // ==================== API Calls ====================

  public ResponseEntity<Void> signUp(String email, String username, String password, String contactEmail) {
    // 실제 API 호출
  }

  public ResponseEntity<Void> signUp() {
    return signUp("seller@example.com", "seller1", "password1!", "contact@example.com");
  }

  // ==================== Convenience Methods ====================

  public String signUpAndGetToken() {
    signUp();
    return issueToken("seller@example.com", "password1!").getBody().accessToken();
  }
}
```

### 규칙
- `record` 타입, `BaseFixture`를 첫 번째 컴포넌트로 가짐
- `create()` 정적 팩토리로 생성
- API Calls 섹션: 실제 엔드포인트를 호출하는 메서드. 모든 파라미터를 받는 버전 + 기본값 오버로드
- Convenience Methods 섹션: 여러 API를 조합한 헬퍼 (signUpAndGetToken 등)
- `FixtureConfiguration`에 `@Bean @Scope("prototype")`으로 등록

---

## 검증 패턴별 가이드

### 필드 누락 검증
특정 필드를 빼고 `Map`으로 요청한다.
```java
@Test
void email_속성이_지정되지_않으면_400_Bad_Request_상태코드를_반환한다(@Autowired SellerFixture fixture) {
  // Arrange
  Map<String, Object> body = Map.of(
      "username", "seller1",
      "password", "password1!",
      "contactEmail", "contact@example.com"
  );

  // Act
  var response = fixture.signUpRaw(body);

  // Assert
  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
}
```
- Fixture에 `xxxRaw(Map<String, Object> body)` 메서드를 둔다
- 정상 요청의 필드 중 검증 대상만 빼서 요청

### 형식 검증
잘못된 형식의 값을 넣어 요청한다.
```java
@Test
void email_속성이_올바른_형식을_따르지_않으면_400_Bad_Request_상태코드를_반환한다(@Autowired SellerFixture fixture) {
  // Arrange
  // Act
  var response = fixture.signUp("invalid-email", "seller1", "password1!", "contact@example.com");

  // Assert
  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
}
```

### 중복 검증
같은 요청을 두 번 보낸다.
```java
@Test
void email_속성에_이미_존재하는_이메일_주소가_지정되면_400_Bad_Request_상태코드를_반환한다(@Autowired SellerFixture fixture) {
  // Arrange
  fixture.signUp("dup@example.com", "seller1", "password1!", "c1@example.com");

  // Act
  var response = fixture.signUp("dup@example.com", "seller2", "password2!", "c2@example.com");

  // Assert
  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
}
```

### 인증 검증
토큰을 `null`로 요청한다.
```java
@Test
void 접근_토큰을_사용하지_않으면_401_Unauthorized_상태코드를_반환한다(@Autowired SellerFixture fixture) {
  // Arrange
  // Act
  var response = fixture.getMe(null);

  // Assert
  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
}
```

### 권한(역할) 검증
다른 역할의 토큰으로 요청한다.
```java
@Test
void 판매자가_아닌_사용자의_접근_토큰을_사용하면_403_Forbidden_상태코드를_반환한다(
    @Autowired ShopperFixture shopperFixture,
    @Autowired ProductFixture productFixture) {
  // Arrange
  String shopperToken = shopperFixture.signUpAndGetToken();

  // Act
  var response = productFixture.registerProduct(shopperToken, "Product", ...);

  // Assert
  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
}
```

### 소유권 검증
다른 소유자의 리소스에 접근한다.
```java
@Test
void 다른_판매자가_등록한_상품_식별자를_사용하면_404_Not_Found_상태코드를_반환한다(@Autowired SellerFixture fixture) {
  // Arrange
  String tokenA = fixture.signUpAndGetToken("a@ex.com", "sellerA", ...);
  String tokenB = fixture.signUpAndGetToken("b@ex.com", "sellerB", ...);
  String productId = fixture.registerProductAndGetId(tokenA, ...);

  // Act
  var response = fixture.getProduct(tokenB, productId);

  // Assert
  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
}
```

### 페이지네이션 검증
데이터를 페이지 크기보다 많이 삽입한 뒤 순회한다.
```java
@Test
void 두_번째_페이지를_올바르게_반환한다(@Autowired ProductFixture fixture, @Autowired SellerFixture sellerFixture) {
  // Arrange: 15개 상품 등록 (페이지 크기 10)
  String token = sellerFixture.signUpAndGetToken();
  for (int i = 0; i < 15; i++) {
    fixture.registerProduct(token, "Product " + i, ...);
  }

  // Act
  var firstPage = fixture.browseProducts(shopperToken, null);
  var secondPage = fixture.browseProducts(shopperToken, firstPage.continuationToken());

  // Assert
  assertThat(secondPage.items()).hasSize(5);
}
```

### 정렬 검증
데이터의 시간 순서를 확인한다.
```java
@Test
void 상품_목록을_등록_시점_역순으로_정렬한다(@Autowired SellerFixture fixture) {
  // Arrange
  String token = fixture.signUpAndGetToken();
  fixture.registerProduct(token, "First", ...);
  fixture.registerProduct(token, "Second", ...);

  // Act
  var response = fixture.listProducts(token);

  // Assert
  assertThat(response.items().get(0).name()).isEqualTo("Second");
  assertThat(response.items().get(1).name()).isEqualTo("First");
}
```

### 암호화 검증
저장된 비밀번호가 원문과 다르고, 매처로 검증 가능한지 확인한다.
```java
@Test
void 비밀번호를_올바르게_암호화한다(@Autowired SellerFixture fixture) {
  // Arrange
  String rawPassword = "password1!";
  fixture.signUp("s@ex.com", "seller1", rawPassword, "c@ex.com");

  // Act
  String token = fixture.issueToken("s@ex.com", rawPassword).getBody().accessToken();

  // Assert: 토큰이 발행되면 암호화된 비밀번호로 매칭된 것
  assertThat(token).isNotBlank();
}
```

---

## 응답 검증 기준

| 상태코드 | 의미 | 검증 대상 |
|----------|------|-----------|
| 200 OK | 조회/토큰 발행 성공 | `response.getStatusCode()` + 응답 본문 |
| 201 Created | 리소스 생성 | `response.getStatusCode()` + Location 헤더 |
| 204 No Content | 성공 (본문 없음) | `response.getStatusCode()` |
| 400 Bad Request | 입력값 오류 | `response.getStatusCode()` |
| 401 Unauthorized | 인증 실패 | `response.getStatusCode()` |
| 403 Forbidden | 권한 없음 | `response.getStatusCode()` |
| 404 Not Found | 리소스 없음/소유권 불일치 | `response.getStatusCode()` |

---

## 테스트 데이터 규칙

- 테스트 간 데이터 격리: `@Transactional` 롤백 또는 H2 create-drop
- 이메일, 유저네임 등 유니크 값은 테스트마다 고유하게 생성
- Fixture의 기본값 오버로드를 활용하되, 검증 대상 필드는 명시적으로 지정
- 외부 API 의존은 테스트 프로파일에서 Mock 또는 Stub으로 대체
