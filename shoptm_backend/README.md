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
- DB 비밀번호·JWT 시크릿·최초 관리자 비밀번호는 소스에 하드코딩하지 않고 환경변수(`DB_PASSWORD`, `JWT_SECRET`, `ADMIN_INIT_PASSWORD`)로 주입

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

### 테스트 의존성 누락으로 테스트 빌드 자체가 실패
- **문제**: `build.gradle`에 `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'`만 있고 JUnit Jupiter API·`@SpringBootTest`가 포함된 `spring-boot-starter-test`가 빠져 있어, 유일한 테스트 코드인 `ShopApplicationTests`조차 `org.junit.jupiter.api`를 찾지 못해 컴파일 단계에서 실패했음 (`./gradlew test` 실행 자체가 불가능한 상태).
- **해결**: `testImplementation 'org.springframework.boot:spring-boot-starter-test'` 추가. 실제 DB에 연결한 상태로 `./gradlew test`를 실행해 `contextLoads()`가 통과하는 것까지 확인함.

### 상품 목록 조회 시 N+1 쿼리
- **문제**: `ProductResponseDto.from()`이 상품마다 `category.getName()`, `sizes` 컬렉션을 참조하는데 둘 다 `LAZY` 연관관계라서, 상품 30개를 조회하면 목록 쿼리 1번 + 카테고리/사이즈 조회가 상품마다 추가로 나가는 구조였음 (최대 61개 쿼리).
- **해결**: `ProductRepository`의 목록 조회 메서드들에 `@EntityGraph(attributePaths = {"category", "sizes"})`를 붙여서 한 번의 JOIN 쿼리로 함께 가져오도록 변경. 반영 후 실제 SQL 로그로 상품 30개 조회가 쿼리 1번으로 처리되는 것을 확인함.

### 업로드 파일 검증이 확장자만 확인
- **문제**: 이미지 업로드 시 파일명 확장자만 화이트리스트로 검사하고 있어서, 임의의 파일을 `.jpg`로 이름만 바꿔 업로드하면 그대로 통과되는 상태였음. (정적 리소스로만 서빙되어 원격 코드 실행 위험은 아니지만 실제 파일 검증은 아니었음)
- **해결**: `ProductService.validateImageContent()`를 추가해 파일 앞부분 매직 바이트(JPEG `FF D8 FF`, PNG `89 50 4E 47...`, GIF `47 49 46 38`, WEBP `RIFF...WEBP`)를 직접 확인하고, 확장자가 주장하는 형식과 실제 내용이 다르면 업로드를 거부하도록 변경.

### 게시판/댓글/Q&A 수정·삭제가 로그인 없이, 남의 글도 가능
- **문제**: `SecurityConfig`가 `/api/boards/**`, `/api/comments/**`, `/api/qna/**`를 메서드 구분 없이 `permitAll()`로 열어둬서, 로그인하지 않은 상태로도 `PUT/DELETE`로 임의 게시글·댓글·Q&A를 수정·삭제할 수 있었음. 게다가 각 서비스(`BoardService`/`CommentService`/`QnaService`)의 작성·수정·삭제 메서드 어디에도 작성자 검증이 없어서, 로그인은 했더라도 URL의 id만 바꾸면 다른 회원의 글을 수정·삭제하거나(IDOR) 다른 회원 명의로 글을 작성할 수 있었음.
- **해결**: 조회(GET)만 `permitAll`로 남기고 나머지 메서드는 `authenticated()`로 폴백되도록 `SecurityConfig`를 `HttpMethod.GET` 기준으로 좁힘. 이미 소유자 검증이 일관되게 적용돼 있던 `CartService`와 동일한 패턴으로, 세 서비스의 작성/수정/삭제 메서드에 `SecurityUtil.checkOwnerOrAdmin()`을 추가. 관리자 전용 삭제 엔드포인트(`/api/admin/boards/{id}` 등)는 같은 서비스 메서드를 공유하며 `checkOwnerOrAdmin`이 관리자를 통과시키므로 그대로 동작.

