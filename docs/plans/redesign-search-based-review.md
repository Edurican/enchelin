# Enchelin 핵심 플로우 재설계 — 검색 기반 리뷰 & 리뷰된 식당 지도

## Context

현재 구현은 **테스트 목적의 임시 구조**이다. `RestaurantService.saveAndGetRestaurantsAround()`는 사용자 위치 주변 1km 반경에서 14개 카테고리 키워드(한식/양식/…/베이커리)를 순차적으로 Kakao Local API에 긁어 수백 개의 식당을 DB에 저장하고, 지도에는 그 전부를 마커로 표시한다. 이는 “동네 식당 탐색기”에 가깝다.

프로젝트의 실제 비전은 다르다:

1. 사용자는 **카카오맵 검색**으로 특정 음식점을 찾는다.
2. 그 음식점에 **리뷰를 남기면** DB에 식당이 등록된다.
3. 지도에는 **리뷰가 존재하는 식당(전체 사용자 기준)만** 마커로 표시된다.
4. 지도는 **로그인 후에만** 볼 수 있으며, **현재 화면(bounds) 내**에 있는 DB 식당만 마커로 렌더링된다.
5. 마커 클릭 → 해당 식당의 리뷰를 볼 수 있다.

즉 지도는 “주변 탐색기”가 아니라 **“리뷰된 식당 모음 지도”** 이고, 식당은 Kakao 검색을 경유해 **리뷰가 남겨질 때만 DB에 upsert** 된다.

---

## 변경 요약

| 영역 | 기존 | 변경 |
|---|---|---|
| 지도 초기 데이터 | 주변 1km 14개 카테고리 자동 크롤링 | 현재 viewport(bounds) 내 **리뷰 있는** DB 식당만 조회 |
| 식당 등록 시점 | 주변 검색 시 일괄 저장 | **리뷰 작성 시점**에 해당 식당만 upsert |
| 검색 UX | 없음 (위치 기반만) | 지도 상단 검색창 → Kakao Local Keyword Search |
| 지도 접근 권한 | 누구나 | **로그인 필수** |
| `/restaurant/nearby` | 메인 API | **삭제 / 교체** |

---

## 백엔드 변경

### 1. 새 엔드포인트: `GET /restaurants` (bounds 조회)

**용도**: 지도 viewport 안에 있는 “리뷰가 1개 이상 존재하는” 식당 목록 반환.

```
GET /restaurants?swLng=&swLat=&neLng=&neLat=
```

- 응답: `List<RestaurantResponse>` (id, kakaoApiId, name, category, address, x, y, placeUrl, reviewCount, avgRating)
- 조건:
  - `x BETWEEN swLng AND neLng`
  - `y BETWEEN swLat AND neLat`
  - `EXISTS (SELECT 1 FROM review r WHERE r.restaurant_id = restaurant.id AND r.status = 'ACTIVE')`
- viewport가 너무 크면 상한(예: 200개) + 정렬(최근 리뷰순)
- **재사용**: 기존 `RestaurantRepository` 확장 — `@Query`로 JPQL 작성. 기존 `Restaurant` 엔티티 그대로 사용.

`RestaurantRepository`에 추가:

```java
@Query("""
  SELECT r FROM Restaurant r
  WHERE r.x BETWEEN :swLng AND :neLng
    AND r.y BETWEEN :swLat AND :neLat
    AND EXISTS (SELECT 1 FROM Review rv
                WHERE rv.restaurantId = r.id AND rv.status = com.edurican.enchelinbe.common.Status.ACTIVE)
""")
List<Restaurant> findReviewedInBounds(...);
```

### 2. 새 엔드포인트: `GET /restaurants/search` (Kakao keyword search 프록시)

**용도**: 프론트 검색창에 입력한 키워드를 Kakao Local Keyword Search API로 전달.

```
GET /restaurants/search?query=&x=&y=&page=
```

