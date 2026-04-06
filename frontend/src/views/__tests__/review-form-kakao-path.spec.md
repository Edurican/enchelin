# ReviewFormView — kakaoApiId 경유 리뷰 작성 명세

## Feature: 카카오 검색 결과에서 리뷰 작성 폼 진입

### Scenario 1: kakaoApiId 쿼리로 진입 시 식당 미리보기 카드 렌더
- **Given**: `/reviews/new?kakaoApiId=12345&name=스타벅스 강남&category=카페&address=서울 강남구&x=127.0&y=37.5&placeUrl=https://place.map.kakao.com/12345` 로 접근
- **When**: 컴포넌트가 마운트됨
- **Then**:
  - 식당 미리보기 카드(`.restaurant-preview`)가 렌더된다
  - 카드에 `스타벅스 강남` 텍스트가 표시된다
  - 카드에 `카페` 카테고리가 표시된다
  - 카드에 `서울 강남구` 주소가 표시된다

### Scenario 2: kakaoApiId 경유 제출 시 스냅샷 페이로드 전송
- **Given**: kakaoApiId 쿼리로 폼에 진입한 상태
- **When**: 평점(4) + 코멘트 입력 후 제출
- **Then**:
  - `createReview` 가 `{ kakaoApiId: '12345', name: '스타벅스 강남', ..., rating: 4, comment: '...' }` 로 호출된다
  - `restaurantId` 는 전송되지 않는다

### Scenario 3: kakaoApiId 없이 restaurantId 경유 진입 (레거시)
- **Given**: `/reviews/new?restaurantId=99` 로 접근
- **When**: 컴포넌트가 마운트됨
- **Then**:
  - 식당 미리보기 카드가 렌더되지 않는다

### Scenario 4: 수정(Edit) 모드 진입
- **Given**: `/reviews/42/edit?restaurantId=99` 로 접근
- **When**: 컴포넌트 마운트 시 `fetchRestaurantReviews(99, 0, 50)` 호출
- **Then**:
  - reviewId=42 에 해당하는 리뷰의 rating/comment 가 폼에 프리필된다

### Scenario 5: 유효성 검사
- **Given**: 평점 미선택 상태
- **When**: 제출
- **Then**: `errors.rating` 에 '평점을 선택해주세요.' 메시지가 표시된다