### 찜 목록에 소유자 검증이 통째로 빠짐
- **문제**: `WishlistService`의 조회·추가·삭제 어디에도 `SecurityUtil.checkOwnerOrAdmin()`이 없어서, 로그인한 회원이 `/api/members/{memberId}/wishlist`의 `memberId`만 다른 회원 것으로 바꾸면 그 회원의 찜 목록을 보고, 마음대로 추가·삭제할 수 있었음(IDOR). 같은 구조인 `CartService`는 모든 메서드에 이 검증이 이미 붙어 있어 Wishlist만 빠뜨린 것이 명확했음.
- **해결**: `getWishlist`/`addWishlist`/`removeWishlist` 모두 회원을 조회한 뒤 `SecurityUtil.checkOwnerOrAdmin(member.getEmail())`을 거치도록 추가. `addWishlist`는 "이미 찜한 상품" 여부를 확인하기 전에 소유자 검증부터 하도록 순서도 바꿔, 접근 권한이 없는 요청에 목록 상태를 흘리지 않게 함.

### 관리자 계정 비밀번호가 소스에 하드코딩 + 로그에 평문 출력
- **문제**: `DataInitializer`가 최초 기동 시 관리자 계정이 없으면 `admin@forme.com` / `1234`로 자동 생성하는데, 비밀번호 `"1234"`가 소스에 그대로 박혀 있었고(공개 저장소라 누구나 확인 가능) 생성 로그에도 `"관리자 계정 생성: {} / 1234"` 형태로 평문 비밀번호를 그대로 남기고 있었음.
- **해결**: `DB_PASSWORD`/`JWT_SECRET`과 동일하게 관리자 초기 비밀번호도 필수 환경변수(`ADMIN_INIT_PASSWORD`)로만 주입받도록 변경, 값이 없으면 기동 자체가 실패함(placeholder 미해석 예외). 로그에서도 비밀번호 값을 제거. 이미 생성돼 있던 기존 관리자 계정에는 영향 없음(계정이 없을 때만 실행되는 분기라 비밀번호는 그대로 유지됨).

### 리뷰 수정·삭제 소유자검증 누락 + 임의 주문ID로 "구매 인증" 리뷰 위조 가능
- **문제**: `ReviewService.updateReview`/`deleteReview`에 소유자 검증이 없어서 로그인한 사용자가 `reviewId`만 바꾸면 남의 리뷰를 수정·삭제할 수 있었음(IDOR). 게다가 `createReview`는 `dto.getOrderId()`가 실제로 그 회원의 주문인지, 그 주문에 리뷰 대상 상품이 포함돼 있는지 전혀 검증하지 않아서, 남의 주문 id를 넣거나 자기 주문이라도 사지 않은 상품에 걸어서 "구매 확인(orders 연결)" 표시가 붙은 리뷰를 위조할 수 있었음. `ReviewController.createReview`가 모든 예외를 `catch(Exception)`으로 삼켜 `500`으로만 응답하고 있어서, 이 검증들을 추가해도 클라이언트가 원인을 구분할 수 없는 상태이기도 했음.
- **해결**: `CartService`와 동일한 패턴으로 작성·수정·삭제에 `SecurityUtil.checkOwnerOrAdmin()` 추가. `createReview`에 `orderId`가 있을 때 그 주문이 실제로 해당 회원 소유인지, 리뷰 대상 상품이 그 주문에 포함돼 있는지 검증하는 로직 추가. `ReviewController`의 불필요한 `catch(Exception)`을 제거해 `GlobalExceptionHandler`가 검증 실패를 `400`/`403`으로 정확히 응답하도록 정리(프론트는 이미 `orderId`를 항상 `null`로 보내고 있어 이 경로는 API 직접 호출 방어용).

### 주문 생성 시 재고 차감이 동시 요청에 취약 + 관리자 취소 시 재고 미복구
- **문제**: `OrderService.createOrder`가 재고 확인(`product.getStock() < quantity`)과 차감(`product.setStock(...)`)을 조회 후 다시 쓰는(read-then-write) 방식으로 처리해서, 같은 상품에 동시에 여러 주문이 들어오면 둘 다 "재고 충분" 판정을 통과해 실제 재고보다 많이 팔리는(오버셀) 레이스 컨디션이 있었음. 또한 회원 본인이 취소하면(`cancelOrder`) 재고를 복구하지만, 관리자가 주문 상태를 변경하는 `updateOrderStatus`는 상태값을 검증도 없이 그대로 저장하기만 해서, 관리자가 주문을 CANCELLED로 바꿔도 이미 차감된 재고가 복구되지 않고 영구히 사라지는 문제가 있었음.
- **해결**: `ProductRepository`에 `"재고 >= 주문수량"`일 때만 적용되는 조건부 원자적 UPDATE(`decreaseStockIfAvailable`)를 추가해 재고 확인과 차감을 하나의 DB 연산으로 처리. `updateOrderStatus`에는 상태값을 `PENDING/PAID/PREPARING/SHIPPED/DELIVERED/CANCELLED`로 검증하는 화이트리스트를 추가하고, 취소가 아니던 주문이 CANCELLED로 바뀔 때만(이미 취소된 주문을 다시 취소해도 중복 복구되지 않도록) `cancelOrder`와 동일하게 재고를 복구하도록 수정. 재고 5개인 상품에 동시에 10건을 주문해 정확히 5건만 성공하고 재고가 0으로 남는 것, 관리자가 취소하면 재고가 1 복구되고 같은 주문을 다시 취소해도 중복 복구되지 않는 것, 잘못된 상태값은 거부되는 것을 실제로 확인함.