- 응답: Kakao `Document` 목록을 얇게 매핑한 `RestaurantSearchResponse` (kakaoApiId, name, category, address, x, y, placeUrl, distance)
- x/y는 선택 (지도 중심 좌표 전달 시 거리 정렬)
- **재사용**: 기존 `KakaoApiService` (또는 `RestaurantService` 안의 Kakao WebClient 호출부) 분리/재활용. 14-키워드 루프 로직은 **제거**하고 단일 키워드 호출만 남긴다.

### 3. `POST /reviews` 수정 — 리뷰 작성 시 식당 upsert

**현재**: 프론트가 `restaurantId`(DB id)를 요구. DB에 없으면 실패.
**변경**: `kakaoApiId`와 식당 스냅샷(name, category, address, x, y, placeUrl)을 함께 받도록 DTO 확장. 서비스에서 `findByKakaoApiId(...)` → 없으면 신규 저장 후 해당 id 사용.

```java
public record CreateReviewRequest(
    String kakaoApiId,     // 필수
    String name,           // Kakao 원본 스냅샷
    String category,
    String address,
    String placeUrl,
    Double x,
    Double y,
    @Min(0) @Max(5) Integer rating,
    @NotBlank @Size(max = 100) String comment
) {}
```

- `ReviewService.createReview()`에서 upsert 로직 추가.
- 기존 `RestaurantRepository.findByKakaoApiId()` 재활용.
- **이점**: 프론트가 Kakao 검색 결과를 그대로 들고 리뷰 폼으로 진입할 수 있고, DB에는 **리뷰된 식당만** 존재하는 불변식이 유지된다.

### 4. 삭제 / 축소: `GET /restaurant/nearby`

- `RestaurentController` (오타 포함 클래스명도 차후 정정 대상)에서 제거.
- `RestaurantService.saveAndGetRestaurantsAround()` 및 14개 카테고리 키워드 루프 삭제.
- Kakao API 호출 공통부만 `KakaoLocalClient`로 남김(재사용).

### 5. `GET /restaurants/{id}` (단건 조회) — **신규 추가**

- 프론트 `RestaurantDetailView`가 이미 `fetchRestaurantDetail`을 호출하지만 백엔드 미구현 상태. 이 재설계와 함께 추가.
- 응답: name/category/address/x/y/placeUrl + reviewCount + avgRating.

### 영향 파일 (백엔드)

- `backend/src/main/java/.../controller/RestaurentController.java` — `/restaurant/nearby` 제거, 새 엔드포인트 3개 추가 (`GET /restaurants`, `GET /restaurants/search`, `GET /restaurants/{id}`)
- `backend/src/main/java/.../service/RestaurantService.java` — 크롤링 로직 제거, 검색/bounds 조회/단건 조회 메서드 추가
- `backend/src/main/java/.../repository/RestaurantRepository.java` — `findReviewedInBounds(...)` 추가
- `backend/src/main/java/.../controller/ReviewController.java` + `ReviewService.java` + `CreateReviewRequest.java` — upsert 지원
- (신규 혹은 기존) `KakaoLocalClient` — 단일 키워드/카테고리 검색 공통화

---

## 프론트엔드 변경

### 1. 라우터 — 지도 로그인 가드

`frontend/src/router/index.js`에서 `/map` 라우트에 `meta: { requiresAuth: true }` 추가. 기존 `beforeEach` 가드가 이미 `requiresAuth` 분기를 지원하므로 재사용.

### 2. `MapView.vue` — 검색창 + bounds 기반 조회

- 상단 검색창 컴포넌트 `SearchBar.vue` (신규) 추가.
- `onBoundsChanged`에서 x/y(center)만 받지 말고 **south-west / north-east** 코너도 함께 받도록 `KakaoMap.vue` 수정:
  - Kakao SDK: `map.getBounds()` → `sw = bounds.getSouthWest()`, `ne = bounds.getNorthEast()`
  - emit payload: `{ center: {x,y}, sw: {lat,lng}, ne: {lat,lng} }`
