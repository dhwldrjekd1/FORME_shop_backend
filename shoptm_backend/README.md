# FORME - 멀티브랜드 쇼핑몰 포트폴리오 (Backend)

> 리바이스, 칼하트, 빈폴, 딕키즈 4개 브랜드를 하나의 플랫폼에서 운영하는 멀티브랜드 쇼핑몰 포트폴리오입니다.

---

## 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 프로젝트명 | FORME 멀티브랜드 쇼핑몰 |
| 개발 기간 | 2026.03 |
| 개발자 | 최동윤 |
| 개발 인원 | 1인 (풀스택) |
| 배포 환경 | Ubuntu Server + Spring Boot |
| 접속 URL | https://forme.dyy.kr |

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Framework | Spring Boot 3 |
| ORM | Spring Data JPA |
| DB | PostgreSQL |
| 빌드 도구 | Gradle |
| 인증 | Spring Security + JWT |
| 결제 | 토스페이먼츠 API |
| 정적 리소스 | Vue 3 dist → Spring Boot static 내장 서빙 |
| 배포 | Docker / Ubuntu Server |

---

## 프로젝트 구조

```
src/main/java/com/forme/shop/
├── member/       # 회원가입·로그인·마이페이지
├── product/      # 상품·사이즈 관리
├── category/     # 카테고리 관리
├── cart/         # 장바구니
├── order/        # 주문 생성·관리
├── delivery/     # 배송 정보
├── payment/      # 토스페이먼츠 결제 연동
├── review/       # 리뷰 CRUD
├── wishlist/     # 위시리스트
├── board/        # 커뮤니티 게시판·댓글
├── qna/          # QnA 게시판
├── admin/        # 관리자 대시보드
├── analytics/    # 페이지뷰 방문 분석
├── settings/     # 사이트 설정
├── size/         # AI 사이즈 추천
├── common/       # 파일 업로드, 공통 예외 처리
└── config/       # Security, JWT, Web 설정
```

---

## 구현 기능

### 회원 / 인증
- 회원가입 / 로그인 / 로그아웃 (JWT)
- AccessToken 발급 (Spring Security 필터 기반 인증)
- 토큰은 응답 바디가 아니라 httpOnly 쿠키로만 전달 — 자바스크립트가 읽을 수 없어 XSS로 인한 토큰 탈취 방지
- 로그아웃 시 토큰 즉시 무효화 — jti 기반 서버 블랙리스트에 등록해, 만료 전이라도 같은 토큰 재사용 차단
- 리소스 소유자 검증 — 회원정보·장바구니·주문 API는 서비스 계층에서 로그인한 본인(또는 관리자)의 데이터인지 대조 후 처리 (SecurityUtil)
- DB 비밀번호·JWT 시크릿은 소스에 하드코딩하지 않고 환경변수(`DB_PASSWORD`, `JWT_SECRET`)로 주입

### 상품
- 상품 목록 (브랜드·카테고리·뱃지 필터, 정렬)
- 상품 상세 (사이즈별 재고 관리)
- 이미지 파일 업로드 — 원본 파일명 대신 UUID로 파일명을 재생성하고 허용된 확장자(jpg/jpeg/png/gif/webp)만 통과시켜 경로 조작·임의 파일 업로드 방지

### 장바구니 / 결제
- 장바구니 CRUD (본인 소유 항목만 접근 가능)
- 토스페이먼츠 결제 승인 API 연동
- 결제 승인 금액과 서버가 재계산한 주문 금액을 대조 후 일치할 때만 주문 생성 및 PAID 상태 전환
- 주문 생성 및 배송 정보 연동

### 리뷰 / 커뮤니티
- 리뷰 작성·수정·삭제, 중복 방지
- 게시판·QnA CRUD (댓글 포함)

### 관리자
- 회원·주문·상품·카테고리·리뷰 관리
- 방문자 분석 (페이지뷰 트래킹)
- 사이트 설정 관리

### 사이즈 추천
- 키·몸무게 기반 사이즈 추천 로직 (비회원 포함)

### 성능
- 정적 리소스 캐싱 정책 세분화 (`WebConfig`) — 해시가 붙는 빌드 산출물(`assets/**`)은 1년 캐싱, 이미지는 1일 캐싱, SPA 진입점(`index.html`)은 매번 재검증
- Spring Security 기본 헤더가 정적 리소스까지 `no-store`로 덮어쓰던 문제 수정 — 캐싱 정책을 리소스 핸들러에서 세분화 관리

---

## 트러블슈팅

### 결제 금액 검증 로직에 남아있던 우회 경로
- **문제**: 토스페이먼츠 결제 승인 응답에 `totalAmount`가 없는 경우, 클라이언트가 요청 시 보낸 `amount`를 그대로 신뢰 가능한 결제 금액으로 사용하는 폴백 로직이 있었음.
- **원인**: PG사 응답이 비정상인 상황을 대비한 방어 코드였지만, 결과적으로 정상 승인 응답이 아니어도 클라이언트가 보낸 임의의 금액이 주문 생성에 그대로 쓰일 수 있는 우회 경로가 됨.
- **해결**: `totalAmount`가 없으면 클라이언트 값으로 대체하지 않고 결제 승인 자체를 실패 처리(fail-closed)하도록 변경 (`TossController`).

### 예외 처리 시 서버 내부 정보 노출
- **문제**: 처리되지 않은 예외 발생 시 500 응답 바디에 예외 클래스명·메시지를 그대로 담아 반환했고, 여러 컨트롤러에서 `printStackTrace()`로만 콘솔에 출력하고 있었음.
- **원인**: 개발 중 빠른 디버깅을 위해 넣어둔 코드가 정리되지 않고 남아있었음.
- **해결**: SLF4J 로거로 교체해 서버 로그에 상세 스택트레이스를 남기고, 클라이언트에는 내부 구현이 드러나지 않는 일반화된 메시지만 반환하도록 변경 (`GlobalExceptionHandler`, `ReviewController`, `ProductController`).

---

## 빌드 및 배포

```bash
# JAR 빌드
./gradlew bootJar

# Docker 실행
docker-compose up -d
```

> Vue 3 프론트엔드 dist 파일은 `src/main/resources/static/`에 위치하며 Spring Boot가 함께 서빙합니다.

---

## 연관 레포지토리

| 구분 | 링크 |
|------|------|
| Frontend | [FORME_shop_frontend](https://github.com/dhwldrjekd1/FORME_shop_frontend) |
