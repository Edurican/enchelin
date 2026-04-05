# Open Questions

## enchelin-mvp - 2026-04-02

- [ ] Railway deployment model: will frontend be deployed as a separate static site service or bundled with backend? -- Affects Dockerfile strategy and CORS configuration
- [ ] Kakao Maps JavaScript API key: is this the same key as the REST API key (`kakao.api.key`) or a separate app key? -- Determines whether a new Kakao app registration is needed for the frontend SDK
- [ ] GitHub OAuth App: has one been created yet? Need Client ID and Client Secret for both local dev and Railway production callback URLs -- Blocks Phase 1 auth implementation
- [ ] Railway project: has it been created? Are there existing services or starting from scratch? -- Affects Phase 1.3 timeline
- [ ] JWT token refresh strategy: should the MVP implement refresh tokens or just use long-lived access tokens (e.g., 24h)? -- Affects auth complexity; long-lived tokens are simpler but less secure
- [ ] Review ownership: should review update/delete require only author check, or should an admin role (GOURMAND from RoleEnum) also have permissions? -- Affects authorization logic complexity
- [ ] Geolocation fallback: if user denies location permission, should the map default to a specific location (e.g., bootcamp address)? -- Affects map screen UX
- [ ] Existing `deploy.yml` references EC2 + Docker Hub + "kochelin" naming: is EC2 deployment still needed alongside Railway, or fully replacing? -- Determines whether to rewrite or create a new workflow file
