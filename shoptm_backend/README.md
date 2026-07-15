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
├── faq/          # FAQ 관리
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
- 상품 목록 (브랜드·카테고리·뱃지 필터, 정렬) — `@EntityGraph`로 카테고리·사이즈 재고를 한 쿼리에서 함께 조회 (N+1 방지)
- 상품 상세 (사이즈별 재고 관리)
- 이미지 파일 업로드 — 원본 파일명 대신 UUID로 파일명을 재생성하고, 확장자 화이트리스트(jpg/jpeg/png/gif/webp) 검사에 더해 실제 파일 앞부분 매직 바이트까지 확인해 확장자만 위장한 파일 업로드를 방지

### 장바구니 / 결제
- 장바구니 CRUD (본인 소유 항목만 접근 가능)
- 토스페이먼츠 결제 승인 API 연동
- 결제 승인 금액과 서버가 재계산한 주문 금액을 대조 후 일치할 때만 주문 생성 및 PAID 상태 전환
- 주문 생성 및 배송 정보 연동

### 리뷰 / 커뮤니티
- 리뷰 작성·수정·삭제, 중복 방지
- 게시판·QnA CRUD (댓글 포함)
- FAQ CRUD — 카테고리·정렬 순서 관리, 조회는 비로그인도 가능하고 등록/수정/삭제는 관리자만 가능 (`/api/admin/**` URL 패턴으로 차단)

### 관리자
- 회원·주문·상품·카테고리·리뷰·FAQ 관리
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

### 예외 메시지를 그대로 반환하는 컨트롤러가 더 있었음
- **문제**: 위와 같은 계열의 문제가 `TossController`(결제 승인 실패), `AnalyticsController`(방문 기록), `SizeRecommendController`(사이즈 추천), `ProductController.changeProductId`에도 남아있었고, `GlobalExceptionHandler`의 JSON 파싱 오류 핸들러도 Jackson 원본 파싱 메시지를 그대로 응답에 담고 있었음.
- **해결**: 동일하게 SLF4J 로거로 상세 내용은 서버 로그에만 남기고, 클라이언트에는 일반화된 메시지를 반환하도록 통일.

### 새 테이블 추가 시 DB 권한 누락으로 500 에러
- **문제**: FAQ 기능 추가 후 `faq` 테이블을 새로 만들었는데 `GET /api/faq` 호출 시 `permission denied for table faq`로 500 에러 발생.
- **원인**: 이 프로젝트 DB는 스키마 소유자(`postgres`)와 애플리케이션 접속 계정(`shoptm`)이 분리되어 있고, 스키마에 `ALTER DEFAULT PRIVILEGES`가 설정되어 있지 않아 새 테이블을 만들 때마다 `shoptm` 계정에 수동으로 권한을 부여해야 했음.
- **해결**: 처음엔 `faq` 테이블 하나에만 수동으로 GRANT했지만, 근본 원인을 스키마 차원에서 해결하기 위해 `ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES/SEQUENCES TO shoptm` 설정. 이후로는 `postgres` 계정으로 만드는 새 테이블·시퀀스에 `shoptm` 권한이 자동으로 부여됨 (실제로 테스트 테이블을 만들어 확인 후 삭제함).

### 운영 환경에 안전하지 않은 JPA 설정
- **문제**: `ddl-auto: update`로 운영 DB에 그대로 붙어있어 엔티티가 바뀌면 Hibernate가 스키마를 자동으로 조용히 변경할 수 있는 상태였고, `show-sql: true`로 모든 SQL이 로그에 계속 쌓이고 있었음.
- **원인**: 별도 환경 분리 없이 처음부터 단일 설정으로 운영 중이었음.
- **해결**: 스키마 변경은 이미 `shoptm.sql`을 직접 고치고 수동으로 반영하는 방식으로 운영 중이므로, `ddl-auto`를 `validate`로 바꿔 엔티티-스키마 불일치를 조용히 덮어쓰지 않고 기동 시 검증만 하도록 변경. `show-sql`은 `false`로 전환. 반영 전 로컬에서 별도 포트로 `validate` 모드 기동이 성공하는 것을 먼저 확인한 뒤 운영에 적용함.

### 상품 목록 조회 시 N+1 쿼리
- **문제**: `ProductResponseDto.from()`이 상품마다 `category.getName()`, `sizes` 컬렉션을 참조하는데 둘 다 `LAZY` 연관관계라서, 상품 30개를 조회하면 목록 쿼리 1번 + 카테고리/사이즈 조회가 상품마다 추가로 나가는 구조였음 (최대 61개 쿼리).
- **해결**: `ProductRepository`의 목록 조회 메서드들에 `@EntityGraph(attributePaths = {"category", "sizes"})`를 붙여서 한 번의 JOIN 쿼리로 함께 가져오도록 변경. 반영 후 실제 SQL 로그로 상품 30개 조회가 쿼리 1번으로 처리되는 것을 확인함.

### 업로드 파일 검증이 확장자만 확인
- **문제**: 이미지 업로드 시 파일명 확장자만 화이트리스트로 검사하고 있어서, 임의의 파일을 `.jpg`로 이름만 바꿔 업로드하면 그대로 통과되는 상태였음. (정적 리소스로만 서빙되어 원격 코드 실행 위험은 아니지만 실제 파일 검증은 아니었음)
- **해결**: `ProductService.validateImageContent()`를 추가해 파일 앞부분 매직 바이트(JPEG `FF D8 FF`, PNG `89 50 4E 47...`, GIF `47 49 46 38`, WEBP `RIFF...WEBP`)를 직접 확인하고, 확장자가 주장하는 형식과 실제 내용이 다르면 업로드를 거부하도록 변경.

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
