# 백엔드 보안 & 구조 개선 계획 (Issue #38)

## Context

외부 코드 리뷰에서 12건의 보안/구조 이슈가 지적되었다. 코드를 실제로 확인한 결과 **대부분 유효**하며, 일부는 더 광범위하게 수정이 필요하다 (예: JWT 필터의 "모든 GET 요청 패스" 문제는 리뷰에 없지만 치명적). 동시에 리뷰가 과거 상태를 기반으로 한 것으로 보이는 항목도 있다 (예: `RestaurentController.java`는 이미 삭제된 상태).

**목적**: Critical/High 보안 취약점을 신속히 제거하고, Medium/Low는 구조 개선 PR로 별도 처리하여 회귀 리스크를 낮춘다. 시크릿 로테이션은 코드 작업이 아니라 사용자 수동 작업이므로 범위 밖으로 분리한다.

---

## 리뷰 타당성 검토 (12개 항목)

| # | 리뷰 항목 | 검증 결과 | 실제 위치 | 판정 |
|---|----------|-----------|----------|------|
| 1 | `.env` 프로덕션 시크릿 평문 | .gitignore에 `.env` 포함됨(라인 18). 커밋은 안 됐으나 로컬 유출 리스크는 존재 | `.env`, `.gitignore:18` | **유효** — 단, 코드 작업 아님 (별도 조치) |
| 2 | JWT 기본 시크릿 하드코딩 | `default-dev-secret-key-that-should-be-changed-in-production-env` 기본값 존재 | `application.yml:8` | **유효** |
| 3 | OAuth state 파라미터 없음 | `redirectToGitHub`에서 state 미생성, callback 미검증 | `AuthController.java:32-45` | **유효** |
| 4 | JWT 토큰 URL 쿼리 노출 | `.queryParam("token", token)` 확인 | `AuthController.java:51-56` | **유효** |
| 5 | POST `/restaurants/{id}/review-summary` 인가 없음 | `@AuthenticatedUser` 없음, JwtAuthenticationFilter는 POST는 검증하지만 이 엔드포인트는 유저 식별을 요구하지 않음 | `RestaurantController.java:73-77` | **유효** (엔드포인트 제거 결정) |
| 6 | RateLimiter 메모리 누수 | `ConcurrentHashMap` 엔트리 제거 로직 없음 (compute 내부에서 deque만 비움) | `RateLimitInterceptor.java:24, 39-52` | **유효** |
| 7 | `saveAndGetRestaurantsAround` 예외 은닉 | `catch (Exception e) { log.error(...) }` 확인, `searchByKeyword`는 `BusinessException` throw → 불일치 | `RestaurantService.java:194-196` vs `127-130` | **유효** |
| 8 | UserController가 Repository 직접 참조 | `UserService` 자체가 존재하지 않음 — 더 근본적 문제 | `UserController.java:18` | **유효** |
| 9 | Soft-deleted 리뷰 update/delete 가능 | `updateReview`/`deleteReview`가 `findById`만 사용, status 검증 없음 | `ReviewService.java:122-152` | **유효** |
| 10 | `RestaurentController.java` 잔존 | Glob 결과 **파일 없음** — 이미 정리됨 | — | **무효 (해결됨)** |
| 11 | `new RestTemplate()` 직접 생성 | 생성자에서 `this.restTemplate = new RestTemplate()`, 타임아웃 미설정 | `RestaurantService.java:42` | **유효** |
| 12 | CORS 설정 부재 | `WebConfig`에 `addCorsMappings` 없음, `SecurityConfig` 파일 자체가 없음 | `WebConfig.java` | **유효** — 현재 same-origin이라 당장 문제는 없음 |
| L1 | `application-local.yml` gitignore 미포함 | `.gitignore`에 미등록 | `.gitignore` | **유효** |
| L2 | SPA Forwarding 과매칭 | `/{path:[^.]*}` 등 — `/api` 매칭 가능성 존재 (현재는 `@RestController`가 먼저 잡혀서 동작 중) | `SpaForwardingController.java:10-15` | **유효 (잠재 리스크)** |
| L3 | Review 엔티티에 `@Column` 없음 | 실제로는 **모든 필드에 `@Column` 적용됨** — 리뷰 부정확. 단, 엔티티가 `service/` 패키지에 있는 구조 문제는 실재 | `service/Review.java:22-35` | **리뷰는 부정확, 다른 이슈는 유효** |

