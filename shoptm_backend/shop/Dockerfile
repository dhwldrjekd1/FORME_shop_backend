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

# 의존성 캐시: package*.json 먼저 복사 → 소스 변경 시에도 install 캐시 재사용
COPY package.json package-lock.json* ./
RUN npm ci

# 나머지 프론트 소스 복사 (backend, dist, node_modules 등은 .dockerignore 로 제외)
COPY . .

# vite.config.js 의 outDir 은 호스트 기준 ../backend/... 라서 컨테이너에선 안 맞음
# → CLI 플래그로 dist 를 컨테이너 안 로컬 경로에 출력하도록 강제
RUN npx vite build --outDir dist --emptyOutDir

# ─────────────────────────────────────────────────────────────
# Stage 2: 백엔드(Gradle) 빌드
# ─────────────────────────────────────────────────────────────
FROM gradle:8.10-jdk21-alpine AS backend
WORKDIR /build

# 의존성 캐시 최적화: gradle 파일들 먼저 복사
COPY backend/build.gradle backend/settings.gradle ./
COPY backend/gradle ./gradle
COPY backend/gradlew ./gradlew
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 백엔드 소스 복사
COPY backend/src ./src

# 프론트 빌드 산출물을 Spring Boot static 리소스 폴더로 주입
COPY --from=frontend /build/dist ./src/main/resources/static

# 부트 jar 생성 (테스트는 빌드 시 스킵 — DB 연결 실패 회피)
RUN ./gradlew bootJar --no-daemon -x test

# ─────────────────────────────────────────────────────────────
# Stage 3: 런타임 (JRE 21 만)
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드된 jar 만 복사
COPY --from=backend /build/build/libs/*.jar app.jar

EXPOSE 8302

# Spring Boot 가 jar 안의 static 리소스를 자동 서빙
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
