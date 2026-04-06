# GET /restaurants/search (Kakao 프록시)

Kakao Local Keyword Search API에 단일 키워드로 위임한다. center 좌표 제공 시 거리순 정렬.

## Endpoint
`GET /restaurants/search?query=&x=&y=&page=`

## 응답 스키마
`[{ kakaoApiId, name, category, address, x, y, placeUrl, distance }]`

## Scenarios

### Given Kakao API가 정상 응답
- When `GET /restaurants/search?query=스타벅스&x=127.0&y=37.5`
- Then 200 OK + 매핑된 식당 목록 + 거리순 정렬

### Given query 누락
- When `GET /restaurants/search`
- Then 400 BAD_REQUEST

### Given query가 1자 미만(빈 문자열)
- When `GET /restaurants/search?query=`
- Then 400 BAD_REQUEST

### Given Kakao 빈 결과
- When 위 호출
- Then 200 OK + 빈 배열

### Given 초당 5회 초과 호출 (rate limit)
- When 같은 IP로 6번째 호출
- Then 429 TOO_MANY_REQUESTS