**추가 발견 (리뷰 누락):**
- **JwtAuthenticationFilter가 모든 GET 요청을 스킵** (`JwtAuthenticationFilter.java:34-36`) → 이후 GET 엔드포인트에 인증이 필요해지면 즉시 취약. 명시적 화이트리스트로 변경 필요.
- **AuthController 이중 콜백**: GET 콜백과 POST 토큰 엔드포인트가 병존 → OAuth 흐름 단일화 필요.
- **엔티티들이 `service/` 패키지에 위치** (`User.java`, `Review.java`, `Restaurant.java`) → 레이어드 아키텍처 위반.

---

## 실행 계획 (PR 분리 전략)

보안은 빠르게 merge, 구조는 리뷰 여유를 두고 분리한다. **이슈 #38은 PR 3개로 처리**하고, **엔티티 패키지 재배치는 별도 이슈(PR-4)** 로 진행한다.

### PR 단위 요약
- **PR-1** (이슈 #38): Critical 보안 — #2, #3, #4, #5
- **PR-2** (이슈 #38): High 안정성 — #6, #7, #9, #11, JWT 필터 화이트리스트
- **PR-3** (이슈 #38): Medium 구조 — #8, #12, L2(SPA 매칭)
- **PR-4** (신규 이슈): 엔티티 패키지 재배치 (`service/` → `entity/`) — 이번 작업과 독립

### PR-1: Critical 보안 (#2, #3, #4, #5) — 최우선

**목표**: OAuth/JWT 관련 공격 벡터 제거.

1. **JWT secret 기본값 제거** — `application.yml:8`
   - `${JWT_SECRET:default...}` → `${JWT_SECRET}` (기본값 삭제)
   - `application-local.yml`에 개발용 값 명시 (gitignore 처리 병행)
   - `JwtProvider` 생성자에서 길이 < 32 bytes면 `IllegalStateException` 기동 실패

2. **OAuth state 파라미터 추가** — `AuthController.java`
   - `redirectToGitHub`: `UUID.randomUUID()` state 생성 → `HttpSession`에 저장, GitHub authorize URL에 `state` 쿼리 추가
   - `githubCallback`: `@RequestParam String state` + session state 비교, 불일치 시 401
   - 새 `OAuthStateService`(또는 session helper) 도입

3. **JWT URL 노출 제거 (one-time code + POST exchange)** — `AuthController.java:51-56`
   - **방식 근거**: 웹(SPA)과 모바일(네이티브, deep link) 양쪽 모두 지원 가능한 유일한 방식. HttpOnly 쿠키는 모바일 네이티브에 부적합, fragment는 브라우저 히스토리에 잔존.
   - 짧은 TTL(60초) one-time authorization code 발급 → in-memory `Caffeine` 캐시에 `code → JWT` 매핑 저장 (`expireAfterWrite=60s`)
   - 웹: callback이 `{frontend-url}/auth/callback?code=...`로 리다이렉트 → SPA가 즉시 `POST /api/auth/exchange`로 code 교환
   - 모바일: deep link `myapp://auth/callback?code=...`로 리다이렉트 (추후 앱 도입 시) → 앱이 동일 exchange 엔드포인트 호출
   - 새 엔드포인트 `POST /api/auth/exchange {code}` 추가: JWT 반환, code는 사용 즉시 cache.invalidate
   - `app.frontend-url`, `app.mobile-scheme`(신규, optional) 설정 활용

4. **POST review-summary 엔드포인트 제거** — `RestaurantController.java:73-77`
   - 외부에서 임의로 호출하는 공격 벡터 자체를 제거 (사용자 결정)
   - `@PostMapping("/restaurants/{id}/review-summary")` 메서드 삭제
   - `ReviewSummaryService.forceGenerateSummary(id)`도 외부 호출 경로 없이 내부 스케줄/조건부 생성만 유지 (`SchedulingConfig` 기반)
   - 기존 `ReviewSummaryService.generateSummary()` 로직은 존치 (`persistSummary()`와 함께 내부 호출 유지)
   - 운영자 수동 재생성이 필요하면 DB 직접 조작 또는 추후 관리자 엔드포인트를 별도 이슈로 추가

**영향 파일:**
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-local.yml` (신규 또는 수정)
- `backend/src/main/java/com/edurican/enchelinbe/auth/JwtProvider.java`
- `backend/src/main/java/com/edurican/enchelinbe/controller/AuthController.java`
- `backend/src/main/java/com/edurican/enchelinbe/auth/AuthService.java`
- `backend/src/main/java/com/edurican/enchelinbe/controller/RestaurantController.java`
- `backend/src/main/java/com/edurican/enchelinbe/config/WebConfig.java`
- `.gitignore` (application-local.yml 추가)

### PR-2: High 구조/안정성 (#6, #7, #9, #11 + 추가 JWT 필터) — 중간 우선순위

**목표**: 런타임 안정성과 데이터 무결성 확보.

1. **RateLimiter 메모리 누수 해결** — `RateLimitInterceptor.java`
   - `ConcurrentHashMap` → `Caffeine.newBuilder().expireAfterAccess(5, MINUTES).build()`로 교체
   - 내부 `Deque<Long>`는 유지하되 `Cache` 만료 시 자동 제거
   - `clearAll()`은 `cache.invalidateAll()`로 변경 (테스트 호환)

2. **`saveAndGetRestaurantsAround` 예외 처리 일관화** — `RestaurantService.java:194-196`
   - `searchByKeyword`와 동일하게 `BusinessException(ErrorCode.KAKAO_API_ERROR)` throw
   - 단, 이미 저장한 부분 결과를 살리고 싶다면 부분 실패 로깅 + 호출자에게 명시적으로 전파
   - 컨트롤러 레벨에서 `ErrorCode.KAKAO_API_ERROR` 응답 매핑 확인

3. **Soft-deleted 리뷰 접근 차단** — `ReviewService.java:122-152`
   - `ReviewRepository`에 `findByIdAndStatus(Long id, EntityStatus status)` 추가
   - `updateReview`/`deleteReview`에서 `findByIdAndStatus(reviewId, ACTIVE)` 사용
   - 없으면 `REVIEW_NOT_FOUND` 반환 (DELETED를 노출하지 않음)

4. **RestTemplate Bean화** — `RestaurantService.java:42`
   - 새 `RestTemplateConfig` 또는 기존 `WebConfig`에 `@Bean RestTemplate kakaoRestTemplate()` 정의
   - `HttpComponentsClientHttpRequestFactory`로 connect=2s, read=3s 타임아웃 설정
   - `RestaurantService` 생성자에서 주입받기 (현재 생성자에서 `new` 제거)

5. **JwtAuthenticationFilter 화이트리스트화** — `JwtAuthenticationFilter.java:24-39`
   - 현재 "모든 GET 스킵"을 제거
   - `/api/auth/**`, `GET /restaurants/**`, `GET /reviews/**`, `GET /users/**`, `/actuator/**` 등 공개 엔드포인트만 명시적으로 스킵
   - `AntPathMatcher` 사용

**영향 파일:**
- `backend/src/main/java/com/edurican/enchelinbe/config/RateLimitInterceptor.java`
- `backend/src/main/java/com/edurican/enchelinbe/service/RestaurantService.java`
- `backend/src/main/java/com/edurican/enchelinbe/service/ReviewService.java`
- `backend/src/main/java/com/edurican/enchelinbe/repository/ReviewRepository.java`
- `backend/src/main/java/com/edurican/enchelinbe/auth/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/edurican/enchelinbe/config/WebConfig.java` (RestTemplate Bean)
- `backend/build.gradle` (Caffeine 의존성)

### PR-3: Medium 구조 리팩토링 (#8, #12, L2) — 후순위

**목표**: 레이어드 아키텍처 정리, 미래 배포 시나리오 대비.

1. **UserService 도입** — `UserController.java:18`
   - 새 `UserService` 클래스 생성 (`getUserProfile(Long userId)` 등)
   - `UserController`는 `UserService` 주입만 유지
   - 향후 role/권한 확장 포인트 확보

2. **CORS 설정 추가** — `WebConfig.java`
   - `addCorsMappings(CorsRegistry registry)` 오버라이드
   - `allowedOrigins(frontendUrl)`, allowed methods/headers 명시
   - `app.frontend-url` 프로퍼티 재사용

3. **SpaForwardingController 안전화** — `SpaForwardingController.java:10-15`
   - `@RequestMapping`에서 `/api/**` 명시적 배제
   - 혹은 `WebMvcConfigurer.addResourceHandlers`로 SPA 매칭을 리팩토링

**영향 파일:**
- `backend/src/main/java/com/edurican/enchelinbe/controller/UserController.java`
- `backend/src/main/java/com/edurican/enchelinbe/service/UserService.java` (신규)
- `backend/src/main/java/com/edurican/enchelinbe/config/WebConfig.java`
- `backend/src/main/java/com/edurican/enchelinbe/config/SpaForwardingController.java`

### PR-4 (별도 이슈): 엔티티 패키지 재배치

**목표**: 레이어드 아키텍처 정합성. 이슈 #38과 분리해 `refactor/issue-XX-entity-relocation` 브랜치로 진행.

1. `service/User.java`, `service/Review.java`, `service/Restaurant.java` → `entity/` 패키지로 이동
2. 모든 import 경로 업데이트 (IntelliJ Move Refactor 또는 수동)
3. 프로젝트 메모리 Hot Paths 갱신

**중요**: PR-1~3가 모두 merge된 후에 시작. 진행 중인 브랜치와의 충돌을 피하기 위해 병렬 작업 금지.

---

## 범위 외 (사용자 수동 조치)

- **#1 시크릿 로테이션**: Railway 환경변수 관리 콘솔에서 JWT_SECRET, GITHUB_CLIENT_SECRET, KAKAO_API_KEY, ANTHROPIC_API_KEY, DB 패스워드 재발급. GitHub/Anthropic/Kakao 개발자 콘솔에서도 키 리보크. **코드 PR로는 불가능.**
- **#L1 로컬 DB 비밀번호 정리**: `application-local.yml`의 `enchelin/enchelin`은 로컬 개발용 더미이지만 방침에 따라 .gitignore 처리 권장 (PR-1에 포함).

---

## 검증 계획

### PR-1 검증
- **JWT secret**: `JWT_SECRET` unset 상태에서 `./gradlew bootRun` → 기동 실패 확인
- **OAuth state**:
  - 정상 흐름: `/api/auth/github` → GitHub → callback → JWT 발급 성공
  - 공격 시뮬: state 없이 callback 직접 호출 → 401
  - state 위조: 다른 값으로 callback 호출 → 401
- **one-time code**:
  - 정상 흐름: callback → `?code=...` → POST `/api/auth/exchange` → JWT 수신
  - 재사용 공격: 같은 code를 두 번 exchange → 두 번째 실패
  - TTL 만료: 60초 대기 후 exchange → 실패
- **review-summary 엔드포인트 제거**: `POST /restaurants/{id}/review-summary` → 404 (경로 자체 미등록), `GET /restaurants/{id}/review-summary`는 기존대로 200
- **AI 요약 자동 생성 유지**: 내부 호출/스케줄 경로로 요약이 여전히 생성되는지 확인 (DB에 summary 레코드 생성 관찰)

### PR-2 검증
- **RateLimiter**: 다수의 IP(1000개)에서 호출 후 5분 대기 → 캐시 크기 감소 확인 (통합 테스트 또는 JMH)
- **saveAndGetRestaurantsAround**: Kakao API mock을 ConnectTimeout으로 설정 → `BusinessException` 전파, 컨트롤러 5xx 응답
- **Soft-delete 차단**:
  - 리뷰 생성 → soft delete → update/delete 재시도 → 404 (REVIEW_NOT_FOUND)
  - 기존 AAA 패턴 통합 테스트 추가
- **RestTemplate**: Kakao API mock을 5초 지연시 → 3초에 read timeout 발생 확인
- **JWT 필터**: 공개 경로 목록에 없는 임의의 GET 엔드포인트 → 401 (기존 GET 엔드포인트 회귀 없음 확인)

### PR-3 검증
- **UserService**: 기존 `GET /users/{userId}` 응답 동일 (회귀 테스트)
- **CORS**: `frontend-url`과 다른 origin에서 preflight → 차단 확인, 지정 origin → 허용 확인
- **SPA forwarding**: `/api/unknown` → 404 (SPA HTML이 아닌 JSON 에러), `/some-spa-route` → index.html

### 공통
- `./gradlew test` 전체 통과
- `./gradlew build` 성공
- 수동 E2E: GitHub 로그인 → 리뷰 작성 → 수정 → 삭제 → 프로필 조회 전체 흐름 동작

---

## 참고 자료

- 영향 파일 Hot Path (프로젝트 메모리): `application.yml`, `ReviewService.java`, `Review.java` — 이번 PR들이 모두 touch함 → 신중한 리뷰 필요
- 기존 TDD 스타일(프로젝트 메모리): 통합 테스트 Fixture + AAA 패턴 사용 — PR-2의 리뷰 status 검증 테스트에 적용
