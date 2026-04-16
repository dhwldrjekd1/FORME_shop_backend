# syntax=docker/dockerfile:1.6
#
# 멀티스테이지 빌드:
#  1. frontend  : Node 로 Vue 빌드 → dist 산출
#  2. backend   : Gradle 로 jar 빌드 (frontend dist 를 static 폴더에 포함)
#  3. runtime   : JRE 21 만 들어있는 가벼운 이미지로 실행
#
# 빌드 컨텍스트는 forme/ (parent) — compose 에서 context: .. 로 지정
# 그래야 forme/src(프론트)와 forme/backend(백엔드) 양쪽을 한 번에 다 볼 수 있음.
# ─────────────────────────────────────────────────────────────
# Stage 1: 프론트엔드 빌드
# ─────────────────────────────────────────────────────────────
FROM node:22-alpine AS frontend
WORKDIR /build
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npx vite build --outDir dist --emptyOutDir
# ─────────────────────────────────────────────────────────────
# Stage 2: 백엔드(Gradle) 빌드
# ─────────────────────────────────────────────────────────────
FROM gradle:8.10-jdk21-alpine AS backend
WORKDIR /build
COPY shoptm_backend/build.gradle shoptm_backend/settings.gradle ./
COPY shoptm_backend/gradle ./gradle
COPY shoptm_backend/gradlew ./gradlew
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true
COPY shoptm_backend/src ./src
COPY --from=frontend /build/dist ./src/main/resources/static
RUN ./gradlew bootJar --no-daemon -x test
# ─────────────────────────────────────────────────────────────
# Stage 3: 런타임 (JRE 21 만)
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend /build/build/libs/*.jar app.jar
COPY --from=frontend /build/public/images /app/static-images
COPY --from=frontend /build/public/new /app/static-new
EXPOSE 8302
ENTRYPOINT ["java", "-jar", "/app/app.jar"]