- bounds 변경 시 `restaurantStore.loadByBounds(sw, ne)` 디바운스(300ms) 호출 → 리뷰된 식당 마커만 표시.
- “이 위치에서 검색” 버튼은 **제거** (bounds 변화 시 자동 재조회로 대체).
- 검색창 제출 → `restaurantStore.searchKakao(query, center)` → 결과 드롭다운/리스트 표시 → 항목 선택 시:
  - 해당 좌표로 지도 이동 + 임시 마커 하이라이트
  - “리뷰 작성” 버튼 → `/reviews/new?kakaoApiId=...&name=...&x=...&y=...` 로 네비게이션 (스냅샷을 쿼리로 전달)

### 3. `ReviewFormView.vue` — 생성 경로 확장

- 현재: `restaurantId` 쿼리로만 진입 가능.
- 변경: 쿼리로 `kakaoApiId`, `name`, `category`, `address`, `x`, `y`, `placeUrl`을 받아 바로 리뷰 폼 렌더.
- 제출 시 `createReview()` API에 스냅샷과 함께 전달 → 백엔드 upsert.
- 편집 모드는 기존대로 `reviewId`, `restaurantId` 기반 유지.

### 4. `restaurant` store 정리

- 제거: `loadNearby(x, y)` (또는 내부 구현을 `loadByBounds`로 교체)
- 신규:
  - `loadByBounds(sw, ne)` → `GET /restaurants?swLng=&swLat=&neLng=&neLat=`
  - `searchKakao(query, center?)` → `GET /restaurants/search?query=...`
- `nearbyList` → `mapRestaurants` (리뷰된 DB 식당) / `searchResults` (Kakao 결과) 분리.

### 5. `api/restaurant.js`

- `fetchNearbyRestaurants` 제거.
- `fetchRestaurantsInBounds(sw, ne)`, `searchRestaurants(query, center)` 추가.
- 기존 `fetchRestaurantDetail(id)`는 이제 실제 백엔드 엔드포인트가 생기므로 그대로 사용 가능.

### 영향 파일 (프론트)

- `frontend/src/router/index.js` — `/map` 라우트 `requiresAuth`
- `frontend/src/views/MapView.vue` — 로직 대폭 수정
- `frontend/src/components/map/KakaoMap.vue` — `bounds-changed` 이벤트에 sw/ne 포함
- `frontend/src/components/map/SearchBar.vue` — **신규**
- `frontend/src/components/map/SearchResults.vue` — **신규** (옵션, 드롭다운)
- `frontend/src/views/ReviewFormView.vue` — kakaoApiId 경유 생성 경로 지원
- `frontend/src/stores/restaurant.js` — 메서드 재설계
- `frontend/src/api/restaurant.js` — 엔드포인트 교체

---

## 단계별 실행 순서 (권장)

1. **백엔드 먼저**: `GET /restaurants` (bounds) + `GET /restaurants/search` + `GET /restaurants/{id}` + `POST /reviews` upsert
2. **프론트 API/스토어 교체**: `api/restaurant.js`, `stores/restaurant.js`
3. **KakaoMap.vue** bounds sw/ne emit 추가
4. **MapView.vue** 로그인 가드 + bounds 조회 + 검색창 통합
5. **ReviewFormView.vue** kakaoApiId 생성 경로 지원
6. **오래된 크롤링 API/로직 제거** (`/restaurant/nearby`, 14-키워드 루프)

이렇게 순서를 잡으면 중간 단계에서도 기존 코드가 컴파일/동작 가능하다.

---

## Verification

- **백엔드**
  - `./gradlew bootRun` 실행 후:
    - `GET /restaurants?swLng=126.9&swLat=37.4&neLng=127.1&neLat=37.6` → DB에 리뷰된 식당만 나오는지 (seed 리뷰 1~2건 넣고 확인)
    - `GET /restaurants/search?query=스타벅스 강남` → Kakao 결과 매핑 확인
    - `POST /reviews`에 DB에 없던 `kakaoApiId` 전달 → 201 + restaurant row 생성 확인
    - 이후 `GET /restaurants/{id}` 로 해당 식당 단건 조회
