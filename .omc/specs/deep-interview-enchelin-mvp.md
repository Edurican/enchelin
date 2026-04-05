# Deep Interview Spec: Enchelin MVP

## Metadata
- Interview ID: enchelin-di-001
- Rounds: 8
- Final Ambiguity Score: 15.6%
- Type: brownfield
- Generated: 2026-04-02
- Threshold: 20%
- Status: PASSED

## Clarity Breakdown
| Dimension | Score | Weight | Weighted |
|-----------|-------|--------|----------|
| Goal Clarity | 0.85 | 0.35 | 0.298 |
| Constraint Clarity | 0.85 | 0.25 | 0.213 |
| Success Criteria | 0.85 | 0.25 | 0.213 |
| Context Clarity | 0.80 | 0.15 | 0.120 |
| **Total Clarity** | | | **0.844** |
| **Ambiguity** | | | **15.6%** |

## Goal
특정 부트캠프 인원을 대상으로 한 식당 리뷰 & 탐색 웹 서비스를 1개월 이내에 MVP로 출시한다. 사용자는 GitHub OAuth로 로그인하고, 카카오 지도에서 주변 식당을 검색하고, 리뷰를 작성/조회하며, 마이페이지에서 자신의 활동을 관리한다.

## Constraints
- **인증**: GitHub OAuth (현재 User 엔티티 활용, 추후 추가 인증 도입 가능)
- **접근 제한**: 특정 부트캠프/인원만 사용 가능
- **배포**: Railway (프론트엔드 Vue + 백엔드 Spring Boot + PostgreSQL 통합)
- **일정**: 1개월 이내 MVP 완성
- **기술 스택**: Spring Boot 3.5 + Vue 3 + PostgreSQL 16 (기존 유지)
- **외부 API**: 카카오 지도/검색 API (기존 연동 유지)
- **개발 인원**: 1인 개발 (추정)

## Non-Goals
- 즐겨찾기, 식당 랭킹, 리뷰 사진 업로드 (1차 출시 범위 아님)
- 네이티브 모바일 앱 (웹 반응형으로 대응)
- 추가 인증 방식 (GitHub OAuth 외, 추후 도입)
- 테스트 커버리지 목표 (TDD는 별도로 프로젝트 시작 전 설정 예정)

## Acceptance Criteria
- [ ] GitHub OAuth 로그인/로그아웃이 정상 동작한다
- [ ] 로그인 후 카카오 지도에서 현재 위치 기반 주변 식당이 표시된다
- [ ] 식당을 선택하면 상세 정보와 리뷰 목록이 표시된다
- [ ] 리뷰 작성 (평점 0-5 + 코멘트)이 정상 동작한다
- [ ] 리뷰 수정/삭제가 정상 동작한다
- [ ] 마이페이지에서 내가 작성한 리뷰 목록을 확인할 수 있다
- [ ] 모바일 기기에서 반응형 UI가 정상 표시된다
- [ ] API 에러 시 사용자 친화적 메시지가 표시된다
- [ ] 로딩 상태가 UI에 표시된다
- [ ] CI/CD 파이프라인이 구성되어 PR 머지 시 자동 배포된다
- [ ] Railway에 프론트+백엔드+DB가 정상 배포된다

## Assumptions Exposed & Resolved
| Assumption | Challenge | Resolution |
|------------|-----------|------------|
| Vercel로 전체 배포 가능 | Vercel은 JVM(Spring Boot)을 지원하지 않음 | Railway로 프론트+백+DB 통합 배포 |
| 기본 기능만으로 MVP 충분 | 품질 기준 없이 출시 가능한가? | 모바일 반응형, 에러/로딩 처리, CI/CD 필수 |
| GitHub OAuth로 접근 제한 가능 | 부트캠프 인원 제한을 어떻게? | 우선 GitHub OAuth 유지, 추후 추가 인증 도입 |

## Technical Context
### 기존 구현 (활용)
- **백엔드 API**: 주변 식당 검색 (`GET /restaurant/nearby`), 리뷰 CRUD 완료
- **엔티티**: Restaurant, Review, User + BaseEntity (JPA)
- **에러 처리**: GlobalExceptionHandler + 커스텀 에러코드
- **페이지네이션**: Slice 기반 offset-limit 구현
- **Docker**: docker-compose로 로컬 개발환경 구성

### 미구현 (개발 필요)
- **GitHub OAuth 플로우**: User 엔티티 존재하나 인증 미연동
- **프론트엔드 UI**: Vue 3 스캐폴드만 존재, 실제 화면 없음
- **카카오 지도 UI**: 프론트엔드에 카카오 지도 연동 필요
- **CI/CD**: GitHub Actions 워크플로우 미구성
- **Railway 배포 설정**: Dockerfile 존재하나 Railway 설정 필요

