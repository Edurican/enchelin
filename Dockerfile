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