- **프론트**
  - 비로그인 상태에서 `/map` 접근 → `/login`으로 리다이렉트
  - 로그인 후 `/map` 진입 → 빈 지도 또는 seed 마커 표시 (리뷰된 식당만)
  - 검색창에 “스타벅스” 입력 → Kakao 결과 표시 → 항목 선택 → 리뷰 작성 폼 진입
  - 리뷰 작성 후 지도로 돌아가서 해당 좌표 근처로 이동하면 새 마커가 표시되는지
  - 마커 클릭 → 식당 상세 → 리뷰 리스트에 방금 쓴 리뷰 표시
  - 지도를 크게 드래그 → bounds가 바뀌면 마커가 재조회되는지 (debounce)

---

## 반드시 함께 처리할 항목 (이전 “주의 사항”에서 승격)

### A. Kakao Local API 호출 보호

- `GET /restaurants/search`는 **백엔드 프록시**로만 호출. `KAKAO_API_KEY` (REST 키)는 BE `.env`에서만 사용, 프론트에 절대 노출 금지. `VITE_KAKAO_JS_KEY`(JS 키)는 지도 SDK 용도로만.
- **프론트 디바운스**: 검색창 입력 시 300ms debounce + 최소 2자 이상일 때만 요청. bounds 변경 시에도 300ms debounce로 `/restaurants` 호출.
- **bounds 조회는 DB-only**: 절대 Kakao API로 fan-out하지 않음 (기존 크롤링 제거의 핵심 이유).
- **viewport 상한**: BE에서 `LIMIT 200` + `hasMore` 플래그 응답. 프론트는 zoom-out 과다 시 “더 확대해주세요” 안내.
- **서버 측 간이 rate limit** (MVP 범위에서 단순 구현): Spring `Bucket4j` 또는 `ConcurrentHashMap<userId, lastCalledAt>` 수준으로 검색 엔드포인트에 초당 5회 상한. 과하면 생략 가능.

### B. 스냅샷 위조 방지 (`POST /reviews`)

- 클라이언트가 보내는 식당 필드는 **신뢰 가능한 필드와 그렇지 않은 필드를 분리**한다:
  - `kakaoApiId` → 서버가 기준으로 삼는 고정 식별자
  - `name, category, address, x, y, placeUrl` → 스냅샷
- **upsert 정책**:
  1. `findByKakaoApiId(kakaoApiId)` 히트 → **클라 스냅샷 무시**, DB 값 그대로 사용 (변경은 관리자/별도 동기화 job에 위임).
  2. 미스 → 신규 저장 시에만 클라 스냅샷 사용. **옵션**: 서버가 `KakaoLocalClient.findById(kakaoApiId)` 로 한 번 재검증한 뒤 저장 (MVP에서는 옵션, 환경변수 `KAKAO_VERIFY_ON_CREATE=true`로 토글).
- DTO 검증: `@NotBlank` on `kakaoApiId`, 좌표/카테고리 필드에 합리적 범위 검증.

### C. 리뷰 소유권 검증 (기존 보안 결함 해결)

- `PUT /reviews/{id}`, `DELETE /reviews/{id}` 에서 `review.userId == authenticatedUser.id` 검증. 불일치 시 `403 FORBIDDEN`.
- `ReviewService.updateReview / deleteReview` 시그니처에 `Long currentUserId` 추가. 기존 `@AuthenticatedUser` 리졸버 재사용.

### D. (user, restaurant) 다회 방문 리뷰 + 방문 회차 자동 계산

- **정책**: 1인 1식당에 **여러 리뷰 허용**. 같은 식당을 다시 방문하면 새 리뷰를 작성.
- `Review` 엔티티에 `visitNumber INTEGER NOT NULL` 컬럼 추가.
- **자동 계산**: 리뷰 생성 시 서버가 `SELECT COALESCE(MAX(visit_number), 0) + 1 FROM review WHERE user_id = ? AND restaurant_id = ? AND status = 'ACTIVE'` 로 회차 산정.
  - 동시성: `@Transactional(isolation = SERIALIZABLE)` 또는 DB-level unique constraint `UNIQUE (user_id, restaurant_id, visit_number)` 로 레이스 방지.
