# GET /restaurants (bounds 조회)

지도 viewport(bounds) 안에서 **리뷰가 1개 이상 존재하는** 식당만 반환한다.

## Endpoint
`GET /restaurants?swLng=&swLat=&neLng=&neLat=`

## 응답 스키마
`[{ id, kakaoApiId, name, category, address, x, y, placeUrl, reviewCount, avgRating }]`

## Scenarios

### Given 리뷰가 있는 식당과 없는 식당이 bounds 안에 혼재
- When `GET /restaurants?swLng=126.9&swLat=37.4&neLng=127.1&neLat=37.6`
- Then 200 OK + 리뷰 있는 식당만 응답에 포함

### Given 모든 식당이 bounds 밖
- When 위 호출
- Then 200 OK + 빈 배열

### Given 리뷰 있는 식당이 여러 개이고 평점이 다양
- When 위 호출
- Then 각 식당에 대해 reviewCount, avgRating 정확히 계산

### Given 삭제된 리뷰만 있는 식당
- When 위 호출
- Then 해당 식당은 응답에서 제외 (status=ACTIVE만 카운트)
