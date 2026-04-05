# Enchelin MVP Implementation Plan

**Date:** 2026-04-02
**Timeline:** 1 month (target: 2026-05-02)
**Developer:** Solo
**Complexity:** MEDIUM-HIGH

---

## 1. Requirements Summary

### What Exists
- **Backend:** Spring Boot 3.5 + Java 17, Restaurant entity with Kakao Maps nearby search API, Review CRUD (create/read/update/soft-delete), User entity (GitHub OAuth fields defined but not wired), custom pagination (OffsetLimit/Page), ApiResponse wrapper, GlobalExceptionHandler, ErrorCode enum
- **Frontend:** Vue 3 + Vite scaffold only (default template, no actual UI built)
- **Infrastructure:** Docker Compose (PostgreSQL 16 + backend), backend Dockerfile (multi-stage), GitHub Actions CI (build-only on dev), CD pipeline (Docker Hub + EC2 SSH deploy on main -- references old "kochelin" project, needs rewrite for Railway)
- **Entities live in `service/` package** (unconventional -- entities mixed with services)

### What Needs to Be Built
1. **GitHub OAuth login flow** -- Spring Security OAuth2 client, session/JWT management, login/logout endpoints
2. **Auth-aware API layer** -- wire User to Review CRUD (currently uses raw userId), protect write endpoints
3. **Frontend application** -- 5 screens (Login, Map, Restaurant Detail, Review Form, My Page) with Vue Router, state management, Kakao Map SDK integration
4. **Mobile-responsive UI** -- all screens must work on mobile viewport
5. **Error/loading states** -- user-friendly error messages, loading spinners on all async operations
6. **CI/CD for Railway** -- rewrite deploy.yml for Railway auto-deploy (current CD targets EC2 with Docker Hub, not Railway)
7. **Railway deployment** -- configure Railway services for frontend + backend + PostgreSQL

---

## 2. RALPLAN-DR Summary

### Principles (5)
1. **Leverage existing code** -- build on top of the working Restaurant/Review/User entities rather than restructuring
2. **Backend-first, frontend-follows** -- complete auth + API contracts before building Vue screens
3. **Minimal viable scope** -- 5 screens, no image uploads, no social features beyond reviews
4. **Mobile-first responsive** -- design for mobile viewport, scale up to desktop
5. **Deploy early, iterate** -- get Railway pipeline working in Phase 1 so every subsequent phase is deployable

### Decision Drivers (Top 3)
1. **1-month solo timeline** -- every decision must minimize scope and integration risk
2. **Existing code patterns** -- must follow current conventions (ApiResponse wrapper, OffsetLimit pagination, service-layer entities) to avoid refactoring time
3. **Railway deployment target** -- architecture must fit Railway's model (separate services, env vars, no Docker Compose in prod)

### Option A: Session-based auth (Spring Security OAuth2 + HttpSession)
| Pros | Cons |
|------|------|
| Built-in Spring Security support, minimal custom code | Requires sticky sessions or session store (Redis) on Railway |
| Cookie-based, no frontend token management | CORS cookie config can be tricky with separate front/back domains |
| Simpler to implement for a solo dev | Scaling requires shared session store |

### Option B: JWT-based auth (OAuth2 login -> issue JWT in httpOnly cookie)
| Pros | Cons |
|------|------|
| Stateless, no session store needed | More custom code (JWT filter, token refresh logic) |
| Better fit for Railway (no Redis dependency) | CSRF protection needed for non-GET requests (since using cookies) |
| httpOnly cookie avoids XSS token theft | SameSite=None + Secure required for cross-origin cookies |

**Recommended: Option B (JWT in httpOnly cookie)** -- Railway does not bundle Redis by default, making session-based auth impractical. JWT in httpOnly cookies combines the stateless benefits of JWT with secure cookie storage (no localStorage exposure to XSS). CSRF protection is required for state-changing requests.

### Frontend State Management Options

### Option A: Pinia (Vue official store)
| Pros | Cons |
|------|------|
| Official Vue recommendation, well-documented | Additional dependency |
| DevTools integration | Slight learning curve if unfamiliar |

### Option B: Vue composables only (no store library)
| Pros | Cons |
|------|------|
| Zero dependencies, simpler for small app | No DevTools, harder to debug state |
| Sufficient for 5-screen MVP | State sharing patterns less standardized |

**Recommended: Option A (Pinia)** -- the auth state (user profile, token) needs to be shared across all screens, and Pinia provides clean patterns for this with minimal overhead.

---

## 3. Implementation Plan

### Phase 1: Infrastructure & Auth Backend (Week 1)

