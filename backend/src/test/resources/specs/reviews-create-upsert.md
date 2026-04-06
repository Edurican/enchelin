# POST /reviews (식당 upsert + visitNumber 자동 계산)

리뷰 작성 시 식당이 DB에 없으면 스냅샷으로 신규 저장(upsert), visitNumber는 (user, restaurant)당 자동 증가.

## 요청 바디
`{ kakaoApiId, name, category, address, placeUrl, x, y, rating, comment }`

## Scenarios

### Given DB에 없는 kakaoApiId 로 리뷰 작성
- When POST /reviews
- Then 200 OK, restaurants 테이블에 새 row 생성, review 1개 생성, visitNumber=1

### Given 이미 존재하는 kakaoApiId (DB hit) + 클라가 다른 name 전송
- When POST /reviews
- Then 200 OK, restaurant 정보는 DB값 유지(클라 스냅샷 무시), visitNumber=1

### Given 같은 user가 같은 restaurant에 두 번째 리뷰
- When POST /reviews 두 번 호출
- Then 두 번째 리뷰의 visitNumber=2

### Given 같은 user의 첫 리뷰가 DELETED 상태
- When 두 번째 POST /reviews
- Then visitNumber=1 (ACTIVE 만 카운트)

### Given 다른 user가 같은 restaurant에 리뷰
- When POST /reviews
- Then visitNumber는 해당 user 기준 1

### Given kakaoApiId 누락
- When POST /reviews
- Then 400 BAD_REQUEST

### Given 인증 없음
- When POST /reviews
- Then 401 UNAUTHORIZED

### Given rating 범위 초과
- When POST /reviews rating=6
- Then 400 BAD_REQUEST
