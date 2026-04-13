# 식당 AI 리뷰 요약 - 구현 계획

## Context

식당별 리뷰를 Claude API로 구조화 요약하여 DB에 저장하고, 조회 시 LLM 호출 없이 서빙하는 기능.
사용자 100명 미만 소규모 서비스. 배치 선행 → 증분 갱신 → 할루시네이션 3단 방어 원칙.

BE와 FE를 별도 이슈로 분리하여 관리한다.

---

## 0. 사전 작업: 이슈 & 브랜치 생성

### 이슈 1 (BE)
```bash
gh issue create \
  --title "[BE] 식당 AI 리뷰 요약 생성 · 검증 · 조회 API" \
  --label "type: feature" --label "scope: BE" --label "priority: high" \
  --body "$(cat <<'EOF'
## 개요
Claude API를 활용하여 식당별 리뷰를 구조화 요약하고, 사후 검증 후 DB에 저장.
조회 시 LLM 호출 없이 저장된 요약을 서빙.

## 주요 작업
- [ ] DB 마이그레이션 (`restaurant_review_summary` 테이블)
- [ ] 해시 기반 변경 감지 (`SourceHashCalculator`)
- [ ] Claude API 클라이언트 (`ClaudeClient` 인터페이스 + 구현체 + Fake)
- [ ] 사후 검증기 (`SummaryValidator` — 5종 검증)
- [ ] 단일 식당 요약 생성 서비스
- [ ] 조회 API (`GET /restaurants/{id}/review-summary`)
- [ ] 배치 잡 (`@Scheduled` 매일 04:00 KST, 일일 10건)
- [ ] 통합 테스트 (TDD, Fixture+AAA)

## 참고
- 모델: claude-sonnet-4-6, temperature 0.2
- 검증 정책: passed만 노출, degraded/failed는 저장만
- 최소 리뷰 3개, 최대 50개 입력
EOF
)"
```

### 이슈 2 (FE)
```bash
gh issue create \
  --title "[FE] 식당 상세 페이지 AI 리뷰 요약 카드" \
  --label "type: feature" --label "scope: FE" \
  --body "$(cat <<'EOF'
## 개요
식당 상세 화면에 AI 리뷰 요약 카드를 렌더링.
BE API 응답 status에 따라 요약 내용 또는 "리뷰 부족" 문구 표시.

## 주요 작업
- [ ] `fetchReviewSummary` API 함수 추가
- [ ] `ReviewSummaryCard.vue` 컴포넌트 생성
- [ ] `RestaurantDetailView.vue`에 요약 카드 통합
- [ ] 테스트

## API 연동
- `GET /restaurants/{id}/review-summary`
- 응답: `{ status: "available"|"insufficient"|"unavailable", summary, generated_at, source_review_count, message }`
EOF
)"
```

### 브랜치 생성
BE 이슈 번호 기준으로 브랜치 생성:
```bash
git checkout dev && git pull origin dev
git checkout -b feat/issue-{BE이슈번호}-ai-review-summary
```

---

## 1. 핵심 설계 결정

| 항목 | 결정 | 근거 |
|------|------|------|
| LLM 클라이언트 | `RestClient` 직접 사용 | `GitHubClient.java` 패턴 일치, 추가 의존성 0 |
| JSONB 매핑 | `@JdbcTypeCode(SqlTypes.JSON)` | Hibernate 6 네이티브 지원 |
| BIGINT[] 매핑 | `@JdbcTypeCode(SqlTypes.ARRAY)` | Hibernate 6 네이티브 지원 |
| 스케줄링 설정 | `SchedulingConfig` + `@ConditionalOnProperty` | 테스트/로컬에서 비활성화 |
| 재시도 | 클라이언트 내 수동 3회 지수 백오프 | spring-retry 의존성 불필요 |
| BaseEntity 상속 | **안 함** | PK 전략 상이 (auto-increment X), status/createdAt 불필요 |
| 추가 의존성 | **0개** | RestClient, Hibernate 6 JSON/ARRAY, MessageDigest 모두 기존 스택 |
| 요약 구조 | **카테고리 기반 태그** | 자유 텍스트(positive/negative) 대신 9개 카테고리별 태그 + evidenceReviewIds |
| mentionCount | **제거** | evidenceReviewIds.length로 대체 — Claude 생성 오류 원인 제거 |

---

## 2. 구현 단계 (BE)

### 단계 1: DB 마이그레이션