### 관리자 대시보드/리뷰/QnA/찜 목록에 새로운 N+1
- **문제**: `Review`/`Qna`/`Wishlist`의 `member`/`product`(찜은 `product`, `product.category`까지)가 LAZY 관계인데, 각 `ResponseDto.from()`이 매 항목마다 `member.getName()`/`product.getName()` 등을 읽어서 목록 조회 API(내 리뷰, 상품별 리뷰, 관리자 전체 리뷰, Q&A 목록, 찜 목록)를 호출할 때마다 항목 수만큼 추가 쿼리가 나가고 있었음. 관리자 대시보드(`AdminService.getDashboard()`)는 브랜드별 매출 집계를 위해 전체 주문의 `orderItems`와 그 안의 `product`를 순회해서 주문 수에 비례해 쿼리가 늘었고, 최근 주문 5건 조회를 위해 전체 주문 목록을 별도로 다시 조회하는 중복 쿼리, 판매중/품절 상품 집계를 위해 활성 상품 목록을 두 번 조회하는 중복 쿼리도 있었음.
- **해결**: 세 리포지토리의 목록 조회 메서드에 `@EntityGraph`로 필요한 연관관계를 함께 조회하도록 추가(리뷰/Q&A는 `member`+`product`, 찜은 `product`+`product.category`). 관리자 전체 리뷰 조회는 `findAll()` 대신 `@EntityGraph`를 붙일 수 있는 `findAllByOrderByCreatedAtDesc()`로 교체. `OrderRepository`에 `member`/`orderItems`/`orderItems.product`를 한 번에 JOIN FETCH하는 대시보드 전용 조회를 추가해 기존 두 번의 전체 주문 조회를 하나로 합치고, 활성 상품 목록도 한 번만 조회해 재사용하도록 정리. 테스트 계정으로 리뷰·Q&A·찜을 5건씩 만든 뒤 SQL 로그로 확인한 결과, 각 목록 조회가 항목 수와 무관하게 JOIN이 포함된 쿼리 1번으로 처리됐고, 대시보드 전체 호출도 쿼리 4번(회원 목록/주문+연관관계 JOIN FETCH/상품 카운트/활성 상품 목록)으로 끝나는 것을 확인함.

### 상품 삭제가 주석과 달리 실제로는 하드 삭제
- **문제**: `ProductService.deleteProduct()`의 주석은 "소프트 삭제 - DB에서 실제 삭제 안 하고 is_active = false로 변경"이라고 되어 있는데, 실제 코드는 바로 옆에 "DB에서 완전 삭제"라는 반대되는 주석과 함께 `productRepository.delete(product)`로 하드 삭제하고 있었음. `getProduct()`는 이미 `!product.getIsActive()`면 "삭제된 상품입니다"를 던지도록 소프트 삭제를 전제로 짜여 있어서, 이 경로는 사실상 죽은 코드였음. 상품을 하드 삭제하면 그 상품을 참조하는 과거 주문(`order_items`)·리뷰·Q&A·찜이 고아 참조로 남거나(FK 제약이 있다면 삭제 자체가 실패) 주문 내역에서 상품 정보가 사라질 위험이 있었음.
- **해결**: `productRepository.delete(product)`를 `product.setIsActive(false)`로 교체해 실제로 소프트 삭제되도록 수정(이미 있던 `isActive` 필드와 전 구간의 "활성 상품만 조회" 패턴을 그대로 활용). 상품에 주문을 하나 걸어둔 뒤 관리자로 삭제해서, DB 행은 `is_active=false`로 그대로 남고, 목록/단건 조회에서는 정상적으로 빠지며, 기존 주문은 상품명·이미지가 그대로 표시되는 것을 실제로 확인함.


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
