# 엔첼린 배포 전략 (Deployment Strategy)

**작성일:** 2026-04-07
**대상:** 부트캠프 전용 비공개 애플리케이션 (실 사용자 ≤ 100명)
**개발 인원:** 1~2명
**전제:**
- 광고 미부착 (비상업)
- GitHub OAuth 자체 구현 (Spring Security)
- 사진 업로드 추후 도입 예정
- PostGIS 기반 검색 (이슈 #15~#18 재설계 결과)

> 본 문서는 기존 `enchelin-mvp.md` (Railway 가정) 를 부분 대체한다.
> Phase 1 / Step 1.3 의 "Railway" 전제는 본 문서로 갱신되며, 이슈 #23 분해의 기준이 된다.

---

## 1. 결론 (TL;DR)

| 레이어 | 선택 | 비용/월 |
|---|---|---|
| Frontend (Vite SPA) | **Cloudflare Pages** | $0 |
| Backend (Spring Boot) | **Fly.io** (icn / nrt, shared-1x 512MB) | $2~5 |
| Database (Postgres + PostGIS) | **Neon** (Free → 필요 시 Launch $19) | $0~19 |
| Object Storage (사진) | **Cloudflare R2** (egress 무료) | ~$0 |
| Auth | Spring Security + GitHub OAuth (자체) | $0 |
| Error Tracking | **Sentry** Developer Free | $0 |
| Uptime / Cold-start 방지 | UptimeRobot Free (5분 ping) | $0 |
| CI | **GitHub Actions** (테스트·린트만) | $0 |
| DNS / TLS / WAF | **Cloudflare** | 도메인비만 (~$10/년) |

**총 운영 비용: 월 $2~5 부터, 성장 시 ~$45**

핵심 원칙: **운영 시간 ≈ 0**. 기술적 화려함보다 1~2인 팀의 인지 부하 최소화.

---

## 2. 다양한 관점에서의 정리

### 2.1 비용 관점

| 시나리오 | 월 비용 | 비고 |
|---|---|---|
| 풀 무료 (Render Free + Neon Free + CF Pages) | **$0** | Render cold start 30s~1m 감수 |
| 권장 시작 (Fly + Neon Free + CF Pages + R2) | **$2~5** | cold start 거의 없음 |
| 성장 (Fly + Neon Launch + Sentry Team) | ~$45 | 트래픽·DB가 무료 한도 초과 시 |
| 학생 혜택 활용 | $0 (6~12개월) | GitHub Student Pack 크레딧 |

**과금이 발생하는 지점**:
1. **컨테이너 RAM·CPU 시간** (BE가 24h 가동 → 가장 큼)
2. **DB 인스턴스** (Neon Free는 scale-to-zero, 첫 쿼리 1~3s 추가)
3. **이미지 egress** — R2 선택의 결정적 이유 (S3 / Supabase Storage 대비 0원)
4. 빌드 시간·네트워크 in/out — 100명 규모에선 무시 가능

### 2.2 운영 인력 관점 (1~2인)

- **하지 말 것**: Kubernetes, ECS, Terraform, 자체 EC2 + Postgres, 직접 SSH 배포 스크립트
- **위임할 것**: 빌드/배포는 PaaS(Fly), DB 백업은 Neon, TLS·DDoS는 Cloudflare
- **GitHub Actions 역할**: 테스트·린트·보안 스캔만. 배포는 PaaS git 연동에 위임 (이중 책임 회피)
- **롤백**: `fly releases rollback` 한 줄 / Cloudflare Pages는 이전 배포 클릭 한 번
- **Secrets 관리 위치 3곳**: `fly secrets`, Cloudflare Pages env, GitHub Actions secrets — 늘어나면 Doppler 도입 검토

### 2.3 한국 사용자 latency 관점

- BE: Fly `icn` (서울) — Railway/Render(미국·EU) 대비 결정적 우위
- FE: Cloudflare Pages — ICN 엣지 안정
- DB: Neon은 AWS `ap-northeast-2`(서울) 리전 선택 가능 → BE-DB 동일 리전 권장
- **모두 한국 리전 정렬** 시 p95 latency 100ms 이내 달성 가능

### 2.4 보안·법무 관점

- **GitHub OAuth httpOnly/Secure/SameSite=None 쿠키** (기존 ADR 유지)
- **CSRF 보호** 비-GET 요청에 적용 (기존 계획 유지)
- 광고 미부착 → 광고 SDK·쿠키 동의·사업자등록 의무 회피
- **개인정보처리방침 페이지 1개**는 GitHub 프로필(닉네임·아바타) 수집 명시 차원에서 추가 권장
- Cloudflare WAF 무료 룰셋으로 기본 봇/스캔 차단

### 2.5 이슈 #15~#18 (검색 재설계) 관점

- PostGIS 확장이 **필수** → Neon 채택의 결정적 이유
- Flyway `V1__init.sql` 최상단에 `CREATE EXTENSION IF NOT EXISTS postgis;`
- BE-DB 리전 정렬 시 PostGIS bbox 쿼리 latency 최소화
- Neon **DB 브랜칭**: PR마다 격리 DB 스냅샷 → 마이그레이션 dry-run 가능 (선택, 1~2인 팀엔 과할 수 있음)

### 2.6 사진 업로드 관점 (추후)

- **Cloudflare R2** 권장 — egress 무료가 결정적
- Pages·R2가 동일 Cloudflare 계정 → DNS/캐시 룰 통합 관리
- Spring Boot에서 S3 호환 SDK(`aws-sdk-java-v2`)로 R2 접근 가능
- 대안 비교:
  - Supabase Storage 1GB 무료지만 egress $0.09/GB
  - S3 + CloudFront — 설정 복잡, 1~2인 팀엔 비추
- Neon은 BLOB 저장에 부적합 → 이미지는 반드시 Object Storage로 분리

### 2.7 확장성 관점

- 사용자 100명 → 10,000명까지 **현 스택 변경 0**
- 그 이상에서 병목 후보:
  1. Fly shared-1x → dedicated CPU 업그레이드 ($수십)
  2. Neon Launch → Scale 플랜
  3. R2는 사실상 무한
- **이주 비용이 매우 낮음** (Docker + Postgres 표준) → 지금 오버 엔지니어링할 이유 없음

---

## 3. CI/CD 파이프라인 (확정)

```
PR open
  └─ GitHub Actions (게이트만)
       ├─ BE: ./gradlew test (Testcontainers PostGIS)
       ├─ FE: pnpm lint && pnpm test && pnpm build
       └─ Trivy 컨테이너 이미지 스캔 (선택)
  └─ merge to dev
       ├─ Fly: fly deploy --config fly.dev.toml  (스테이징)
       └─ Cloudflare Pages: dev 브랜치 자동 배포
  └─ merge to main
       ├─ Fly: fly deploy --config fly.prod.toml (rolling, 헬스체크)
       ├─ Cloudflare Pages: production 자동 배포
       └─ Sentry: 릴리즈 등록 + 소스맵 업로드 (선택)
```

**원칙**:
- GitHub Actions 는 **"통과 못 하면 머지 못 함"** 의 게이트
- PaaS git 연동이 실제 빌드/배포 담당 (이중 책임 금지)
- Flyway 는 Spring Boot 시작 시 자동 실행, 별도 잡 없음
- 마이그레이션 실패 시 BE 부팅 실패 → Fly 가 이전 릴리즈 유지

---

## 4. 환경 구성

| 환경 | 브랜치 | BE | DB | FE |
|---|---|---|---|---|
| local | (작업 브랜치) | docker compose | docker compose Postgres+PostGIS | `pnpm dev` (Vite) |
| dev (스테이징) | `dev` | Fly app `enchelin-be-dev` | Neon branch `dev` | CF Pages preview |
| prod | `main` | Fly app `enchelin-be-prod` | Neon branch `main` | CF Pages production |

**도메인 안**:
- prod: `enchelin.app` (FE), `api.enchelin.app` (BE)
- dev: `dev.enchelin.app`, `api-dev.enchelin.app`
- 모두 Cloudflare DNS · Universal SSL

---

## 5. 비교: 왜 Railway / Vercel / Supabase / AWS 가 아닌가

| 후보 | 탈락 이유 |
|---|---|
| **Railway (BE)** | 한국 리전 없음, free tier 폐지(2023.08), Fly 대비 컨트롤·비용 효율 열세. 다만 GUI/DX 는 우위 — 운영 부담을 더 줄이고 싶다면 차선책. |
| **Vercel (FE)** | Hobby 플랜이 **개인·비상업 전용**. 부트캠프 전용이라도 회색지대. 100GB 대역폭 한도. DX 는 업계 최고 — 광고·수익화 가능성이 0% 라면 차선책. |
| **Supabase (DB)** | GitHub OAuth 자체 구현으로 결정 → Auth/RLS 가치 절반 소멸. Storage 1GB 는 매력적이나 egress 과금이 R2 대비 불리. 7일 비활성 시 일시정지. |
| **AWS / GCP / k8s** | 1~2인이 IAM·VPC·RDS·ALB 운영하면 본업 못 함. 100명 사용자에게 주는 가치 차이 0. 월 최소 비용도 더 비쌈. |
| **자체 EC2 + Postgres** | TLS·백업·패치·모니터링을 직접. 운영 시간 폭증. |

---

## 6. 위험 및 완화

| 위험 | 영향 | 완화 |
|---|---|---|
| Fly 학습 곡선 (Railway 대비) | 초기 셋업 +반나절 | `fly launch` 마법사 + `fly.toml` 1회 작성으로 끝남 |
| Neon Free 0.5GB 한도 도달 | 서비스 중단 | 알람 설정 + Launch $19 즉시 승급 |
| Cold start (scale-to-zero 사용 시) | 첫 요청 1~3s | min_machines_running=1 (월 $5 안쪽) 또는 UptimeRobot ping |
| Cloudflare Pages 빌드 500회/월 한도 | 머지 폭주 시 빌드 거부 | 100명 규모에선 비현실적, 도달 시 Workers Paid $5 |
| GitHub OAuth 콜백 URL 환경별 분기 | prod/dev 인증 깨짐 | OAuth App 2개 등록 (dev / prod), env 로 분리 |
| 마이그레이션 실패로 BE 부팅 불가 | 배포 차단 | Fly rolling + 헬스체크 → 이전 릴리즈 자동 유지 |
| Secrets 3곳 분산 (Fly/Pages/Actions) | 휴먼 에러 | 런북에 체크리스트, 분기 시 Doppler 도입 |

---

## 7. 이슈 #23 분해 (배포 전용)

이슈 #23 제목이 `railway-deploy` 였다면 **리네이밍 또는 새 epic 생성** 후, 아래 PR 단위로 분해한다.

| # | PR 제목 | 범위 | 의존성 |
|---|---|---|---|
| 1 | `chore(BE): Dockerfile 정비 + /actuator/health 노출` | 멀티스테이지 Dockerfile, 헬스체크, JVM 옵션 | — |
| 2 | `chore(BE): Flyway + PostGIS V1 마이그레이션` | `V1__init.sql` (CREATE EXTENSION postgis 포함), `application-prod.yml` ddl-auto=validate | #1 |
| 3 | `feat(BE): GitHub OAuth + Spring Security (httpOnly 쿠키)` | 기존 mvp 계획 Step 1.1 + 1.2 (원자적 머지) | #2 |
| 4 | `chore(infra): Fly.io 배포 - dev 환경` | `fly.dev.toml`, secrets 가이드, Neon dev branch 연결 | #1 |
| 5 | `chore(infra): Cloudflare Pages FE 배포` | 빌드 명령, env (`VITE_API_BASE_URL`), SPA fallback | — (병렬) |
| 6 | `ci: GitHub Actions 테스트 게이트 워크플로우` | BE Testcontainers PostGIS, FE lint/build, PR 트리거 | #2 |
| 7 | `chore(infra): prod 환경 + 도메인 + Cloudflare DNS` | `fly.prod.toml`, OAuth App prod 콜백, 도메인 연결 | #3,#4,#5 |
| 8 | `docs: 배포·롤백 런북 README` | 1페이지 운영 가이드 | #7 |

**원자성**: #3 은 단독 머지 금지 (인증 부분 노출 방지). #1~#2 가 먼저 prod 에 도달해야 함.

---

## 8. Definition of Done (배포 작업 전체)

- [ ] prod URL 에서 GitHub 로그인 → 지도 → 리뷰 작성 E2E 통과
- [ ] BE-DB 동일 리전(서울), p95 latency < 300ms (BE 내부 측정)
- [ ] Flyway 마이그레이션이 prod 에서 자동 적용, PostGIS 확장 활성
- [ ] PR 머지 시 dev / main 자동 배포 동작
- [ ] 롤백 1회 리허설 (`fly releases rollback`) 성공
- [ ] Sentry 에 BE/FE 에러 1건씩 수신 확인
- [ ] UptimeRobot 알림 Slack/Discord 연동
- [ ] README 에 배포·롤백·secrets 갱신 절차 1페이지 정리
- [ ] 모든 secrets 가 코드/저장소에 없음 (Trivy + gitleaks 통과)

---

## 9. 향후 검토 (Post-MVP)

- **사진 업로드**: R2 + presigned URL, 썸네일은 Cloudflare Images 또는 BE 비동기 리사이즈
- **검색 성능**: PostGIS GiST 인덱스 모니터링, 필요 시 pg_trgm 추가
- **관측성**: OpenTelemetry → Grafana Cloud Free
- **DB 브랜칭 활용**: Neon branch per PR — CI 에서 마이그레이션 dry-run
- **비용 알람**: Fly / Neon / Cloudflare 각 대시보드에 임계치 알람

---

## 10. 변경 이력

- **2026-04-07** — 초안 작성. `enchelin-mvp.md` Phase 1 Step 1.3 (Railway 전제) 을 본 문서로 대체.