**파일:** `backend/src/main/resources/db/migration/V2__add_restaurant_review_summary.sql`

```sql
CREATE TABLE restaurant_review_summary (
    restaurant_id       BIGINT PRIMARY KEY REFERENCES restaurants (id),
    summary_json        JSONB        NOT NULL,
    source_hash         VARCHAR(64)  NOT NULL,
    source_review_count INTEGER      NOT NULL,
    source_review_ids   BIGINT[]     NOT NULL,
    model_version       VARCHAR(50)  NOT NULL,
    prompt_version      VARCHAR(20)  NOT NULL,
    validation_status   VARCHAR(20)  NOT NULL DEFAULT 'pending',
    generated_at        TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_summary_generated_at ON restaurant_review_summary (generated_at);
```

### 단계 2: 엔티티 & 리포지토리

**신규 파일:**
- `service/RestaurantReviewSummary.java` — JPA 엔티티 (BaseEntity 미상속)
  - `restaurantId` (@Id, 수동 할당)
  - `summaryJson` (@JdbcTypeCode JSON → `SummaryJson` 객체)
  - `sourceReviewIds` (@JdbcTypeCode ARRAY → `Long[]`)
- `service/SummaryJson.java` — summary_json JSONB 매핑 record
  - **카테고리 기반 9개 필드** (각 필드 nullable — 리뷰에 언급 없으면 null):
    - `atmosphere` — 분위기 (편안한 분위기, 소박한 분위기 등)
    - `parking` — 주차 공간 (건물 지하 주차 가능 등)
    - `visitPurpose` — 방문 목적 (점심식사, 기념일 등)
    - `taste` — 맛 (고소한, 짭조름한 등)
    - `signatureMenu` — 대표 메뉴 (크림파스타, 함박스테이크 등)
    - `companion` — 동반인 (친구, 연인 등)
    - `service` — 서비스 (친절한 직원, 빠른 응대 등)
    - `costPerformance` — 가성비/가격대 (가성비 좋은 편 등)
    - `portion` — 양/포션 (양이 넉넉한 편 등)
  - 각 카테고리 타입: `List<TagItem>` (nullable) — 카테고리당 복수 태그 가능
  - `TagItem` record — `tag` (String) + `evidenceReviewIds` (List<Long>)
  - `mentionCount` 없음 — `evidenceReviewIds.size()`로 대체
- `repository/RestaurantReviewSummaryRepository.java`

**수정 파일:**
- `repository/ReviewRepository.java` — 쿼리 1개 추가:
  - `findActiveReviewsByRestaurantId(restaurantId)` — status=ACTIVE, LENGTH(comment)>=5, 최신순 50건
  - ~~`findActiveReviewIdAndUpdatedAt`~~ — `SourceHashCalculator`가 `Review` 엔티티를 직접 받으므로 불필요, 생략

### 단계 3: 해시 유틸

**신규:** `service/SourceHashCalculator.java`
- `String compute(List<Review> reviews)` — sorted (id, updatedAt.epoch) → SHA-256 hex
- 순수 함수, 상태 없음

### 단계 4: LLM 클라이언트

**신규 패키지:** `com.edurican.enchelinbe.client`

- `client/ClaudeClient.java` — 인터페이스
  - `SummaryJson generate(String restaurantName, String category, List<ReviewForSummary> reviews)`
- `client/ReviewForSummary.java` — record (reviewId, rating, comment)
- `client/ClaudeRestClient.java` — 프로덕션 구현
  - `@Component` + `@Profile("!test")`
  - `RestClient.create()` (GitHubClient 패턴)
  - `@Value("${anthropic.api-key}")` 로 키 주입
  - POST `https://api.anthropic.com/v1/messages` (model: claude-sonnet-4-6, temp: 0.2)
  - 시스템 프롬프트에 9개 카테고리 JSON 스키마 강제 + 제약 조건:
    1. `evidenceReviewIds`는 입력 리뷰 ID만 사용
    2. `tag`는 한국어 자연어 문장, 15자 이내
    3. 리뷰에 언급되지 않은 카테고리는 `null`로 반환
    4. `signatureMenu`의 tag는 리뷰 코멘트에 substring으로 존재해야 함
  - 3회 재시도 (1s→2s→4s 백오프, 429/5xx만)
  - 실패 시 `BusinessException(CLAUDE_API_ERROR)`