- **응답 DTO**: `ReviewResponse`에 `visitNumber` 포함. 프론트에서 "3번째 방문" 뱃지로 표시.
- **소프트 삭제 시**: `status=DELETED` 된 리뷰는 회차 계산에서 제외 → 삭제 후 재작성 시 동일 번호 재사용 가능 (unique constraint와 충돌하므로 partial unique: `UNIQUE (user_id, restaurant_id, visit_number) WHERE status='ACTIVE'`).
- **수정 시**: `visitNumber`는 변경 불가 (immutable).

### E. DB 인덱스 및 성능 — PostGIS 적극 활용

- **PostGIS 확장 활성화**: `CREATE EXTENSION IF NOT EXISTS postgis;` (Railway Postgres는 PostGIS 지원). 로컬 H2 프로파일에서는 단순 BTREE 폴백 또는 PostGIS 개발용 Docker 이미지 사용.
- **Restaurant 엔티티 지오메트리 컬럼**:
  - 신규 컬럼 `location GEOGRAPHY(POINT, 4326)` 추가 (위도/경도 → EPSG:4326).
  - 기존 `x`, `y` (Double) 는 호환용으로 유지하되, 저장 시 `location = ST_SetSRID(ST_MakePoint(x, y), 4326)::geography` 로 동기화 (JPA `@PrePersist` / `@PreUpdate`).
  - Hibernate Spatial 의존성 추가: `org.hibernate.orm:hibernate-spatial`.
- **GiST 인덱스**: `CREATE INDEX idx_restaurant_location_gist ON restaurant USING GIST (location);`
- **bounds 쿼리 재작성**: JPQL 대신 native query로 PostGIS 함수 사용.
  ```sql
  SELECT r.* FROM restaurant r
  WHERE ST_Intersects(
      r.location,
      ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326)::geography
  )
  AND EXISTS (SELECT 1 FROM review rv
              WHERE rv.restaurant_id = r.id AND rv.status = 'ACTIVE')
  LIMIT 200;
  ```
- **거리 정렬**: Kakao 검색 결과 프록시에서도 지도 중심으로부터 거리 정렬 시 `ST_Distance(location, ST_MakePoint(:cx, :cy)::geography)` 활용 가능 (단, Kakao 검색 결과는 DB에 없을 수 있으므로 BE 메모리 내 거리 계산으로 병행).
- **장점**: 향후 "반경 N km 내", "가장 가까운 N개" 같은 확장 쿼리도 즉시 지원.
- `review.restaurant_id` 기존 인덱스 그대로 사용.

### F. 마커 스타일 / 응답 payload 설계

- `/restaurants` bounds 응답에 `reviewCount`, `avgRating` 포함 → 프론트가 마커 크기/색 다르게 렌더 가능.
- `avgRating`은 BE에서 `AVG(rating)` 쿼리로 계산 (한 번의 조인). 프론트 실시간 계산 제거.

### G. 트랜잭션 경계

- 리뷰 작성 시 (restaurant upsert + review insert)는 **단일 `@Transactional`** 안에서 처리. 실패 시 롤백.

### H. 기존 테스트 데이터 정리

- 현재 DB에 `GET /restaurant/nearby`로 쌓인 “리뷰 없는 식당” 수백 건이 남아있을 수 있음.
- `/restaurants` bounds 쿼리가 `EXISTS review` 조건으로 **자동 배제**하므로 기능상 문제는 없지만, 깨끗한 상태를 위해 로컬 DB는 drop & recreate 권장. 운영 DB는 별도 cleanup SQL (`DELETE FROM restaurant WHERE NOT EXISTS (SELECT 1 FROM review r WHERE r.restaurant_id = restaurant.id)`).

### I. 지도 초기 중심 좌표