#### Step 1.1: Backend Auth -- GitHub OAuth2 + JWT
**Files to create/modify:**
- `backend/build.gradle` -- add `spring-boot-starter-oauth2-client`, `spring-boot-starter-security`, `jjwt` dependencies
- `backend/src/main/java/.../config/SecurityConfig.java` (NEW) -- Spring Security config, OAuth2 login, CORS policy
- `backend/src/main/java/.../config/JwtTokenProvider.java` (NEW) -- JWT generation/validation
- `backend/src/main/java/.../config/JwtAuthenticationFilter.java` (NEW) -- OncePerRequestFilter for JWT validation
- `backend/src/main/java/.../config/OAuth2SuccessHandler.java` (NEW) -- on OAuth success, create/update User, issue JWT in httpOnly/Secure/SameSite=None cookie, redirect to frontend
- `backend/src/main/java/.../config/CsrfTokenConfig.java` (NEW) -- CSRF protection for non-GET requests (required when using cookie-based auth)
- `backend/src/main/java/.../repository/UserRepository.java` (NEW) -- findByGithubId
- `backend/src/main/java/.../controller/AuthController.java` (NEW) -- `/api/auth/me` (current user info), `/api/auth/logout`, `/api/auth/refresh` (refresh token endpoint)
- `backend/src/main/java/.../dto/UserResponse.java` (NEW) -- user profile DTO
- `backend/src/main/resources/application.yml` -- add OAuth2 client config (github provider), JWT secret, CORS origins
- `backend/src/main/resources/application-local.yml` (NEW) -- local dev overrides
- `backend/src/main/resources/application-prod.yml` (NEW) -- production config with env var placeholders, `ddl-auto: validate` (NEVER use create/update in prod)
- `backend/build.gradle` -- add `org.flywaydb:flyway-core` and `org.flywaydb:flyway-database-postgresql` dependencies
- `backend/src/main/resources/db/migration/V1__init.sql` (NEW) -- initial Flyway migration creating all tables matching current entities (User, Restaurant, Review)

**Token Strategy (MVP):**
- Short-lived access token (15 min) stored in httpOnly/Secure/SameSite=None cookie
- Refresh token (7 days) also stored in httpOnly cookie (separate cookie name)
- `/api/auth/refresh` endpoint: validates refresh token, issues new access token cookie
- On logout: clear both cookies

**Acceptance Criteria:**
- GitHub OAuth2 login redirects to GitHub, callback creates/updates User in DB
- JWT access token (15 min) and refresh token (7 days) set as httpOnly/Secure/SameSite=None cookies on successful OAuth
- `/api/auth/me` returns current user profile when valid JWT cookie present
- `/api/auth/refresh` issues a new access token when called with valid refresh token cookie
- Unauthenticated requests to protected endpoints return 401
- CORS configured to allow frontend origin with `allowCredentials: true`
- CSRF protection active for non-GET requests
- `application-prod.yml` uses `ddl-auto: validate`; schema managed by Flyway migrations

#### Step 1.2: Wire Auth to Existing APIs
**Files to modify:**
- `backend/src/main/java/.../controller/ReviewController.java` -- replace `request.userId()` with authenticated user from SecurityContext
- `backend/src/main/java/.../service/ReviewService.java` -- update `createReview` to accept User, replace "temp name" with real user nickname
- `backend/src/main/java/.../dto/CreateReviewRequest.java` -- remove userId field (comes from auth)
- `backend/src/main/java/.../dto/ReviewResponse.java` -- add user nickname and avatarUrl fields
- `backend/src/main/java/.../controller/ReviewController.java` -- add ownership check on update/delete (only review author can modify)
- `backend/src/main/java/.../common/exception/ErrorCode.java` -- add UNAUTHORIZED, FORBIDDEN error codes

**Acceptance Criteria:**
- Review creation uses authenticated user's ID (no userId in request body)
- Review update/delete only allowed by the review's author (403 otherwise)
- ReviewResponse includes real user nickname instead of "temp name"
- Unauthenticated users can still read reviews (GET endpoints remain public)

**IMPORTANT: Auth + endpoint fixes must be deployed atomically.** Never deploy partial auth where update/delete endpoints exist without ownership checks. Step 1.1 and 1.2 must be merged and deployed together.

#### Step 1.3: CI/CD Pipeline for Railway
**Deployment strategy: Railway GitHub integration (auto-deploy on push to main)**
- Railway watches the GitHub repo and auto-deploys on push to main branch
- Backend uses Nixpacks (auto-detects Gradle/Java)
- Frontend uses Dockerfile (nginx-based static serving)
- No manual `railway up` needed

