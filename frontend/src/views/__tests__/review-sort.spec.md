# RestaurantDetailView — 리뷰 정렬 명세

## Feature: 리뷰 정렬 드롭다운

### Scenario 1: 기본 정렬은 최신순
- **Given**: 식당 상세 페이지 진입
- **When**: 컴포넌트가 마운트됨
- **Then**:
  - `fetchRestaurantReviews(id, 0, 10, 'latest')` 가 호출된다
  - 정렬 드롭다운의 선택값은 `latest` 이다

### Scenario 2: 별점 높은순 선택 시 재조회
- **Given**: 식당 상세 페이지가 로드된 상태
- **When**: 정렬 드롭다운에서 `rating_desc` 선택
- **Then**:
  - `fetchRestaurantReviews(id, 0, 10, 'rating_desc')` 가 호출된다
  - offset이 0으로 리셋된다
  - 기존 리뷰 목록이 새 결과로 교체된다

### Scenario 3: 별점 낮은순 선택 시 재조회
- **Given**: 식당 상세 페이지가 로드된 상태
- **When**: 정렬 드롭다운에서 `rating_asc` 선택
- **Then**:
  - `fetchRestaurantReviews(id, 0, 10, 'rating_asc')` 가 호출된다

### Scenario 4: 정렬 변경 후 더보기
- **Given**: `rating_desc` 로 정렬된 상태, hasNext=true
- **When**: '더 보기' 버튼 클릭
- **Then**:
  - `fetchRestaurantReviews(id, 10, 10, 'rating_desc')` 가 호출된다 (sort 유지)