- 로그인 후 `/map` 진입 시 중심 좌표 결정 우선순위:
  1. 사용자 geolocation (허용 시, 5초 타임아웃)
  2. 내가 마지막으로 남긴 리뷰의 식당 좌표 (추가 API 필요: `GET /users/me/reviews?limit=1`) — 옵션
  3. 강남역 기본값 (37.4979, 127.0276)
- MVP는 1 → 3만 구현하고, 2는 별도 이슈로 백로그.

### J. `RestaurentController` 오타 정정

- 클래스명/파일명을 `RestaurantController`로 rename.
- 경로는 이미 `/restaurants`로 재설계되므로 URL 호환성 이슈 없음 (기존 `/restaurant/nearby` 자체가 제거됨).
- **별도 리팩터 커밋**으로 분리 (같은 PR 안에서 Commit 하나로).

---

## 확정된 추가 작업 (이번 범위 포함)

- **K. 마커 클러스터링 (즉시 도입)**: Kakao Maps SDK `MarkerClusterer`. SDK 로드 URL에 `&libraries=clusterer` 추가. `KakaoMap.vue`에서 마커를 `MarkerClusterer`에 위임하여 자동 그룹화, 줌인 시 분해. 이번 마이그레이션에 함께 구현.
- **L. 검색 결과 거리순 고정 정렬**: `/restaurants/search` 응답을 **항상 지도 center(또는 사용자 위치) 기준 거리순**으로 정렬. 토글 UI 없음. center 좌표가 없으면 Kakao 기본 정확도순 폴백. PostGIS 좌표가 있는 경우 `ST_Distance`, 아닌 경우 Haversine 공식으로 메모리 계산.
- **M. Empty State UX**: `mapRestaurants.length === 0 && !loading` 일 때 지도 위 오버레이 배너 — “아직 리뷰된 식당이 없어요. 상단 검색창에서 식당을 찾아 첫 리뷰를 남겨보세요.” 버튼 클릭 시 검색창 포커스.
- **N. `.env.example` 주석 보강**: `KAKAO_API_KEY`(REST — BE 전용)와 `VITE_KAKAO_JS_KEY`(JavaScript — FE 지도 SDK 전용) 역할 구분 명시. 키 발급 위치(Kakao Developers 앱 > 일반 > 앱 키) 링크 주석.
- **O. 리뷰 정렬 옵션**: `GET /restaurants/{id}/reviews?sort=latest|rating_desc|rating_asc` (기본 `latest`). 프론트 식당 상세 페이지 상단에 드롭다운 추가.
- **P. 검색 프록시 응답 필드 최소화 (재확인)**:
  - 반드시 `RestaurantSearchResponse` record로 매핑: `kakaoApiId, name, category, address, x, y, placeUrl, distance` 만 포함.
  - Kakao 원본 JSON(`phone`, `road_address_name`, `category_group_code` 등) 을 그대로 전달하지 **않음**.
  - 섹션 B의 스냅샷 정책과 일관: 이 응답 필드가 곧 `POST /reviews`에 그대로 재전송되는 스냅샷의 원천이므로, 검색 응답에서 **허용되지 않는 필드는 애초에 내려가지 않도록** 고정.

## 보류 / 후속 이슈

- **Kakao 검색 결과 캐시 (Caffeine)**: MVP 트래픽 수준에서 과잉. 후속 이슈로 백로그.
- **접근성(ARIA/키보드 네비)**: 부트캠프 MVP 대상상 우선순위 낮음. 후속 이슈.

---

## 이슈 분할 & 파일 소유권 매트릭스 (병렬 작업 계약)

본 재설계는 **4개 이슈**로 분할하여 서로 다른 에이전트/작업자가 **파일 충돌 없이 병렬**로 진행한다. 아래 파일 소유권은 **엄격히 지킬 것** — 타 이슈 소유 파일을 수정해야 한다면 먼저 담당자에게 알려 계약을 업데이트한다.

