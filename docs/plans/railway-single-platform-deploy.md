# Railway 단일 플랫폼 배포 — 실행 패키지

**작성일:** 2026-04-07
**최종 갱신:** 2026-04-07 (현 레포 상태 대조 후 정정 / 카카오맵 관점 리뷰 반영)
**상태:** 확정 (이슈 #23 기준)
**전제:** 운영 변수 최소화 우선. FE/BE 분리 배포 폐기, same-origin 통합.
**관련 문서:** `deployment-strategy.md` (Fly+Neon 균형안은 비교용으로 보존), `.omc/specs/deep-interview-enchelin-mvp.md`

> ⚠️ **중요 — 본 문서는 현 레포 상태와 대조한 결과 다음 결정을 반영합니다.**
> - **Java 버전: 17 유지** (`build.gradle` toolchain 17, 기존 Dockerfile 17과 정합)
> - **패키지 그룹: `com.edurican.enchelinbe.*`** (계획서 초안의 `com.enchelin.backend.*`는 오기였음)
> - **Dockerfile 위치: 레포 루트로 이동** (`backend/Dockerfile` 유지 시 Railway Dockerfile builder는 dockerfile 디렉터리만 컨텍스트로 잡으므로 `COPY frontend/...` 실패)
> - **Flyway V1은 절대 덮지 않음.** 현 `V1__init.sql` 그대로 유지하고, 필요한 변경은 **V2**로 추가
> - **DB 환경변수는 분리 유지** (`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`) — 현 `application-prod.yml`과 호환, 특수문자 인코딩 이슈 회피
> - **현 인증 모델은 localStorage + `Authorization: Bearer`** (쿠키 인증 아님). 계획 초안의 "쿠키 SameSite None→Lax" 절은 **현 시점 적용 대상 없음**으로 보류
> - **FE axios `baseURL: ''` 유지.** 현 코드가 `/restaurants/...`, `/reviews/...` 등 절대 경로를 사용하므로 `/api`로 prefix 변경 시 전 호출 404
> - **기존 `.github/workflows/deploy.yml`(EC2+DockerHub+MySQL 흐름) 삭제 필수** — main 푸시 시 EC2로 잘못 배포되는 것을 막아야 함
> - **Spring Security 의존성 현재 없음.** SecurityFilterChain 코드는 신규 도입 시점에 별도 PR로 분리 (본 배포 PR에서는 다루지 않음)
> - **🔑 카카오맵 빌드타임 키 이름은 `VITE_KAKAO_JS_KEY`** — 실제 FE 코드(`frontend/src/components/map/KakaoMap.vue`)와 `.env.example`이 이 이름을 사용. 초안에 적혀 있던 `VITE_KAKAO_MAP_KEY`는 **오기**이며 이 이름으로 주입하면 `appkey=undefined`로 빌드되어 지도 초기화 실패. Dockerfile ARG/ENV와 Railway Build Variable 모두 `VITE_KAKAO_JS_KEY`로 등록해야 함
> - **Kakao 앱 키는 JS 키(FE) / REST 키(BE) 2개 분리.** Web 플랫폼 도메인 등록은 **JavaScript 키 앱**에 해야 지도 SDK가 로드됨 (REST 키 앱과 혼동 금지)

---

## 0. 최종 구조 한눈에

```
Railway Project (Singapore)
├── service: app   ← Spring Boot (API + Vue 정적 동봉), Dockerfile
└── service: db    ← postgis/postgis:16-3.4
```

환경: **local + prod 2개만**. main 머지 → Railway 자동 배포.

---

## 1. 폴더 구조

```
enchelin/
├── Dockerfile                      # ★ 레포 루트 (멀티스테이지 node→gradle→jre)
├── railway.json                    # ★ 레포 루트
├── backend/
│   ├── build.gradle
│   └── src/main/
│       ├── java/.../config/
│       │   ├── SecurityConfig.java
│       │   └── SpaForwardingController.java
│       └── resources/
│           ├── application.yml
│           ├── application-prod.yml
│           ├── db/migration/V1__init.sql
│           └── static/             # ← Docker 빌드 시 frontend dist 복사 위치 (gitignore)
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/lib/http.js
└── .github/workflows/ci.yml
```

`backend/src/main/resources/static/`는 `.gitignore`에 추가 (빌드 산출물).

---

## 2. Frontend — same-origin 전환

### 현 상태 (2026-04-07)
- axios 클라이언트 위치: **`frontend/src/api/client.js`** (계획 초안의 `frontend/src/lib/http.js`는 존재하지 않음)
- 현재 `baseURL: ''` + 절대 경로 호출(`/restaurants/...`, `/reviews/...`, `/users/...`, `/api/auth/...`)
- vite proxy: `/api`, `/restaurants`, `/reviews`, `/restaurants`, `/users` 5종 등록되어 있음
- vite `envDir: '../'` (레포 루트 `.env`를 읽음)

### 결정
1. **`client.js`의 `baseURL: ''`은 그대로 유지.** `/api` prefix로 바꾸면 기존 호출 전부 404. (BE 컨트롤러를 일괄 `/api/**`로 옮기는 것은 본 배포 PR 범위 밖)
2. `frontend/src/api/auth.js`의 `import.meta.env.VITE_API_BASE` 분기는 **삭제** (same-origin이므로 불필요).
3. vite proxy는 로컬 dev에서 BE 통신 유지 목적이므로 **현행 5종 그대로 유지**. `docker-compose.yml`도 로컬 dev용으로 유지.
4. **빌드 타임 환경변수는 Docker `ARG`로 주입.** vite `envDir: '../'` 때문에 컨테이너 빌드 컨텍스트에 루트 `.env`가 들어가지 않으면 `VITE_KAKAO_JS_KEY`가 빈 값으로 빌드되어 카카오맵 SDK가 `appkey=undefined`로 로드됨 → Dockerfile에서 명시적으로 ARG/ENV 주입(5절 참조). **키 이름은 반드시 `VITE_KAKAO_JS_KEY`** (실제 FE 코드 `KakaoMap.vue:115`에서 `import.meta.env.VITE_KAKAO_JS_KEY`를 읽음).

### `frontend/src/api/auth.js` 변경 (예시)
```diff
- const API_BASE = import.meta.env.VITE_API_BASE || ''
-
- export function getGithubLoginUrl() {
-   return `${API_BASE}/api/auth/github`
- }
+ export function getGithubLoginUrl() {
+   return '/api/auth/github'
+ }
```

> 초안에 있었던 `frontend/src/lib/http.js` 새 파일 생성 / `baseURL: '/api'` 변경 항목은 **철회**.

---

## 3. Backend — 정적 서빙 + SPA fallback

### 현 상태
- `application.yml`은 `spring.profiles.active: local`이 하드코딩되어 있음 → prod 활성화는 **`SPRING_PROFILES_ACTIVE=prod` 환경변수**로 override.
- `application-prod.yml`은 **`DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 3개 분리 변수**를 사용 중. **이 형식을 유지.** (Railway가 제공하는 `DATABASE_URL`은 `postgresql://` 형식이라 Spring `spring.datasource.url`이 요구하는 `jdbc:postgresql://`와 다름. URL 합성 + 특수문자 인코딩 이슈 회피 차원에서도 분리 유지 권장.)
- `spring-boot-starter-actuator` 의존성이 **현재 없음.** healthcheck를 `/actuator/health`로 쓰려면 build.gradle에 추가 필요.
- `hibernate-spatial` 의존성이 **현재 없음.** 현 V1 스키마는 검색을 `x/y DOUBLE` 컬럼으로 처리(`location GEOMETRY` 컬럼은 nullable, 미사용). 따라서 **본 PR에서는 spatial dialect 강제 안 함.** PostGIS 기반 spatial 쿼리 도입은 별도 PR로 분리.
- `Spring Security` 의존성이 **현재 없음.** 인증은 `FilterConfig` + JWT 필터로 처리 중이고, FE는 `localStorage + Authorization: Bearer` 모델 → **본 PR에서 SecurityFilterChain·CookieCsrf·SameSite 변경 안 함.**

### `backend/src/main/resources/application.yml` (변경 후)
```yaml
spring:
  application:
    name: enchelin
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

jwt:
  secret: ${JWT_SECRET:default-dev-secret-key-that-should-be-changed-in-production-env}
  expiration: ${JWT_EXPIRATION:3600000}

github:
  client-id: ${GITHUB_CLIENT_ID:}
  client-secret: ${GITHUB_CLIENT_SECRET:}
  redirect-uri: ${GITHUB_REDIRECT_URI:http://localhost:8080/api/auth/github/callback}

app:
  frontend-url: ${APP_FRONTEND_URL:http://localhost:5173}   # same-origin 전환 후에도 dev 리다이렉트용으로 유지 가능
```

> `mvc.throw-exception-if-no-handler-found: true`는 Spring Boot 3.2+에서 정적 자원/SPA fallback과 충돌 케이스가 있어 **추가하지 않음.**

### `backend/src/main/resources/application-prod.yml` (변경 후)
```yaml
server:
  port: ${PORT:8080}
  forward-headers-strategy: framework

spring:
  datasource:
    url: ${DB_URL}                  # jdbc:postgresql://...
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

# actuator는 의존성 추가 후에만 활성화. 현 시점에서는 startCommand healthcheck를 GET /로 대체하거나 PR2에서 actuator 의존성과 함께 도입.
# management:
#   endpoints:
#     web:
#       exposure:
#         include: health
#   endpoint:
#     health:
#       probes:
#         enabled: true

kakao:
  api:
    key: ${KAKAO_API_KEY}

github:
  client-id: ${GITHUB_CLIENT_ID}
  client-secret: ${GITHUB_CLIENT_SECRET}
  redirect-uri: ${APP_BASE_URL}/api/auth/github/callback

# ⚠️ B1 (블로커 수정): AuthController#frontendUrl 가 OAuth 콜백 후 redirect URI 빌드에 사용.
#  prod에서 누락 시 default(http://localhost:5173)로 fallback → 로그인 후 localhost로 튐.
app:
  frontend-url: ${APP_FRONTEND_URL}
```

### `SpaForwardingController.java` — SPA fallback
> ⚠️ Spring `PathPattern`은 부정 lookahead(`(?!api)`)를 지원하지 않음. 초안 패턴은 폐기. **확장자 없는 경로만 매치**하면 `/api/**`·`/actuator/**`는 컨트롤러 매핑이 우선되므로 자연 회피됨.

```java
package com.edurican.enchelinbe.config;   // ★ 실제 그룹/패키지

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardingController {

    // 확장자 없는 GET 경로 = SPA 라우트로 간주 → index.html forward
    @RequestMapping(value = {
        "/",
        "/{path:[^.]*}",
        "/{x:[^.]*}/{y:[^.]*}",
        "/{x:[^.]*}/{y:[^.]*}/{z:[^.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
```

### JWT 필터 화이트리스트 점검
- 현재 `FilterConfig` / JWT 필터가 `/`, `/index.html`, `/assets/**`, `/favicon.ico`, SPA 라우트(`/map`, `/login` 등)에서 토큰을 강제하지 않도록 화이트리스트 확인 필요. 누락 시 정적/SPA 진입 자체가 401.
- **확장자 있는 정적 자원 전반**(`.*\..+` 정규식)을 한 번에 열어주는 편이 `/favicon.ico`, 폰트(`.woff2`), 이미지(`.png`/`.svg`), PWA manifest 등 누락을 방지.
- 본 PR에 포함할 작업: **JWT 필터 skip 패턴에 `/`, `/index.html`, `/assets/**`, 확장자 있는 모든 루트 자원, SPA 라우트 추가**.

### 보류 항목 (별도 PR)
- Spring Security 도입 + `SecurityFilterChain` 작성
- 쿠키 기반 인증 전환 + `SameSite=Lax` + CSRF
- CORS 제거 — *현재 코드에 CORS 설정 자체가 없어 삭제 대상 없음* (계획 초안 9절의 "WebConfig CORS 삭제"는 사실관계 오류, 본 갱신본에서 삭제)

---

## 4. Flyway — 현 V1 유지 (절대 덮지 않음)

> ⚠️ 현 레포에는 이미 운영 중인 `V1__init.sql`이 존재한다. 초안 4절의 V1 SQL과 **스키마가 다르며**(github_id VARCHAR vs BIGINT, restaurants `x/y DOUBLE` + nullable `GEOMETRY`, reviews `comment VARCHAR(100)` + `visit_number` + 부분 unique index 등), 덮어쓰면 **Flyway checksum mismatch + `ddl-auto: validate` 실패로 부팅 불가.**
>
> **본 PR 정책: V1은 그대로 둔다.** 스키마 변경이 필요하면 **`V2__*.sql` 신규 마이그레이션**으로 추가한다. 아래 SQL은 *참고용 원안*일 뿐 실행하지 않는다.

### 현 `V1__init.sql` (요약)
- `users(id, github_id VARCHAR(255) UNIQUE, email, nickname, avatar_url, html_url, role)`
- `restaurants(id, kakao_api_id UNIQUE, name, category, place_url, address, x DOUBLE, y DOUBLE, location GEOMETRY(Point,4326) NULL)`
  - 인덱스: `(x,y)` B-tree, `location` GIST
- `reviews(id, user_id, restaurant_id FK, rating 0~5, comment VARCHAR(100), visit_number, status DEFAULT 'ACTIVE', created_at, updated_at)`
  - 부분 unique: `(user_id, restaurant_id, visit_number) WHERE status='ACTIVE'`

### 참고용 (적용 금지) — PostGIS 도입 시 V2 후보
```sql
-- V2 예시 (적용 시점은 별도 결정)
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE users (
  id           BIGSERIAL PRIMARY KEY,
  github_id    BIGINT      NOT NULL UNIQUE,
  email        VARCHAR(255),
  nickname     VARCHAR(100) NOT NULL,
  avatar_url   TEXT,
  html_url     TEXT,
  role         VARCHAR(20)  NOT NULL DEFAULT 'USER',
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE restaurants (
  id            BIGSERIAL PRIMARY KEY,
  kakao_api_id  VARCHAR(64) NOT NULL UNIQUE,
  name          VARCHAR(255) NOT NULL,
  category      VARCHAR(100),
  place_url     TEXT,
  address       TEXT,
  location      GEOGRAPHY(POINT, 4326) NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_restaurants_location ON restaurants USING GIST (location);

CREATE TABLE reviews (
  id             BIGSERIAL PRIMARY KEY,
  user_id        BIGINT NOT NULL REFERENCES users(id),
  restaurant_id  BIGINT NOT NULL REFERENCES restaurants(id),
  rating         SMALLINT NOT NULL CHECK (rating BETWEEN 0 AND 5),
  comment        TEXT,
  status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_reviews_restaurant ON reviews(restaurant_id);
CREATE INDEX idx_reviews_user ON reviews(user_id);
```

bounds 조회 예:
```sql
SELECT * FROM restaurants
WHERE ST_Intersects(
  location,
  ST_MakeEnvelope(:minX, :minY, :maxX, :maxY, 4326)::geography
);
```

---

## 5. Dockerfile (멀티스테이지, 동봉 빌드)

> ⚠️ **위치는 레포 루트 `/Dockerfile`.** Railway Dockerfile builder는 dockerfile이 위치한 디렉터리를 빌드 컨텍스트로 잡으므로, `backend/Dockerfile`에 두면 `COPY frontend/...`가 컨텍스트 밖이 되어 실패한다. 기존 `backend/Dockerfile`은 본 PR에서 **삭제**한다.
>
> Java는 **17 유지** (build.gradle toolchain 17). vite `envDir: '../'` 영향으로 빌드타임 환경변수는 ARG로 명시 주입한다.

### `/Dockerfile` (레포 루트)
```dockerfile
# ---------- Stage 1: Frontend build ----------
FROM node:20-alpine AS fe
WORKDIR /fe
# ⚠️ 키 이름은 FE 코드(KakaoMap.vue)가 읽는 VITE_KAKAO_JS_KEY와 반드시 일치해야 함.
ARG VITE_KAKAO_JS_KEY=""
ENV VITE_KAKAO_JS_KEY=$VITE_KAKAO_JS_KEY
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
# vite envDir='../' 대응: 빌드 컨텍스트에서 루트 .env가 없을 수 있으므로 ENV로 주입한 값을 사용.
RUN npm run build

# ---------- Stage 2: Backend build ----------
FROM eclipse-temurin:17-jdk-jammy AS be
WORKDIR /be
COPY backend/gradlew ./
COPY backend/gradle gradle
COPY backend/build.gradle backend/settings.gradle ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true
COPY backend/src src
# FE 산출물을 Spring static으로 복사
COPY --from=fe /fe/dist/ src/main/resources/static/
RUN ./gradlew clean bootJar -x test --no-daemon

# ---------- Stage 3: Runtime ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=be /be/build/libs/*.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
```

### `/railway.json` (레포 루트)
```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "DOCKERFILE",
    "dockerfilePath": "Dockerfile"
  },
  "deploy": {
    "startCommand": "java $JAVA_OPTS -jar app.jar",
    "healthcheckPath": "/",
    "healthcheckTimeout": 60,
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 3
  }
}
```

> healthcheck는 **`/`** (SPA index.html)로 시작. `actuator` 의존성 추가 PR이 머지되면 `/actuator/health`로 교체.

---

## 6. Railway 서비스 구성 (수동 1회)

1. **프로젝트 생성** → 리전 **Singapore**.
2. **db 서비스**: "Deploy from Docker Image" → `postgis/postgis:16-3.4`
   - 환경변수: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB=enchelin`
   - Volume mount: `/var/lib/postgresql/data`
3. **app 서비스**: GitHub 레포 연결 → root, Dockerfile 빌더.
4. **app 환경변수** (현 yml 변수명과 정합):
   ```
   SPRING_PROFILES_ACTIVE=prod
   PORT=8080

   # DB는 분리 변수 유지 (application-prod.yml과 일치)
   DB_URL=jdbc:postgresql://${{ db.RAILWAY_PRIVATE_DOMAIN }}:5432/${{ db.POSTGRES_DB }}
   DB_USERNAME=${{ db.POSTGRES_USER }}
   DB_PASSWORD=${{ db.POSTGRES_PASSWORD }}

   APP_BASE_URL=https://<your-app>.up.railway.app
   APP_FRONTEND_URL=https://<your-app>.up.railway.app

   # GitHub OAuth (현 yml은 GITHUB_CLIENT_ID/_SECRET 사용)
   GITHUB_CLIENT_ID=...
   GITHUB_CLIENT_SECRET=...
   GITHUB_REDIRECT_URI=https://<your-app>.up.railway.app/api/auth/github/callback

   KAKAO_API_KEY=...               # 서버측 Kakao REST 키 (현 yml의 kakao.api.key)
   JWT_SECRET=...
   JWT_EXPIRATION=3600000
   ```
   **빌드 변수 (Docker ARG로 주입):**
   ```
   VITE_KAKAO_JS_KEY=...           # Railway "Build Variables" 영역에 등록 (Kakao JavaScript 키)
   ```
   > ⚠️ 키 이름 주의: `VITE_KAKAO_MAP_KEY`(X) / `VITE_KAKAO_JS_KEY`(O). FE 코드가 읽는 이름과 정확히 일치해야 하며, runtime env가 아니라 **Build Variable** 영역에 등록해야 Vite 빌드 타임에 치환됨.
   > Railway 변수 reference 문법은 실제 서비스명에 따라 `${{ <ServiceName>.VAR }}` 형태. 위 `db.*`는 예시이므로 본인 프로젝트의 db 서비스명으로 치환할 것.
5. **자동 배포**: main 브랜치 + "Wait for CI" ON.
6. **롤백**: Deployments 탭 → 이전 릴리즈 → "Redeploy".

---

## 7. OAuth / Kakao 등록 (단일 도메인)

- **GitHub OAuth App**: Authorization callback URL = `https://<your-app>.up.railway.app/api/auth/github/callback` (1개만 등록)
- **Kakao Developers — JavaScript 키 앱**: 내 애플리케이션 → 플랫폼 → **Web 플랫폼**에 `https://<your-app>.up.railway.app` 등록
  - 이 등록이 빠지면 `dapi.kakao.com/v2/maps/sdk.js` 로드 시 *appkey is not registered* 에러로 지도 전체 실패
  - 빌드 변수 `VITE_KAKAO_JS_KEY`에는 이 앱의 **JavaScript 키**를 넣는다
- **Kakao Developers — REST 키 앱** (서버용, 별도 앱이어도/동일 앱이어도 무방):
  - BE 환경변수 `KAKAO_API_KEY`에는 해당 앱의 **REST API 키**를 넣는다
  - 서버 사이드 호출이므로 Web 플랫폼 도메인 등록은 불필요

---

## 8. CI 게이트 (`.github/workflows/ci.yml`)

> ⚠️ **기존 `.github/workflows/deploy.yml`(EC2+DockerHub+MySQL) 파일은 본 PR에서 삭제한다.** 남겨두면 main 푸시 때 EC2 흐름이 함께 트리거되어 잘못된 배포가 발생한다.
>
> Java는 17 유지. frontend `package.json`에 lint 스크립트가 없으므로 lint 스텝은 제외(추후 도입 시 추가).

```yaml
name: ci
on:
  pull_request:
  push:
    branches: [main, dev]
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17' }
      - run: ./gradlew test
        working-directory: backend
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20' }
      - run: npm ci && npm run build
        working-directory: frontend
```

배포는 Railway에 위임 — Actions에서 `railway up` 같은 거 호출하지 않음.

---

## 9. 변경/삭제 체크리스트 (현 레포 상태 대조 후 갱신)

**삭제**
- `.github/workflows/deploy.yml` (EC2 + DockerHub + MySQL 흐름) — main 푸시 충돌 방지
- `backend/Dockerfile` (루트 Dockerfile로 대체)
- `frontend/src/api/auth.js`의 `import.meta.env.VITE_API_BASE` 분기
- 루트/`frontend` `.env*`에서 `VITE_API_BASE` 키 (있다면)

**변경**
- `application-prod.yml`: 현 `DB_URL/USERNAME/PASSWORD` 분리 변수 **그대로 유지** (초안의 `DATABASE_URL` 단일화 철회)
- `application.yml`: `spring.profiles.active`를 `${SPRING_PROFILES_ACTIVE:local}`로 override 가능하게 변경
- JWT 필터 화이트리스트에 `/`, `/index.html`, `/assets/**`, `/favicon.ico`, SPA 라우트 추가
- `.gitignore`에 `backend/src/main/resources/static/` 추가

**추가**
- `/Dockerfile` (레포 루트, 멀티스테이지)
- `/railway.json` (레포 루트, healthcheck `/`)
- `com.edurican.enchelinbe.config.SpaForwardingController`

**유지 (초안의 변경 제안 철회)**
- FE axios `client.js`의 `baseURL: ''` 유지 (`/api` 변경 안 함)
- `docker-compose.yml` (로컬 dev 용) 유지
- vite `proxy` 5종 (`/api`, `/restaurants`, `/reviews`, `/users`) 유지
- 현 `V1__init.sql` 유지 — 변경은 V2로
- 쿠키 SameSite·CORS·SecurityFilterChain 변경 **본 PR에서 다루지 않음** (현 시점 인증은 localStorage + Bearer)

**보류 (별도 PR)**
- `spring-boot-starter-actuator` 의존성 + healthcheck `/actuator/health` 전환 (현 `/` healthcheck의 false-positive 해소)
- `hibernate-spatial` 의존성 + PostGIS 기반 spatial 쿼리 (V2 마이그레이션 포함)
- Spring Security 도입 + 쿠키 인증 전환
  - 🗺️ **CSP 도입 시 카카오맵 화이트리스트 필수**: `script-src https://dapi.kakao.com https://t1.daumcdn.net 'unsafe-inline'`, `img-src https://*.daumcdn.net data:`, `connect-src 'self' https://dapi.kakao.com`. 누락 시 지도 타일/마커 즉시 차단

---

## 10. 검증 시나리오

1. **로컬 단일 이미지**
   ```bash
   docker build -f Dockerfile --build-arg VITE_KAKAO_JS_KEY=$VITE_KAKAO_JS_KEY -t enchelin .
   docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=local enchelin
   ```
   - `http://localhost:8080/` → SPA 로딩
   - `/map` 새로고침 → 404 아닌 SPA 정상
   - **지도 타일 로딩 확인** — 브라우저 devtools Network 탭에서 `dapi.kakao.com/v2/maps/sdk.js?appkey=<실키>` 200 응답, `kakao is not registered`/`appkey=undefined` 에러 없음
   - `/api/restaurants?...` → JSON 응답 (⚠️ healthcheck `/`는 SPA 200을 돌려주므로 DB/Flyway 실패 상태에서도 false-positive. **반드시 실제 API 1건을 수동 스모크 테스트**)
2. **prod E2E**: GitHub 로그인 → 지도 진입 → **카카오맵 렌더 + 마커 클러스터 정상** → bounds 검색 → 리뷰 작성 → 마이페이지
3. **Flyway 로그**에서 `V1__init` 적용 성공 확인 (PostGIS 확장 도입은 V2 후속 PR에서 별도 확인)
4. **롤백 리허설**: Railway Deployments → 이전 버전 Redeploy → 헬스체크 통과 + API 스모크 테스트 재실행
5. **사전 점검 체크**:
   - `frontend/package-lock.json` 커밋되어 있는지 (없으면 `npm ci` 실패)
   - Kakao Developers에서 JS 키 앱의 Web 플랫폼에 Railway 도메인 등록됐는지
   - Railway Build Variables에 `VITE_KAKAO_JS_KEY`가 등록됐는지 (Runtime Variables 아님)

---

## 11. PR 분해 제안 (갱신)

1. **`chore(infra): Railway 단일 플랫폼 배포 셋업`** — 한 PR로 묶어 로컬 dev가 깨지지 않게 한다
   - `/Dockerfile`, `/railway.json` 신규
   - 기존 `backend/Dockerfile` 삭제
   - 기존 `.github/workflows/deploy.yml` 삭제
   - `.github/workflows/ci.yml` 갱신 (Java 17, lint 제거)
   - `application.yml` profile override, `application-prod.yml` 환경변수 정합 (DB_URL/USERNAME/PASSWORD 유지)
   - `SpaForwardingController` (`com.edurican.enchelinbe.config`) 추가
   - JWT 필터 화이트리스트 정적/SPA 경로 추가
   - `frontend/src/api/auth.js`의 `VITE_API_BASE` 분기 제거
   - `.gitignore`에 `backend/src/main/resources/static/` 추가
2. **`docs: Railway 배포·롤백 런북`** — 본 문서 + 운영자 런북 분리
3. *(후속, 별도 이슈)* `feat(BE): actuator + /actuator/health 전환`
4. *(후속, 별도 이슈)* `feat(BE): hibernate-spatial + PostGIS V2 마이그레이션`
5. *(후속, 별도 이슈)* `feat(BE): Spring Security 도입 + 쿠키 인증 전환` (CSP 적용 시 카카오맵 도메인 화이트리스트 포함)

---

## 12. 우선순위별 액션 (리뷰 결과)

### 🔴 블로커
- [x] **B1. `app.frontend-url` prod 누락** — `AuthController.java:29-52`에서 `@Value("${app.frontend-url}")`로 OAuth 콜백 후 FE redirect URI 빌드에 사용 중. prod yml에 없으면 default `http://localhost:5173`로 fallback되어 **로그인 후 localhost로 튐**. 3절 `application-prod.yml`에 `app.frontend-url: ${APP_FRONTEND_URL}` 추가 완료. 6절 환경변수 `APP_FRONTEND_URL` 등록 필수.

### 🟠 중요
- [ ] **I1. Dockerfile gradle 캐시 워밍 라인 제거** — `RUN ./gradlew dependencies --no-daemon || true` 는 `|| true`로 실패를 삼키고 효과도 미미. **삭제** 또는 `|| true` 제거. 5절 Dockerfile 갱신 필요.
- [ ] **I2. SPA fallback 컨트롤러 깊이/메서드 수정** — 현 매핑은 최대 3-depth + 모든 HTTP 메서드. `/restaurant/123/reviews/edit` 같은 4-depth 라우트 404, POST/PUT까지 forward됨. **수정안:**
  ```java
  @GetMapping({"/", "/{path:[^.]*}", "/**/{path:[^.]*}"})
  ```
  3절 `SpaForwardingController` 코드 블록 갱신 필요.
- [ ] **I3. Stage 2 베이스를 `gradle:8.10-jdk17`로 교체 검토** — `eclipse-temurin:17-jdk-jammy` + Wrapper는 매 빌드마다 Gradle 배포본 다운로드. Singapore→KR 빌드 시간 5~7분 일상화 위험. (선택사항: Wrapper 캐시 전략과 trade-off 검토)
- [ ] **I4. healthcheck false-positive 안전장치** — `/`는 SPA index 200이라 DB/Flyway 실패해도 active 승격됨. 본 PR에 **`spring-boot-starter-actuator` 의존성 한 줄 추가 + railway.json `healthcheckPath: /actuator/health`** 로 해소 권고 (코드 변경 없음, actuator 자동 활성화). 5절 railway.json + build.gradle 갱신 필요.

### 🟡 마이너 / 보강
- [ ] **M1. JWT 필터 화이트리스트 정규식 예시 본문화** — 3절에 권고만 있고 실제 패턴 없음. 추가:
  ```java
  // AntPathMatcher 기준
  "/", "/index.html", "/assets/**", "/favicon.ico",
  "/*.js", "/*.css", "/*.png", "/*.svg", "/*.ico", "/*.woff2", "/*.json"
  ```
- [ ] **M2. `DB_URL` reference 변수 검증** — 6절 `${{ db.POSTGRES_DB }}` 가 Railway에서 노출되는 이름과 일치하는지 확인. 불확실하면 **리터럴 `enchelin`** 박는 것이 안전.
- [ ] **M3. CI gradlew 실행 권한 처리** — 8절 `ci.yml`의 `./gradlew test` 앞에 `chmod +x ./gradlew &&` 추가 (Windows 커밋 대응).
- [ ] **M4. `/.dockerignore` 추가** — 9절 "추가" 항목에 포함. 권고 내용:
  ```
  .git
  **/node_modules
  **/build
  **/.gradle
  **/.idea
  **/dist
  *.log
  ```
- [ ] **M5. PR1 분할 검토** — 11절 단일 PR 9변경은 리뷰 부담 큼. 선택적 분할:
  - PR1a: 루트 Dockerfile + railway.json + .dockerignore + deploy.yml 삭제 + ci.yml 갱신
  - PR1b: SpaForwardingController + JWT 화이트리스트 + profile override + auth.js VITE_API_BASE 제거

---

## 13. 리뷰 반영 이력 (2026-04-07)

카카오맵 관점 리뷰 결과 다음 항목을 본 문서에 반영:

1. **[블로커 수정]** 빌드타임 키 이름 `VITE_KAKAO_MAP_KEY` → **`VITE_KAKAO_JS_KEY`** 로 전면 교체 (상단 주의사항, 2절, 5절 Dockerfile, 6절 Build Variables)
2. **[명확화]** 7절에 Kakao JS 키 / REST 키 앱 분리 및 Web 플랫폼 등록 대상 명시
3. **[방어 강화]** 3절 JWT 필터 화이트리스트에 확장자 기반 정규식 권고 추가
4. **[검증 강화]** 10절에 카카오맵 렌더 확인 단계, `/` healthcheck false-positive 경고, 사전 점검 체크리스트 추가
5. **[후속 메모]** 9절 보류 항목 및 11절 PR #5에 CSP 도입 시 카카오 도메인 화이트리스트 요구사항 기록

사전 확인 완료:
- `frontend/package-lock.json` 존재 ✅
- `.env.example`이 `VITE_KAKAO_JS_KEY` 사용 중 ✅ (계획 초안만 오기였음)