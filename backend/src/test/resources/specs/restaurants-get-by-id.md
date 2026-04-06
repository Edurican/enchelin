# GET /restaurants/{id}

단건 식당 조회.

## 응답 스키마
`{ id, kakaoApiId, name, category, address, x, y, placeUrl, reviewCount, avgRating }`

## Scenarios

### Given 존재하는 식당 + 리뷰 2개 (rating 4, 2)
- When `GET /restaurants/{id}`
- Then 200 OK + reviewCount=2, avgRating=3.0

### Given 존재하지 않는 id
- When `GET /restaurants/99999`
- Then 404 NOT_FOUND

### Given 리뷰가 없는 식당
- When 위 호출
- Then 200 OK + reviewCount=0, avgRating=0.0