### Issue 1 — [BE] 검색 기반 재설계: API/도메인/PostGIS
**소유 파일**:
- `backend/**/*.java`
- `backend/build.gradle`
- `backend/src/main/resources/application*.yml`
- `backend/src/main/resources/db/**` (마이그레이션 SQL)

**포함 항목**: A, B, C, D(서버), E, F, G, H, J + 새 엔드포인트 3개 + `POST /reviews` upsert + Review.visitNumber + Restaurant.location + PostGIS 인덱스/쿼리 + 오래된 크롤링 로직 제거.

### Issue 2 — [FE] 지도 뷰 재설계: 검색창 + bounds + 클러스터링
**소유 파일**:
- `frontend/src/router/index.js`
- `frontend/src/views/MapView.vue`
- `frontend/src/components/map/**` (KakaoMap.vue, 신규 SearchBar.vue, 신규 SearchResults.vue)
- `frontend/src/stores/restaurant.js`
- `frontend/src/api/restaurant.js`

**포함 항목**: 라우터 로그인 가드, bounds 기반 조회, Kakao 검색 프록시 호출, 마커 클러스터링(K), Empty state(M), 초기 중심 좌표(I), 거리순 정렬(L), 디바운스. **지도/검색 관련 화면의 반응형 구현도 본 이슈 범위**.

### Issue 3 — [FE] 리뷰 폼/상세: kakaoApiId 경유 작성 + 정렬 + 방문 회차 UI
**소유 파일**:
- `frontend/src/views/ReviewFormView.vue`
- `frontend/src/views/RestaurantDetailView.vue`
- `frontend/src/views/MyPageView.vue`
- `frontend/src/views/LoginView.vue`
- `frontend/src/views/AuthCallbackView.vue`
- `frontend/src/components/review/**` (ReviewList, ReviewCard, RatingInput, StarRating)
- `frontend/src/api/review.js`

**포함 항목**: ReviewFormView의 `kakaoApiId` 쿼리 경유 생성 경로, `CreateReviewRequest` 스냅샷 필드 전송, `visitNumber` 뱃지 표시, 리뷰 정렬 옵션(O), D의 프론트 대응. **리뷰/상세/마이페이지/로그인 관련 화면의 반응형 구현도 본 이슈 범위**.

### Issue 4 — [common] Shell 반응형 레이어 + 환경변수 문서
**축소된 소유 파일** (Issues 2/3와 충돌 방지를 위해 의도적으로 shell만):
- `frontend/index.html`
- `frontend/src/components/common/AppLayout.vue`
- `frontend/src/components/common/NavBar.vue`
- `frontend/src/components/common/ConfirmDialog.vue`
- `frontend/src/components/common/AppToast.vue`
- `frontend/src/components/common/LoadingSpinner.vue`
- `frontend/src/components/common/ErrorMessage.vue`
- `frontend/src/assets/**` (전역 CSS/토큰)
- `.env.example`

**포함 항목**: viewport meta 태그 확인, 전역 `100dvh` + safe-area 적용, NavBar 모바일 bottom-fixed / 데스크톱 전환 구현, 공통 컴포넌트 반응형 토큰/브레이크포인트 정비, `.env.example` 주석 보강(N). **개별 뷰(MapView/ReviewFormView/RestaurantDetailView 등)의 반응형은 Issues 2/3 범위이며 본 이슈에서 수정하지 않음**.

### 공유 계약 (Cross-issue Contract)

1. **검색 결과 → 리뷰 작성 네비게이션 쿼리 스키마** (Issue 2 ↔ Issue 3):
   ```
   /reviews/new?kakaoApiId=...&name=...&category=...&address=...&x=...&y=...&placeUrl=...
   ```
   이 스키마는 **고정**이며 변경 시 양쪽 이슈에 공지.

2. **`POST /reviews` 요청 바디** (Issue 1 ↔ Issue 3):
   `{ kakaoApiId, name, category, address, placeUrl, x, y, rating, comment }`. 서버는 섹션 B 정책에 따라 DB에 존재하면 스냅샷 무시.

