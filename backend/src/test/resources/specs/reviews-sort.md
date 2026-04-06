# GET /restaurants/{id}/reviews?sort=...

리뷰 정렬 옵션.

## 정렬 값
- `latest` (기본) — createdAt DESC
- `rating_desc` — rating DESC, createdAt DESC
- `rating_asc` — rating ASC, createdAt DESC

## Scenarios

### Given 리뷰 3개 (rating 5, 1, 3)
- When sort=rating_desc
- Then 5, 3, 1 순

### Given 리뷰 3개
- When sort=rating_asc
- Then 1, 3, 5 순

### Given sort 미지정
- When 호출
- Then latest (createdAt DESC)

### Given 잘못된 sort 값
- When sort=invalid
- Then 400 BAD_REQUEST