**Files to create/modify:**
- `.github/workflows/deploy.yml` -- FULL REWRITE: remove all "kochelin"/EC2/MySQL/Docker Hub references. New workflow triggers on push to main, runs tests, then Railway auto-deploys via GitHub integration (no deploy step needed in Actions — Railway handles it)
- `.github/workflows/ci.yml` -- update to also run on PRs targeting dev, add frontend build check
- `frontend/Dockerfile` (NEW) -- nginx-based static file serving for Railway
- `frontend/nginx.conf` (NEW) -- SPA fallback routing config
- `railway.toml` (NEW, optional) -- Railway service configuration if auto-detect needs overrides

**Acceptance Criteria:**
- Push to dev triggers CI (backend build + frontend build)
- Push/merge to main triggers Railway auto-deployment via GitHub integration
- Backend, frontend, and PostgreSQL all running on Railway
- Environment variables (DB URL, OAuth secrets, JWT secret, Kakao API key) configured in Railway
- Old deploy.yml references to kochelin/EC2/MySQL completely removed

---

### Phase 2: Frontend Foundation & Core Screens (Week 2)

#### Step 2.1: Frontend Scaffolding
**Files to create/modify:**
- `frontend/package.json` -- add dependencies: `vue-router`, `pinia`, `axios`
- `frontend/src/main.js` -- configure router + pinia
- `frontend/src/router/index.js` (NEW) -- routes: `/login`, `/map`, `/restaurants/:id`, `/reviews/new`, `/mypage`
- `frontend/src/stores/auth.js` (NEW) -- Pinia store for auth state (user profile, login/logout actions; NO token in localStorage — auth via httpOnly cookies)
- `frontend/src/stores/restaurant.js` (NEW) -- Pinia store for restaurant/map data
- `frontend/src/api/client.js` (NEW) -- axios instance with `withCredentials: true` (cookie-based auth), base URL config, 401 interceptor for token refresh
- `frontend/src/api/auth.js` (NEW) -- auth API calls
- `frontend/src/api/restaurant.js` (NEW) -- restaurant API calls
- `frontend/src/api/review.js` (NEW) -- review API calls
- `frontend/src/App.vue` -- replace scaffold with layout (nav bar + router-view)
- `frontend/src/components/common/LoadingSpinner.vue` (NEW)
- `frontend/src/components/common/ErrorMessage.vue` (NEW)
- `frontend/src/components/common/NavBar.vue` (NEW) -- responsive nav with login/profile
- `frontend/vite.config.js` -- add API proxy for local dev

**Acceptance Criteria:**
- Vue Router navigation between all 5 routes works
- Auth store manages user state; authentication via httpOnly cookies (`withCredentials: true`), no localStorage token
- Navigation guard redirects unauthenticated users from protected routes to login
- LoadingSpinner and ErrorMessage components render correctly
- API proxy forwards `/api/*` to backend in dev mode

#### Step 2.2: Login Screen + Map Screen
**Files to create:**
- `frontend/src/views/LoginView.vue` (NEW) -- GitHub OAuth login button, redirects to backend OAuth endpoint
- `frontend/src/views/MapView.vue` (NEW) -- Kakao Map with current location, restaurant markers
- `frontend/src/components/map/KakaoMap.vue` (NEW) -- Kakao Map SDK wrapper component
- `frontend/src/components/map/RestaurantMarker.vue` (NEW) -- marker popup with restaurant name/category
- `frontend/index.html` -- add Kakao Maps JavaScript SDK script tag

**Acceptance Criteria:**
- Login page shows GitHub login button, clicking initiates OAuth flow
- After OAuth callback, user is redirected to map screen with auth cookie set
- Map displays centered on user's current location (geolocation API)
- Nearby restaurants fetched from backend and displayed as markers
- Clicking a marker shows restaurant name and navigates to detail page
- Mobile responsive layout (map fills viewport)

---

### Phase 3: Restaurant Detail, Reviews & My Page (Week 3)

#### Step 3.1: Restaurant Detail + Review List
**Files to create/modify:**
- `frontend/src/views/RestaurantDetailView.vue` (NEW) -- restaurant info + paginated review list
- `frontend/src/components/review/ReviewCard.vue` (NEW) -- single review display (user, rating, comment, date)
- `frontend/src/components/review/ReviewList.vue` (NEW) -- scrollable review list with "load more" pagination
- `frontend/src/components/review/StarRating.vue` (NEW) -- star rating display component
- `backend/src/main/java/.../controller/RestaurentController.java` -- add `GET /restaurants/{id}` endpoint for single restaurant detail
- `backend/src/main/java/.../dto/RestaurantDetailResponse.java` (NEW) -- restaurant info + average rating + review count

