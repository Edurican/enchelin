# 마이페이지 & 리뷰 시스템 개선 계획

## Context
GitHub OAuth로 로그인 시 nickname/avatarUrl을 받아오지만 JWT에 포함되지 않아 마이페이지에서 "사용자"로 표시됨. 리뷰 응답의 `userName`이 항상 `null`이라 댓글 작성자를 알 수 없고, 소유권 체크도 실패함. 마이페이지에 사용자 통계, 리뷰 정렬이 없으며, 리뷰 카드에 음식점 이름이 표시되지 않음.

## 근본 원인
1. **JWT에 사용자 정보 누락**: `JwtProvider.createToken(Long userId)`가 subject만 설정 → 프론트엔드 `payload.nickname` 항상 null
2. **리뷰 응답에 userName 미설정**: `ReviewService.toReviewResponse()`가 `null` 하드코딩
3. **마이페이지 기능 부재**: 통계/정렬 미구현, 리뷰에 음식점 이름 미표시

---

## Phase 1: 기반 작업 (의존성 없음)

### 1-1. JWT에 사용자 클레임 추가
**파일**: `backend/.../auth/JwtProvider.java`
- `createToken(User user)` 오버로드 추가
- `.claim("nickname", ...)`, `.claim("avatarUrl", ...)`, `.claim("htmlUrl", ...)` 포함

**파일**: `backend/.../auth/AuthService.java`
- `jwtProvider.createToken(user.getId())` → `jwtProvider.createToken(user)` 변경 (User 객체는 이미 로드됨)

**프론트엔드 변경 없음**: `auth.js`가 이미 `payload.nickname`, `payload.avatarUrl`, `payload.htmlUrl`을 읽고 있음

### 1-2. ErrorCode 추가
**파일**: `backend/.../common/exception/ErrorCode.java`
- `USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다.")`

---

## Phase 2: 리뷰 데이터 보강

### 2-1. UserStatsResponse DTO
**새 파일**: `backend/.../dto/UserStatsResponse.java`
- `int reviewCount`, `double averageRating`

### 2-2. ReviewRepository 쿼리 추가
**파일**: `backend/.../repository/ReviewRepository.java`
- `findByUserIdAndStatusOrderByRatingDescCreatedAtDesc(...)` — 평점순
- `findByUserIdActiveOrderByRestaurantName(...)` — @Query JOIN으로 음식점 이름순
- `findUserStats(Long userId)` — COUNT + AVG 단일 쿼리, `UserStatsProjection` 인터페이스

### 2-3. ReviewService 수정
**파일**: `backend/.../service/ReviewService.java`
- `UserRepository` 주입
- `toReviewResponse` 확장: `Map<Long, String> userNames` 파라미터 추가, `null` → `userNames.getOrDefault(...)`
- `getRestaurantReview()`: userName 배치 조회 추가
- `getUserReview()`: sort 파라미터 추가 (latest/name/rating), userName 배치 조회
- `updateReview()`: 단건 유저 조회 후 응답에 포함
- `getUserStats(Long userId)` 메서드 추가

### 2-4. ReviewController 수정
**파일**: `backend/.../controller/ReviewController.java`
- `GET /users/{userId}/reviews`에 `sort` 파라미터 추가 (기본값 "latest")
- `GET /users/{userId}/stats` 엔드포인트 추가

---

## Phase 3: 프론트엔드 (Phase 1~2 의존)

### 3-1. API 함수 수정
**파일**: `frontend/src/api/review.js`
- `fetchUserReviews`에 `sort` 파라미터 추가
- `fetchUserStats(userId)` 추가

### 3-2. ReviewCard 수정
**파일**: `frontend/src/components/review/ReviewCard.vue`
- 헤더에 음식점 이름 표시 (`review.restaurantName` → RestaurantDetail 링크)

### 3-3. MyPageView 수정
**파일**: `frontend/src/views/MyPageView.vue`
- **프로필 영역**: avatar + nickname 표시 (JWT 수정으로 자동 해결)
- **통계 카드 추가**: 리뷰 개수 + 평균 별점 (fetchUserStats 호출)
- **정렬 드롭다운 추가**: 최신순/이름순/평점순 (RestaurantDetailView 패턴 재사용)
- **레이아웃 변경**: 데스크톱 2컬럼 그리드 → 단일 컬럼 (CSS `grid-template-columns: repeat(2, 1fr)` 미디어쿼리 제거)

### 3-4. RestaurantDetailView 수정
**파일**: `frontend/src/views/RestaurantDetailView.vue`
- 소유권 체크 변경: `review.userName === auth.user.nickname` → `String(review.userId) === String(auth.user.id)`

---

## 수정 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `backend/.../auth/JwtProvider.java` | `createToken(User)` 오버로드 추가 |
| `backend/.../auth/AuthService.java` | createToken 호출 변경 |
| `backend/.../common/exception/ErrorCode.java` | USER_NOT_FOUND 추가 |
| `backend/.../dto/UserStatsResponse.java` | **새 파일** — 사용자 통계 DTO |
| `backend/.../repository/ReviewRepository.java` | 정렬 쿼리 + 통계 쿼리 추가 |
| `backend/.../service/ReviewService.java` | userName 배치 조회, 정렬, 통계 |
| `backend/.../controller/ReviewController.java` | 통계 엔드포인트, sort 파라미터 |
| `frontend/src/api/review.js` | sort 파라미터, 통계 API 추가 |
| `frontend/src/components/review/ReviewCard.vue` | 음식점 이름 표시 |
| `frontend/src/views/MyPageView.vue` | 통계, 정렬, 단일컬럼 |
| `frontend/src/views/RestaurantDetailView.vue` | userId 소유권 체크 |

## 재사용 패턴
- **배치 조회**: `ReviewService.getUserReview`의 restaurantNames 패턴 → userName에 동일 적용
- **프로젝션 인터페이스**: `RestaurantReviewStats` 패턴 → `UserStatsProjection`
- **정렬 switch**: `getRestaurantReview`의 `switch(sort)` → `getUserReview`에 동일 적용
- **정렬 UI**: `RestaurantDetailView`의 `<select v-model="sort">` → MyPageView에 복제

## 검증 방법
1. `./gradlew build` 성공 + 기존 테스트 통과
2. 로그인 후 JWT 디코딩하여 nickname/avatarUrl/htmlUrl 클레임 확인
3. 마이페이지: GitHub 닉네임 + 아바타 + 통계(리뷰 수, 평균 별점) 표시 확인
4. 마이페이지: 리뷰에 음식점 이름 표시 확인
5. 마이페이지: 최신순/이름순/평점순 정렬 동작 확인
6. 마이페이지: 리뷰 단일 컬럼 레이아웃 확인
7. 식당 상세: 리뷰 작성자 이름 표시 + 본인 리뷰 수정/삭제 버튼 확인