### 화면 구성 (5개 화면)
1. **로그인 화면** — GitHub OAuth 로그인 버튼
2. **지도 화면 (메인)** — 카카오 지도 + 주변 식당 마커 표시
3. **식당 상세 화면** — 식당 정보 + 리뷰 목록 (페이지네이션)
4. **리뷰 작성 화면** — 평점 선택 + 코멘트 입력
5. **마이페이지** — 내 정보 + 내 리뷰 목록

## Ontology (Key Entities)

| Entity | Type | Fields | Relationships |
|--------|------|--------|---------------|
| User | core domain | id, githubId, email, nickname, avatarUrl, htmlUrl, role | User has many Reviews |
| Restaurant | core domain | id, kakaoApiId, name, category, placeUrl, address, x, y | Restaurant has many Reviews |
| Review | core domain | id, userId, restaurantId, rating, comment, status, createdAt, updatedAt | Review belongs to User, Review belongs to Restaurant |
| Map | supporting | - (UI component, not persisted) | Map displays Restaurants |

## Ontology Convergence

| Round | Entity Count | New | Changed | Stable | Stability Ratio |
|-------|-------------|-----|---------|--------|----------------|
| 1 | 3 | 3 | - | - | N/A |
| 2 | 3 | 0 | 0 | 3 | 100% |
| 3 | 3 | 0 | 0 | 3 | 100% |
| 4 | 3 | 0 | 0 | 3 | 100% |
| 5 | 4 | 1 | 0 | 3 | 75% |
| 6 | 4 | 0 | 0 | 4 | 100% |
| 7 | 4 | 0 | 0 | 4 | 100% |
| 8 | 4 | 0 | 0 | 4 | 100% |

## Interview Transcript
<details>
<summary>Full Q&A (8 rounds)</summary>

### Round 1
**Q:** Enchelin 프로젝트의 최종 목표는 무엇인가요? (포트폴리오 vs 실서비스 vs 팀 프로젝트)
**A:** 실제 서비스이고 배포하는 것을 목표로 함. 단, 사용자는 특정 부트캠프나 인원만 사용할 수 있도록 할 것.
**Ambiguity:** 70.4% (Goal: 0.45, Constraints: 0.15, Criteria: 0.10, Context: 0.50)

### Round 2
**Q:** 사용자 접근 제어 구현 방식은? GitHub OAuth 유지할지?
**A:** 현재는 GitHub OAuth를 유지할 것이며 추후에 추가 인증을 도입할 예정.
**Ambiguity:** 61.0% (Goal: 0.50, Constraints: 0.40, Criteria: 0.10, Context: 0.60)

### Round 3
**Q:** 실서비스 출시 최소 기능 범위는?
**A:** 기본 MVP(로그인 + 식당 검색 + 리뷰 CRUD + 리뷰 조회)에 배포는 버셀로 할 예정.
**Ambiguity:** 40.7% (Goal: 0.70, Constraints: 0.50, Criteria: 0.50, Context: 0.65)

### Round 4
**Q:** Spring Boot 백엔드와 PostgreSQL 배포 전략은?
**A:** Vercel로 백엔드 배포가 불가능함을 알게 되어, Railway를 선택.
**Ambiguity:** 33.7% (Goal: 0.70, Constraints: 0.70, Criteria: 0.55, Context: 0.70)

### Round 5
**Q:** 사용자가 앱에서 보게 될 화면 구성은?
**A:** 로그인 → 지도(주변 식당) → 식당 상세/리뷰 → 리뷰 작성 → 마이페이지 구성 동의.
**Ambiguity:** 27.6% (Goal: 0.85, Constraints: 0.70, Criteria: 0.55, Context: 0.75)

### Round 6
**Q:** MVP 출시 전 충족해야 할 품질 기준은?
**A:** 품질 기준 필요.
**Ambiguity:** 23.9% (Goal: 0.85, Constraints: 0.70, Criteria: 0.70, Context: 0.75)

### Round 7
**Q:** 구체적인 품질 항목 선택
**A:** 모바일 반응형 UI, 에러/로딩 처리, CI/CD 파이프라인. 테스트는 TDD 응용 버전을 추후 설정 예정.
**Ambiguity:** 20.1% (Goal: 0.85, Constraints: 0.70, Criteria: 0.85, Context: 0.75)

### Round 8
**Q:** MVP 완성 일정과 팀 구성은?
**A:** 1개월 이내.
**Ambiguity:** 15.6% (Goal: 0.85, Constraints: 0.85, Criteria: 0.85, Context: 0.80)

</details>