- `client/FakeClaudeClient.java` — 테스트용
  - `@Component` + `@Profile("test")`
  - 입력 리뷰에서 결정론적 고정 JSON 반환

### 단계 5: 사후 검증기

**신규:** `service/SummaryValidator.java`

5종 검증 (카테고리 기반):

| # | 검증 | 대상 카테고리 | 실패 시 |
|---|------|---------------|---------|
| 1 | `evidenceReviewIds` 모두 입력 리뷰에 존재 | 전체 9개 | 해당 태그 제거 |
| 2 | `evidenceReviewIds`가 비어있지 않음 | 전체 9개 | 해당 태그 제거 |
| 3 | `signatureMenu`의 tag가 최소 1건 리뷰 코멘트에 substring 존재 | signatureMenu만 | 해당 태그 제거 |
| 4 | `tag`가 null이 아니고 공백이 아님 | 전체 9개 | 해당 태그 제거 |
| 5 | `tag` 길이 15자 이내 | 전체 9개 | 해당 태그 제거 |

- `mentionCount` 검증 제거 — 필드 자체가 없으므로 불필요
- positive/negative 평점 검증 제거 — 카테고리가 긍정/부정으로 나뉘지 않으므로 불필요
- 결과: passed (전체 통과) / degraded (일부 제거, 1개+ 생존) / failed (유효 0개)
- 카테고리 내 모든 태그가 제거되면 해당 카테고리를 `null`로 설정

### 단계 6: 요약 생성 서비스

**신규:** `service/ReviewSummaryService.java`
- `generateSummaryIfStale(Long restaurantId)` — 오케스트레이션
  - 리뷰 로드 → 3개 미만 스킵 → 해시 비교 → LLM 호출 → 검증 → 저장
- `getSummaryForRestaurant(Long restaurantId)` — 조회 로직
  - passed → available, degraded/failed → unavailable, 미생성 → insufficient
- `computeSourceHash(...)` 위임

### 단계 7: 조회 API

**신규:** `dto/ReviewSummaryResponse.java` — record
- `status` ("available" | "insufficient"), `summary` (nullable), `generatedAt` (nullable), `sourceReviewCount` (nullable), `message` (nullable)
- `static available(...)` / `static insufficient()` / `static unavailable()` 팩토리 메서드

**수정:** `controller/RestaurantController.java`
- `ReviewSummaryService` 주입 추가
- `@GetMapping("/restaurants/{id}/review-summary")` 엔드포인트 추가

### 단계 8: 배치 잡

**신규:**
- `config/SchedulingConfig.java` — `@Configuration` + `@EnableScheduling`
- `service/ReviewSummaryScheduler.java`
  - `@ConditionalOnProperty(name = "summary.scheduler.enabled", havingValue = "true", matchIfMissing = false)`
  - `@Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")`
  - 전체 식당 → 재생성 대상 필터 → generated_at ASC (NULL 최우선) → 상위 10건 순차 처리
  - 개별 식당 예외 catch & 로깅 (한 건 실패가 전체를 중단시키지 않음)

### 설정 파일 수정

**`application.yml`** 추가:
```yaml
anthropic:
  api-key: ${ANTHROPIC_API_KEY:}
summary:
  scheduler:
    enabled: false
  model-version: claude-sonnet-4-6
  prompt-version: v1.0.0
```

**`application-prod.yml`** 추가:
```yaml
anthropic:
  api-key: ${ANTHROPIC_API_KEY}
summary:
  scheduler:
    enabled: true
```

**`application-test.yml`** 추가:
```yaml
anthropic:
  api-key: test-key
summary:
  scheduler:
    enabled: false
```

**`ErrorCode.java`** 추가:
```java
CLAUDE_API_ERROR(HttpStatus.BAD_GATEWAY, "AI001", "AI 요약 생성 중 오류가 발생했습니다."),
```

**`.env.example`** 추가: `ANTHROPIC_API_KEY=`

---

## 3. 구현 단계 (FE)

### `frontend/src/api/restaurant.js` — 함수 추가
```js
export function fetchReviewSummary(restaurantId) {
  return client.get(`/restaurants/${restaurantId}/review-summary`)
}
```

