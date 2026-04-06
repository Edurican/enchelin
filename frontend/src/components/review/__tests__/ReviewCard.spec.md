# ReviewCard — visitNumber 뱃지 명세

## Feature: 방문 회차 뱃지 표시

### Scenario 1: visitNumber가 있을 때 뱃지 렌더
- **Given**: review 객체에 `visitNumber: 3` 포함
- **When**: ReviewCard 렌더
- **Then**: `.visit-badge` 요소가 존재하며 '3번째 방문' 텍스트를 포함한다

### Scenario 2: visitNumber가 1일 때
- **Given**: review 객체에 `visitNumber: 1` 포함
- **When**: ReviewCard 렌더
- **Then**: '1번째 방문' 텍스트가 표시된다

### Scenario 3: visitNumber가 없거나 falsy일 때 뱃지 미렌더
- **Given**: review 객체에 `visitNumber: 0` 또는 `visitNumber` 필드 없음
- **When**: ReviewCard 렌더
- **Then**: `.visit-badge` 요소가 존재하지 않는다

### Scenario 4: isOwner=true 시 수정/삭제 버튼 표시
- **Given**: isOwner prop = true
- **When**: ReviewCard 렌더
- **Then**: '수정', '삭제' 버튼이 모두 표시된다

### Scenario 5: isOwner=false 시 수정/삭제 버튼 미표시
- **Given**: isOwner prop = false
- **When**: ReviewCard 렌더
- **Then**: '수정', '삭제' 버튼이 존재하지 않는다

### Scenario 6: 날짜 포맷
- **Given**: review.createdAt = '2024-03-15T12:00:00'
- **When**: ReviewCard 렌더
- **Then**: '2024.03.15' 형식으로 표시된다