3. **`GET /restaurants` bounds 응답 스키마** (Issue 1 ↔ Issue 2):
   `[{ id, kakaoApiId, name, category, address, x, y, placeUrl, reviewCount, avgRating }]`

4. **`GET /restaurants/search` 응답 스키마** (Issue 1 ↔ Issue 2):
   `[{ kakaoApiId, name, category, address, x, y, placeUrl, distance }]`

5. **`ReviewResponse` 스키마** (Issue 1 ↔ Issue 3):
   `{ reviewId, userId, userName, restaurantId, restaurantName, rating, comment, visitNumber, createdAt }`

### 병렬성 & 머지 순서

- **개발**: 4개 이슈 모두 동시 시작 가능. FE 이슈 2/3는 BE 엔드포인트가 없는 동안 mock 응답으로 개발.
- **머지 순서 권장**: Issue 1 → Issue 2 → Issue 3 → Issue 4 (혹은 Issue 4는 언제든).
- **E2E 스모크 테스트**: 4개 모두 머지된 후 dev 브랜치에서 진행.

---

## Responsive: 모바일 & 웹 양쪽 정상 동작 (필수 제약)

**본 앱은 데스크톱 웹 브라우저와 모바일 웹 브라우저 양쪽에서 모두 정상 동작해야 한다.** 재설계된 모든 화면은 아래 원칙을 지켜 구현한다.

### 전역 레이아웃
- `<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">` — `index.html` 확인 및 필요 시 추가.
- `height: 100dvh` (`100vh` 대신) 로 iOS Safari 주소창 변동 대응. 기존 `AppLayout.vue`가 이미 `100dvh` 사용 중 → 유지.
- **safe-area 패딩**: `env(safe-area-inset-*)` 를 nav bar 상/하단에 반영 (iOS 노치/홈바).
- **하단 NavBar**는 모바일에서 bottom fixed(`position: fixed; bottom: 0`), 데스크톱(≥768px)에서는 상단 또는 사이드로 전환 (기존 `AppLayout`의 media query 확장).

### 지도 화면 (핵심)
- `.kakao-map` 는 `flex: 1` + `width: 100%` 로 항상 남은 공간 채움 (방금 적용한 수정 유지).
- **검색창**: 모바일에서 상단 full-width, sticky. 데스크톱에서 최대 너비 480px, 지도 위 오버레이.
- **검색 결과 드롭다운**: 모바일은 full-width bottom sheet 스타일(선택), 데스크톱은 검색창 하단 드롭다운.
- **마커/InfoWindow 터치 타깃**: 최소 44×44px 히트 영역. InfoWindow는 모바일에서 텍스트 크기 축소 방지 (≥13px).
- **bounds-changed 디바운스**: 모바일 드래그 제스처 중 과도 호출 방지 — 300ms debounce 유지.
- **“현재 위치” 버튼**: 하단 우측 floating, 모바일 safe-area 고려.

### 리뷰 작성 폼
- `ReviewFormView`는 폼 너비를 `max-width: 560px; margin-inline: auto; padding-inline: 16px;` 로 반응형.
- `textarea`는 모바일 키보드 대응 — `autofocus` 지양, 스크롤 점프 방지.
- `RatingInput` 별 아이콘은 터치 타깃 최소 36×36px.

### 상세 페이지 / 마이페이지
- 카드 레이아웃은 single-column(≤ 768px) / two-column(≥ 1024px) 로 전환.
- 이미지(프로필 아바타)는 `max-width: 100%; height: auto;`.

### 검증용 브레이크포인트
- **모바일**: 360 × 640 (Galaxy S), 390 × 844 (iPhone 13)
- **태블릿**: 768 × 1024 (iPad)
- **데스크톱**: 1280 × 800, 1920 × 1080

### Verification에 추가
- Chrome DevTools Device Toolbar에서 위 브레이크포인트로 지도 뷰, 검색, 리뷰 작성, 상세, 마이페이지 전 경로를 수동 확인.
- 실기기(가능 시 iOS Safari, Android Chrome) 로 최소 한 번 스모크 테스트.