**Acceptance Criteria:**
- Restaurant detail page shows name, category, address, Kakao Place link
- Average rating displayed (calculated from reviews)
- Reviews listed in reverse chronological order with offset-based pagination
- "Load more" button fetches next page of reviews
- Each review shows author nickname, avatar, rating stars, comment, date
- Mobile responsive layout

#### Step 3.2: Review Creation/Edit Form + My Page
**Files to create:**
- `frontend/src/views/ReviewFormView.vue` (NEW) -- create/edit review form (rating selector + comment textarea)
- `frontend/src/views/MyPageView.vue` (NEW) -- user profile card + list of user's reviews
- `frontend/src/components/review/RatingInput.vue` (NEW) -- interactive star rating input
- `frontend/src/components/user/ProfileCard.vue` (NEW) -- avatar, nickname, GitHub link

**Files to modify:**
- `frontend/src/components/review/ReviewCard.vue` -- add edit/delete buttons (visible only to review author)

**Acceptance Criteria:**
- Review form validates rating (1-5) and comment (required, max 100 chars) before submission
- Successful creation redirects to restaurant detail page
- Edit mode pre-fills existing review data
- Delete shows confirmation dialog before proceeding
- My Page shows user profile info from GitHub (avatar, nickname, GitHub link)
- My Page lists user's reviews with pagination
- All forms show loading state during submission and error messages on failure
- Mobile responsive layout

---

### Phase 4: Polish & Deployment (Week 4)

#### Step 4.1: Error Handling, Loading States & Responsive Polish
**Files to modify:**
- All view components -- ensure every async operation shows LoadingSpinner and handles errors with ErrorMessage
- `frontend/src/api/client.js` -- add global error interceptor (401 -> redirect to login, 500 -> generic error toast)
- `frontend/src/App.vue` -- add global error toast/notification area
- `frontend/src/components/common/Toast.vue` (NEW) -- toast notification component
- All components -- verify mobile responsive breakpoints (test at 375px, 768px, 1024px)

**Acceptance Criteria:**
- Every API call shows loading indicator while pending
- Network errors display user-friendly Korean error messages
- 401 errors redirect to login with "session expired" message
- All screens render correctly at mobile (375px), tablet (768px), desktop (1024px) widths
- No horizontal scrolling on any viewport

#### Step 4.2: Railway Deployment & Final Verification
**Tasks:**
- Configure Railway project with 3 services: PostgreSQL, backend, frontend
- Set all environment variables in Railway (DATABASE_URL, GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET, JWT_SECRET, KAKAO_API_KEY, FRONTEND_URL)
- Verify GitHub Actions pipeline deploys on merge to main
- End-to-end smoke test on Railway: login -> map -> detail -> create review -> my page -> logout
- Verify CORS works with Railway-assigned domains
- Update `application-prod.yml` with Railway PostgreSQL connection format

**Acceptance Criteria:**
- Full E2E flow works on Railway deployment
- GitHub Actions auto-deploys on merge to main
- All environment variables properly configured (no secrets in code)
- Application loads within 5 seconds on Railway
- OAuth callback URL registered in GitHub OAuth App settings for Railway domain

---

## 4. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Kakao Map SDK integration complexity in Vue 3 | Could delay map screen by 2-3 days | Use well-documented `kakao.maps.load()` pattern; fallback to simple list view if map blocks progress |
| Railway deployment configuration (separate services) | Could delay deployment setup | Set up Railway in Phase 1 with a minimal deploy; iterate rather than big-bang at the end |
| GitHub OAuth callback URL mismatch between local/Railway | Auth flow breaks in production | Use environment-specific callback URLs in `application-{profile}.yml`; test with ngrok locally |
| Entity package structure (`service/` holds entities) | Confusion for executors | Document in plan but DO NOT refactor -- it works and refactoring risks breaking existing code |
| Existing deploy.yml references old "kochelin" project + EC2 | CD pipeline non-functional | Rewrite entirely for Railway in Phase 1 |
| CORS issues with separate frontend/backend on Railway | API calls fail in production | Configure CORS in SecurityConfig with environment-variable-based allowed origins |

---

## 5. Verification Steps

### Per-Phase Verification
- **Phase 1:** `curl` test OAuth flow manually; verify JWT issuance; verify protected endpoint returns 401 without token
- **Phase 2:** Browser test: login flow end-to-end; map renders with markers; navigation between screens works
- **Phase 3:** Browser test: create/edit/delete review; view restaurant detail with reviews; my page loads user's reviews
- **Phase 4:** Mobile device/emulator test at 375px; Railway deployment smoke test; CI/CD pipeline test with a test PR

