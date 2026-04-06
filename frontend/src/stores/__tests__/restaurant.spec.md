# 테스트 명세: useRestaurantStore

## loadByBounds

### Given: bounds 파라미터 + API 정상 응답 (배열)
When: `loadByBounds(sw, ne)` 호출
Then: `mapRestaurants`에 응답 배열 저장, `loading=false`, `error=null`

### Given: bounds 파라미터 + API 정상 응답 (`{ restaurants, hasMore }` 형태)
When: `loadByBounds(sw, ne)` 호출
Then: `mapRestaurants`에 `restaurants` 저장, `hasMore=true`

### Given: API 호출 진행 중
When: `loadByBounds` 호출 직후
Then: `loading=true`

### Given: API 실패
When: `loadByBounds(sw, ne)` 호출
Then: `error`에 에러 객체 저장, `mapRestaurants=[]`

---

## searchKakao

### Given: query + center
When: `searchKakao(query, center)` 호출
Then: API에 query와 center 전달, `searchResults`에 응답 저장, `searchLoading=false`

### Given: API 실패
When: `searchKakao(query)` 호출
Then: `searchResults=[]`

### Given: center 없음
When: `searchKakao(query)` 호출 (center 생략)
Then: API에 center=null 전달, 정상 동작

---

## clearSearch

### Given: searchResults에 항목 존재
When: `clearSearch()` 호출
Then: `searchResults=[]`

---

## selectRestaurant / clearSelected

### Given: 식당 객체
When: `selectRestaurant(restaurant)` → `clearSelected()`
Then: selected에 식당 저장 후 null로 초기화