### `frontend/src/components/review/ReviewSummaryCard.vue` — 신규
- `restaurantId` prop → onMounted에서 API 호출
- `status === "available"`: 카테고리별 태그 카드 렌더링
  - 9개 카테고리를 순회하며 `null`이 아닌 항목만 표시
  - 각 카테고리: 라벨 + 태그 칩 목록 (evidenceReviewIds.length로 "N명 언급" 표시)
  - 카테고리 표시 순서: 대표메뉴 → 맛 → 분위기 → 서비스 → 가성비 → 양 → 방문목적 → 동반인 → 주차
  - 카테고리 라벨 매핑: `signatureMenu` → "대표 메뉴", `taste` → "맛", `atmosphere` → "분위기" 등
  - "AI가 생성한 요약입니다" 면책 문구
- `status === "insufficient"`: "리뷰가 부족하여 요약을 제공할 수 없습니다" 문구
- 기존 `.card` CSS 클래스 + `tokens.css` 디자인 토큰 활용

### `frontend/src/views/RestaurantDetailView.vue` — 수정
- `ReviewSummaryCard` import
- `restaurant-info` 섹션과 `reviews-section` 사이에 배치

---

## 4. 파일 매니페스트

### 신규 파일 (BE: 14개)
1. `backend/src/main/resources/db/migration/V2__add_restaurant_review_summary.sql`
2. `backend/src/main/java/.../service/RestaurantReviewSummary.java`
3. `backend/src/main/java/.../service/SummaryJson.java`
4. `backend/src/main/java/.../service/SourceHashCalculator.java`
5. `backend/src/main/java/.../service/ReviewSummaryService.java`
6. `backend/src/main/java/.../service/ReviewSummaryScheduler.java`
7. `backend/src/main/java/.../service/SummaryValidator.java`
8. `backend/src/main/java/.../repository/RestaurantReviewSummaryRepository.java`
9. `backend/src/main/java/.../client/ClaudeClient.java`
10. `backend/src/main/java/.../client/ClaudeRestClient.java`
11. `backend/src/main/java/.../client/FakeClaudeClient.java`
12. `backend/src/main/java/.../client/ReviewForSummary.java`
13. `backend/src/main/java/.../dto/ReviewSummaryResponse.java`
14. `backend/src/main/java/.../config/SchedulingConfig.java`

### 수정 파일 (BE: 6개)
1. `backend/.../repository/ReviewRepository.java` — 쿼리 2개 추가
2. `backend/.../controller/RestaurantController.java` — 엔드포인트 추가
3. `backend/.../common/exception/ErrorCode.java` — CLAUDE_API_ERROR
4. `backend/src/main/resources/application.yml`
5. `backend/src/main/resources/application-prod.yml`
6. `backend/src/test/resources/application-test.yml`

### 신규 파일 (FE: 1개)
1. `frontend/src/components/review/ReviewSummaryCard.vue`

### 수정 파일 (FE: 2개)
1. `frontend/src/api/restaurant.js`
2. `frontend/src/views/RestaurantDetailView.vue`

### 기타
- `.env.example` — ANTHROPIC_API_KEY 추가

---

## 5. 테스트 전략 (TDD, Fixture+AAA)

### 테스트 파일
1. `api/summary/SourceHashTest.java` — 해시 계산 검증
2. `api/summary/SummaryValidatorTest.java` — 5종 검증 각각 + passed/degraded/failed 분류
3. `api/restaurant/reviewSummary/GET_specs.java` — 조회 API 4케이스
4. `api/fixture/ReviewSummaryFixture.java` + `FixtureConfiguration` 등록

### 테스트 케이스
- **해시**: 리뷰 추가/수정/삭제 시 변화 감지, 동일 리뷰 → 동일 해시
- **검증기**: 5종 검증 각각 통과/실패, passed/degraded/failed 분류
  - evidenceReviewIds 존재 여부, 비어있지 않음, signatureMenu substring 존재, tag 비공백, tag 길이 15자 이내
- **조회 API**: passed → available, degraded/failed → unavailable, 미생성 → insufficient
- **버전 변경**: model/prompt 버전 불일치 → 재생성 후보 편입
- **FakeClaudeClient**: 테스트 프로파일에서 자동 주입, 9개 카테고리 결정론적 결과

---

## 6. 검증 방법

1. `./gradlew test` — 전체 통합 테스트 그린
2. 로컬에서 수동 LLM smoke test (@Tag("llm") 테스트 1회)
3. 조회 API curl 테스트:
   - 리뷰 3개 이상 식당 → 배치 실행 후 `GET /restaurants/{id}/review-summary` → available
   - 리뷰 부족 식당 → insufficient
4. FE: 식당 상세 페이지에서 요약 카드 렌더링 확인