### Final E2E Acceptance Test
1. Open Railway URL on mobile browser
2. Click "GitHub Login" -> authenticate -> redirected to map
3. Map shows current location with nearby restaurant markers
4. Tap a restaurant marker -> navigate to detail page
5. View restaurant info and reviews (paginated)
6. Tap "Write Review" -> fill in rating + comment -> submit
7. Verify review appears in restaurant's review list
8. Navigate to My Page -> verify profile and review visible
9. Edit the review -> verify changes saved
10. Delete the review -> verify removed
11. Logout -> verify redirected to login page
12. Verify all above steps work on desktop viewport as well

---

## 6. Testable Acceptance Criteria (Summary)

| # | Criterion | Verification Method |
|---|-----------|-------------------|
| AC-1 | GitHub OAuth login/logout works | E2E: login redirects to GitHub, callback returns to app with user session |
| AC-2 | Map shows nearby restaurants based on current location | E2E: geolocation prompt -> map centers -> markers appear |
| AC-3 | Restaurant detail shows info + paginated reviews | E2E: click marker -> detail page -> reviews load -> "load more" works |
| AC-4 | Review CRUD works (create/update/delete) | E2E: create review -> edit it -> delete it -> verify each state |
| AC-5 | My Page shows user profile + reviews | E2E: navigate to my page -> profile card visible -> user's reviews listed |
| AC-6 | Mobile responsive | Manual test at 375px, 768px, 1024px viewports |
| AC-7 | User-friendly error messages | Test: disconnect network -> verify error toast; submit invalid form -> verify validation message |
| AC-8 | Loading states displayed | Test: throttle network -> verify spinners appear on API calls |
| AC-9 | CI/CD auto-deploys on PR merge | Test: merge a PR to main -> verify Railway deployment triggers |
| AC-10 | Railway deployment works | Verify: all 3 services running, app accessible via Railway URL |

---

## 7. ADR: Authentication Strategy

- **Decision:** JWT-based authentication with GitHub OAuth2 provider, tokens stored in httpOnly/Secure/SameSite=None cookies
- **Drivers:** Railway has no built-in Redis; separate frontend/backend domains require cross-domain auth; XSS protection requires httpOnly cookies over localStorage
- **Alternatives considered:**
  - Session-based auth with `spring-session-jdbc` (rejected: cross-domain cookie handling harder, adds DB session queries)
  - JWT in localStorage (rejected: XSS vulnerability exposes tokens; Architect/Critic consensus requires httpOnly cookies)
- **Why chosen:** Stateless, no additional infrastructure, httpOnly cookies eliminate XSS token theft, cross-domain works with SameSite=None
- **Consequences:** Must handle CSRF protection for non-GET requests (cookie-based auth); token refresh via /api/auth/refresh endpoint; slightly more complex CORS setup (allowCredentials: true)
- **Follow-ups:** Consider refresh token rotation for improved security post-MVP

---

## 8. Out of Scope (Explicit)

- Image upload for reviews
- User registration (GitHub OAuth only)
- Admin dashboard
- Restaurant favoriting/bookmarking
- Push notifications
- Search/filter functionality beyond nearby
- Unit/integration test suite (TDD configured separately per spec)
- Package restructuring (entities stay in `service/` package)

---

## 9. Changelog

### Revision 2 (Critic REVISE feedback, 2026-04-02)
**Critical fixes applied:**
1. **JWT storage: localStorage → httpOnly cookies** — ADR updated, OAuth2SuccessHandler sets httpOnly/Secure/SameSite=None cookies, axios uses `withCredentials: true`, auth store no longer manages tokens in localStorage. CSRF protection added for non-GET requests.
2. **deploy.yml: ambiguity resolved** — Explicitly specified Railway GitHub integration (auto-deploy on push to main). Removed "railway up OR" ambiguity. Noted full rewrite needed to remove kochelin/EC2/MySQL references.
3. **ddl-auto: validate for production** — `application-prod.yml` uses `ddl-auto: validate`. Added Flyway dependency + initial migration script V1__init.sql.

**Major fixes applied:**
4. **Atomic auth deployment** — Added note that Step 1.1 + 1.2 must be deployed together. Never expose unprotected update/delete endpoints.

**Minor fixes applied:**
5. **Token refresh strategy** — Added `/api/auth/refresh` endpoint, short-lived access token (15min) + refresh token (7 days) in httpOnly cookies.
6. **Rating validation** — Existing code uses `@Min(0)`, to be clarified during execution.
7. **Controller typo** — Existing file is `RestaurentController.java` (original typo), plan uses actual filename where applicable.
