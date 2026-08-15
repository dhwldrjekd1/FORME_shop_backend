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

### 배송 조회 API에 소유자 검증 누락
- **문제**: `GET /api/orders/{orderId}/delivery`(일반회원용 배송 조회)가 `orderId`로 배송 정보를 바로 조회해서 반환할 뿐, 그 주문이 로그인한 회원 본인 것인지 전혀 확인하지 않았음. 로그인만 하면 orderId를 바꿔가며 다른 회원의 배송 상태·운송장 번호를 볼 수 있었음(IDOR). 같은 패키지의 `OrderService.getOrder()`는 이미 이 검증이 있는 것과 대조됨.
- **해결**: `DeliveryService.getDelivery()`에서 배송 정보를 조회하기 전에 먼저 주문을 조회해 `SecurityUtil.checkOwnerOrAdmin(orders.getMember().getEmail())`을 거치도록 추가. 실제로 주문 하나를 만들고 관리자가 배송 정보를 등록한 뒤, 다른 회원 계정으로 조회를 시도하면 `403`, 주문 본인과 관리자는 `200`으로 정상 조회되는 것을 확인함.

### (교차검증에서 발견) 재고 원자적 UPDATE 도입 후 등급 자동 승급이 저장되지 않음
- **문제**: 위 재고 동시성 수정에서 쓴 `@Modifying(clearAutomatically = true)`는 실행 직후 영속성 컨텍스트 전체를 비운다. `createOrder()`는 맨 앞에서 `member`를 조회해두고 나중에 `updateMemberGrade(member)`에서 `member.setGrade(newGrade)`로 등급을 바꾸는데, 그 사이에 있는 `decreaseStockIfAvailable()` 호출이 `member`를 detach시켜버려서 더티 체킹이 더 이상 적용되지 않았음 — `member.setGrade()`를 호출해도 DB에는 반영되지 않는 상태였음. 실제로 50만원 이상 주문을 넣어 확인한 결과 등급이 BRONZE에서 전혀 바뀌지 않는 것을 재현함(수정 커밋에는 이 사이드이펙트를 놓쳤었고, 독립적인 교차검증 과정에서 발견함).
- **해결**: `updateMemberGrade()`에서 `member.setGrade(newGrade)` 뒤에 `memberRepository.save(member)`를 명시적으로 호출하도록 추가. detach된 엔티티도 id가 있으면 `save()`(내부적으로 `merge()`)가 정상적으로 병합·저장함. 같은 시나리오(50만원 이상 주문)로 재확인해 등급이 BRONZE → SILVER로 정상 반영되는 것을 확인함.

### 토스 결제 승인 후 주문 생성이 실패하면 카드는 결제됐는데 아무 기록도 남지 않음 (2026.08.10)
- **문제**: `TossController.confirmPayment()`는 토스 결제 승인 API를 호출해 카드 결제를 그 자리에서 확정시키지만, `paymentKey`/승인 금액 등을 어디에도 저장하지 않고 그대로 응답만 돌려주는 단순 프록시였음. 이후 프론트가 이어서 호출하는 `POST /members/{id}/orders`(`OrderService.createOrder`)가 재고 부족·상품 삭제 등 어떤 이유로든 실패하면(해당 메서드는 `@Transactional`이라 실패 시 재고 차감까지 전부 롤백됨), 카드는 이미 결제됐는데 그 사실이 DB 어디에도 남지 않고, 환불(취소)도 자동으로 이뤄지지 않는 상태가 됐음.
- **원인**: 결제 승인(PG사 쪽 상태 변경)과 주문 생성(우리 DB 쪽 상태 변경)이 서로 다른 두 트랜잭션으로 완전히 분리되어 있는데, 그 사이를 연결하는 기록이나 실패 시 보상(compensating) 처리가 전혀 없었음.
- **해결**: 결제 승인 성공 시점에 즉시 `payments` 테이블(신규)에 기록을 남기도록 변경(`PaymentService.recordConfirmed`). 응답에 `paymentKey`를 포함시켜 프론트가 주문 생성 요청에 그대로 실어 보내도록 하고, `OrderService.createOrder`는 이 `paymentKey`가 있으면: 주문 생성 성공 시 결제 기록을 해당 주문에 연결(`LINKED`)하고, 실패 시 `PaymentService.refundAndMarkFailed()`(별도 트랜잭션 `REQUIRES_NEW`로 실행되어 주문 생성 트랜잭션이 롤백돼도 함께 롤백되지 않음)가 토스 취소 API를 호출해 자동 환불하고 `REFUNDED`로 남기며, 환불 호출 자체가 실패하면 `REFUND_FAILED`로 남겨 수동 확인이 가능하게 함. 사용자에게는 "결제가 자동으로 취소되었습니다" 또는 "환불 실패, 고객센터 문의" 메시지가 명확히 전달됨. 임시 포트로 별도 기동한 서버에 실제 관리자 계정으로 로그인해, (1) 존재하지 않는 상품으로 주문을 실패시켜 가짜 결제가 토스 테스트 API에서 `NOT_FOUND_PAYMENT`로 환불도 실패하며 `REFUND_FAILED`로 남는 것, (2) 정상 상품으로 주문을 성공시켜 결제 기록이 실제 생성된 주문에 `LINKED`로 연결되고 재고가 정확히 차감되는 것 둘 다 실제 DB 상태로 확인한 뒤 테스트 데이터를 정리함. `ddl-auto: validate` 환경이라 `payments` 테이블은 `shoptm.sql`과 실제 운영 DB에 직접 반영함.

### (교차검증에서 발견) 위 환불 로직 자체에 남아있던 5가지 결함
- **결제 금액 검증이 여전히 클라이언트가 보낸 값을 신뢰**: `paidAmount`를 요청 본문의 값 그대로 썼는데, 이 값은 정상 플로우에서는 서버가 방금 돌려준 값을 그대로 되돌려보내는 것뿐이지만, `/orders`를 직접 호출하면 실제 결제 금액과 무관하게 조작해서 보낼 수 있었음(소액만 결제하고 훨씬 비싼 주문을 승인시키는 위변조 가능). → `paymentKey`가 있으면 요청 본문의 `paidAmount`를 무시하고, 서버가 이미 저장해 둔 `Payment.amount`(진짜 승인 금액)로만 비교하도록 변경. 위조된 `paidAmount`를 실제로 보내봐도 서버가 저장된 진짜 금액 기준으로 정확히 판정하는 것을 확인함.
- **환불이 성공/실패해도 상태를 남기는 로직이 멱등하지 않음**: 같은 결제에 대해 `refundAndMarkFailed`가 두 번 이상 불리면(동시에 실패한 두 요청 등) 두 번째 호출이 토스 취소 API를 다시 부르고, 이미 `REFUNDED`였던 상태를 `REFUND_FAILED`로 잘못 덮어쓸 수 있었음. → 이미 `REFUNDED`/`REFUND_FAILED`인 결제는 다시 처리하지 않고 즉시 반환하도록 가드 추가.
- **결제 승인 기록 저장 자체가 동시 요청에 취약**: `recordConfirmed`의 "있는지 확인 후 없으면 저장"이 원자적이지 않아, 승인 콜백이 거의 동시에 두 번 들어오면 두 번째 저장이 DB unique 제약 위반 예외를 던지고, 이게 컨트롤러의 넓은 `catch(Exception)`에 걸려 실제로는 결제가 잘 기록됐는데도 사용자에게 "결제 승인 처리 중 오류" 라고 잘못 안내될 수 있었음. → `saveAndFlush`로 즉시 저장을 실행해 그 자리에서 제약 위반을 잡아내고, 이미 기록이 있는 정상 상황으로 처리하도록 수정.
- **주문 저장 이후의 부가 처리 실패가 성공한 주문 자체를 되돌림**: 결제 연결(`markLinked`)이나 등급 자동 승급(`updateMemberGrade`)에서 예외가 나면, 이미 저장된 주문까지 통째로 롤백되면서(같은 트랜잭션) 환불도 트리거되지 않는 상태로 남을 수 있었음(카드는 결제되고 실제로 재고도 나갔는데 주문만 사라지는 최악의 경우). → 두 후처리를 각각 별도 `try/catch`로 감싸 실패해도 로그만 남기고 이미 완료된 주문 생성 자체에는 영향을 주지 않도록 수정.
- **재고부족 등 우리가 던진 안전한 메시지가 아닌 예외의 원문이 그대로 노출**: 실패 시 사용자에게 보여주는 메시지에 `e.getMessage()`를 무조건 그대로 넣고 있어서, 재고부족 같은 의도된 메시지뿐 아니라 예상 못한 내부 예외(NPE, DB 오류 등)의 원문까지 그대로 노출될 수 있었음(다른 곳에서는 `GlobalExceptionHandler`가 이런 경우 일반화된 메시지로 걸러주는데, 이 경로는 그 전에 먼저 잡아서 우회하고 있었음). → 우리가 직접 던진 `IllegalArgumentException`(재고부족 등 안전한 문구)만 그대로 노출하고, 그 외 예기치 못한 예외는 서버 로그에만 상세히 남기고 사용자에게는 일반화된 문구만 보여주도록 수정.

(동시에 같은 `paymentKey`로 주문 생성이 두 번 들어오면 각각 별도 주문이 만들어질 수 있는 문제도 이 교차검증에서 함께 발견됐지만, 이는 성격상 아래 "주문 생성 멱등성" 항목 그 자체라 여기서 손대지 않고 별도로 다룸)

### 같은 결제로 주문 생성을 동시에 두 번 요청하면 주문이 두 개 만들어짐 (2026.08.11)
- **문제**: 위 환불 로직에서 결제 승인 여부만 확인(`findConfirmed`)하고 실제로 "이 결제는 이제부터 내가 쓴다"고 표시해두는 절차가 없었음. 같은 `paymentKey`로 거의 동시에 두 번 `POST /orders`가 들어오면(더블클릭, 느린 응답 후 자동 재시도) 두 요청 모두 결제가 `CONFIRMED` 상태인 것을 확인하고 각자 주문을 만들어, 카드는 한 번 결제됐는데 주문이 두 건(재고도 두 번) 생기는 문제가 있었음.
- **해결**: 재고 차감(`decreaseStockIfAvailable`)과 동일한 패턴으로, `payments` 테이블에 `WHERE status='CONFIRMED'`일 때만 `PROCESSING`으로 바꾸는 조건부 원자적 UPDATE(`claimIfConfirmed`)를 추가해 "이 결제를 쓸 권리"를 하나의 요청만 가져가도록 함. 선점에 실패한 요청은, 이미 그 결제로 주문이 만들어져 있으면(재시도 상황) 새로 만들지 않고 원래 주문을 그대로 돌려주고(진짜 멱등성), 아직 처리 중이거나 이미 취소된 결제면 명확한 안내와 함께 거부함. 실제로 같은 `paymentKey`로 요청 5개를 진짜 동시에 쏴서 정확히 1건만 성공하고 재고도 1개만 차감되는 것, 성공 후 재시도하면 새 주문 없이 원래 주문 그대로 반환되는 것을 확인함.
- **교차검증 중 직접 재현한 설계 실수 두 가지**:
  - 처음엔 이 선점(claim)을 주문 생성과 같은 트랜잭션 안에서 실행했는데, 그 상태로 두면 나중에 실패 시 호출하는 환불 처리(`refundAndMarkFailed`, 별도 트랜잭션 `REQUIRES_NEW`)가 **같은 결제 행에 락을 걸려다가 아직 끝나지 않은 바깥 트랜잭션과 서로를 기다리며 자기 자신과 교착 상태에 빠질 뻔함**. 선점 자체도 `REQUIRES_NEW`로 분리해 즉시 커밋·락 해제되도록 고쳐서 해결(실제로 고치기 전 상태를 재현해봤다면 요청이 응답 없이 멈췄을 상황).
  - "주문 생성 성공 후 결제 연결(`markLinked`)이 실패해도 이미 성공한 주문을 롤백시키면 안 된다"는 논리로 `markLinked`도 처음엔 `REQUIRES_NEW`로 분리했는데, 실제로 붙여서 테스트해보니 **정상 케이스가 전부 깨짐** — 그 시점엔 주문이 아직 커밋 전이라, 별도 트랜잭션에서는 그 주문 행이 안 보여서 FK 제약 위반이 났음. `markLinked`는 바깥 트랜잭션에 그대로 합류(`REQUIRED`)하도록 되돌림. (남는 한계: 아주 드물게 `markLinked`의 커밋 자체가 실패하면 이미 성공한 주문까지 함께 롤백될 수 있는 이론적 위험은 완전히 없애지 못했고, try/catch로 일반적인 실패만 흡수함 — 실제로 발생 가능성이 매우 낮고, 완전히 없애려면 아웃박스 패턴 등 더 큰 구조 변경이 필요해 지금 범위에서는 보류함)

### Q&A 비밀글이 서버·프론트 어디서도 실제로 안 가려짐 (2026.08.11)
- **문제**: `Qna`의 `isSecret`(작성자와 관리자만 열람 가능하다는 필드)이 응답 DTO(`QnaResponseDto.from`)에서 전혀 반영되지 않아, `content`/`answer`가 항상 그대로 내려갔음. `GET /api/qna`, `GET /api/qna/{id}`는 `SecurityConfig`에서 `permitAll`이고 `GET /api/products/{id}/qna`도 `/api/products/**` 전체가 `permitAll`이라, **비로그인 상태로도** 비밀글 전체 목록·내용을 그대로 볼 수 있었음. `GET /api/members/{memberId}/qna`("내 문의" 목록)는 인증은 필요했지만 소유자 검증이 아예 없어서, 로그인한 아무 회원이나 URL의 `memberId`만 다른 회원 것으로 바꾸면 그 회원의 문의 목록(비밀글 포함)을 통째로 가져올 수 있었음(IDOR). 프론트(`QnaView.vue`, `DetailView.vue`의 Q&A 탭)도 잠금 아이콘만 보여줄 뿐 실제로는 `content`/`answer`를 그대로 렌더링하고 있어서, 백엔드가 막아도 프론트가 이미 다 노출하는 구조였음.
- **해결**: `QnaResponseDto.from(qna, canViewSecret)`으로 오버로드를 나눠, 목록/단건 조회 경로(`getAllQna`/`getProductQna`/`getQna`)는 요청자가 작성자 본인(`SecurityUtil.getCurrentEmail()`이 `qna.getMember().getEmail()`과 일치) 또는 관리자(`SecurityUtil.isAdmin()`)일 때만 실제 `content`/`answer`를 내려주고, 그 외에는 `null`로 가림(제목·잠금 여부 등 목록에 필요한 메타 정보는 그대로 노출해 기존 UX 유지). 글쓴이 본인/관리자에게만 응답하는 경로(작성 직후, 수정, 관리자 답변 등)는 검증이 이미 끝난 뒤라 기존처럼 가리지 않고 그대로 반환. `getMyQna`에는 `SecurityUtil.checkOwnerOrAdmin()`을 추가해 본인(또는 관리자)만 호출 가능하게 함. 프론트 두 곳은 `content == null`이면 "비밀글입니다. 작성자와 관리자만 볼 수 있습니다"로 안내하도록 수정. 회원가입으로 테스트 계정 두 개를 만들어 A가 비밀글을 쓰고, 익명/B(비작성자)/A 본인/관리자 네 가지 관점으로 실제 API를 호출해 A 본인과 관리자만 내용이 보이고 나머지는 `null`인 것, B가 A의 "내 문의" 목록을 호출하면 403인 것, 일반(비밀 아닌) 글은 여전히 누구나 정상적으로 보이는 것까지 확인 후 테스트 데이터 정리. 독립 교차검증 에이전트가 관리자 대시보드/CSV 내보내기/actuator 등 코드베이스 전체에서 다른 노출 경로가 없는지도 별도로 훑어 확인함.

### 관리자가 회원을 강퇴(또는 본인 탈퇴)해도 이미 발급된 로그인 토큰이 최대 24시간 그대로 유효함 (2026.08.12)
- **문제**: `MemberService.banMember()`/`withdraw()`는 `isActive`만 `false`로 바꿀 뿐, 그 회원이 이미 들고 있는 JWT를 무효화하는 절차가 없었음. `JwtFilter`는 서명·만료만 확인하고 `isActive`를 다시 조회하지 않아서(매 요청마다 DB를 안 보는 무상태 구조), 관리자가 방금 강퇴한 회원이 `jwt.expiration`(24시간) 동안 계속 로그인 상태로 주문·활동이 가능했음. `verifySession()`(새로고침 시 서버 재검증)도 여기엔 도움이 안 됨 — 그건 클라이언트가 스스로 재검증을 "시도"할 때만 동작하고, 강퇴된 사용자가 계속 활동 중이면 애초에 그 재검증 자체가 트리거될 일이 없음.
- **해결**: 기존에 로그아웃 때 쓰던 토큰 단위 블랙리스트(`TokenBlacklistService`, jti로 개별 토큰 폐기)와 별개로, **회원(email) 단위 전체무효화**를 추가함 — `"이 시각 이전에 발급된 토큰은 전부 무효"`라는 기준 시각을 회원별로 저장하고, `JwtFilter`가 토큰의 발급 시각(`iat`)이 그 기준보다 이전이면 인증 처리하지 않도록 함(재고 차감 때 물량을 세션별로 추적하지 않고 "지금 재고"만 원자적으로 확인하는 것과 같은 발상 — 토큰을 세션별로 추적하는 대신 "이 순간 이후 것만 유효"라는 경계선 하나로 전부 막음). `banMember`/`withdraw` 둘 다 `isActive=false` 처리와 함께 이 무효화를 호출.
- **교차검증 중 발견해 추가로 고친 것**: 이 무효화 상태를 메모리에만 두면 서버가 재시작될 때마다(이 프로젝트는 배포할 때마다 재시작함) 강퇴 기록 자체가 사라져 버린다는 걸 교차검증에서 지적받음 — 그대로 뒀다면 이번 수정이 정작 실제 운영에서는 재배포 한 번에 무력화됐을 것. `Member`에 `deactivatedAt` 컬럼을 추가해 DB에도 같이 저장하고, 서버 기동 시(`TokenBlacklistInitializer`, 톰캣이 요청을 받기 전 단계인 `@PostConstruct`에서 실행) 비활성 회원 목록을 읽어 메모리 상태를 복원하도록 함. 실제로 회원을 강퇴한 뒤 **테스트 서버를 통째로 재시작**해서, 재시작 전에 발급된 옛 세션이 재시작 후에도 여전히 401로 막히는 것과 강퇴 안 된 계정은 재시작 후에도 영향이 없는 것을 확인함. `ddl-auto: validate` 환경이라 `deactivated_at` 컬럼은 운영 DB와 `shoptm.sql`에 직접 반영함.
- **남는 한계**: `@PostConstruct`로 옮겨도 Spring Boot 내장 톰캣이 열리는 시점을 완전히 늦추지는 못해서, 서버가 막 재시작된 아주 짧은 순간에는 이론적으로 복원이 끝나기 전에 요청이 들어올 여지가 남아있음(이 프로젝트 트래픽 규모에서는 사실상 무시 가능). 완전히 없애려면 서버 시작 자체를 지연시키는 구조 변경이 필요해 지금 범위에서는 보류함. 또한 로그인 처리(비밀번호 검증) 도중에 정확히 그 회원이 강퇴되는 극히 좁은 타이밍이 겹치면, 강퇴 기준 시각보다 뒤에 발급된 새 토큰이 하나 새어나갈 수 있는 이론적 레이스도 있음 — 발생 확률이 매우 낮고 그다음 강퇴/재검증 때 정리되므로 지금은 손대지 않음.

### 주문 취소 시 재고 복구가 원자적이지 않아 동시 취소 시 재고가 유실될 수 있음 (2026.08.12)
- **문제**: `cancelOrder()`(회원 본인 취소)와 `updateOrderStatus()`(관리자, CANCELLED로 변경 시)의 재고 복구가 둘 다 `item.getProduct().setStock(item.getProduct().getStock() + item.getQuantity())` 같은 조회 후 다시 쓰는(read-then-write) 방식이었음. 재고 차감 쪽은 오버셀 방지를 위해 이미 원자적 UPDATE로 고쳐져 있었는데(`decreaseStockIfAvailable`), 복구 쪽에는 같은 처리가 빠져 있었던 것. 같은 상품이 포함된 두 주문이 동시에 취소되면, 둘 다 취소 전 재고값을 읽어 각자 더해서 쓰다가 한쪽이 다른 쪽 결과를 덮어써 복구분 일부가 조용히 사라질 수 있었음.
- **해결**: 재고 차감과 동일한 패턴으로 원자적 UPDATE(`ProductRepository.increaseStock`, `stock = stock + :quantity`)를 추가해 두 취소 경로 모두 이걸로 재고를 복구하도록 변경.
- **직접 재현해서 고친 함정 두 가지**: (1) 재고 복구가 원자적 UPDATE라 영속성 컨텍스트를 비우는데(`clearAutomatically=true`), 그 앞에서 `save()`만으로 주문 상태를 "CANCELLED"로 바꿔뒀더니 — `save()`는 즉시 flush하지 않고 트랜잭션 커밋 시점까지 미룰 수 있어서, flush되기 전에 컨텍스트가 비워지며 상태 변경 자체가 조용히 유실됨(재고는 정상 복구됐는데 주문 상태만 PAID로 남는 걸 직접 재현해서 확인). `saveAndFlush`로 즉시 반영하도록 고침. (2) 관리자 쪽에서 응답 DTO를 재고 복구 뒤에 조립했더니 `Orders.member`가 LAZY라서 컨텍스트가 비워진 뒤 처음 접근하는 순간 `LazyInitializationException: no session`으로 500 에러가 남(실제로 재현). 그때그때 "비워지기 전에 필요한 걸 미리 읽어둬야 한다"는 순서에 의존하는 대신, 원자적 UPDATE들을 전부 마친 뒤 주문을 다시 조회해서 응답을 조립하는 방식으로 두 메서드를 정리함.
- **교차검증에서 발견해 추가로 막은 것**: 같은 주문에 대한 취소 요청이 동시에 두 번 들어오면(더블클릭, 회원 취소와 관리자 처리가 겹침 등) 위 수정만으로는 여전히 둘 다 "취소 가능" 판정을 통과해 재고를 두 번 복구할 수 있었음 — 재고 자체는 원자적이 됐지만 "이 주문을 취소 처리할 권리"는 원자적이지 않았던 것(항목 2의 결제 멱등성과 같은 종류의 구멍). `OrderRepository`에 `WHERE status = 'PAID'`(회원용) / `WHERE status <> 'CANCELLED'`(관리자용) 조건부 원자적 UPDATE를 추가해 "실제로 상태를 바꾼 요청 단 하나만" 재고를 복구하도록 함. 나아가 취소가 아닌 다른 상태변경(예: SHIPPED)도 `WHERE status <> 'CANCELLED'`로 감싸서, 회원이 막 취소한 주문을 관리자의 다른 상태변경이 거의 동시에 덮어써 "재고는 복구됐는데 주문은 취소 아님"으로 남는 것도 막음. 재고 복구가 상품을 못 찾아 실패하면(현재는 일어날 수 없지만) 조용히 넘어가지 않고 예외를 던져 취소 자체를 롤백시키도록 해서, 일부만 복구되고 주문은 취소된 채로 남는 반쪽짜리 상태도 방지함.
- **검증**: 같은 주문을 진짜로 동시에 5번 취소 요청해서(회원 경로·관리자 경로 각각) 정확히 1건만 성공하고 재고도 정확히 그 주문 수량만큼만 복구되는 것, 회원 취소와 관리자의 다른 상태변경(SHIPPED)을 동시에 보내면 취소가 안전하게 이기고 재고도 정확히 복구되는 것, 취소가 아닌 일반 상태변경과 이미 취소된 주문 재취소(안전한 no-op)는 회귀 없이 그대로 동작하는 것까지 전부 실제 API로 확인.

### 장바구니 담기가 동시 요청에 취약하고, 다른 사이즈를 담아도 같은 줄로 합쳐짐 (2026.08.13)
- **문제**: `CartService.addCart()`가 "조회 후 있으면 수량만 더하고 없으면 새로 담는" 방식이었는데, DB의 중복 방지 유니크 제약이 `UNIQUE(member_id, product_id)`로 **사이즈가 빠져 있었음**. 그래서 (1) 같은 상품을 다른 사이즈로 담아도 서로 다른 줄이 아니라 같은 줄로 합쳐졌고, (2) 같은 상품을 동시에 두 번 담으면(더블클릭) 둘 다 "아직 없음"으로 보고 각자 새로 담으려다 DB 제약 위반으로 500 에러가 났음.
- **해결**: 유니크 제약을 `UNIQUE(member_id, product_id, size)`로 넓히고, `CartRepository`에 `INSERT ... ON CONFLICT (member_id, product_id, size) DO UPDATE`(Postgres 원자적 upsert)를 추가해 "있으면 더하고 없으면 새로 담기"를 DB가 하나의 연산으로 처리하도록 변경. 수량 상한(`@Max(999)`)도 추가하고, `size`가 `null`이면 빈 문자열로 정규화해서(Postgres는 유니크 제약에서 `NULL`끼리는 서로 다른 값으로 취급해 `ON CONFLICT`가 안 걸릴 수 있음) 이 경로에서도 병합이 정확히 되도록 함.
- **시도했다가 실제로 겪은 함정**: 처음엔 재고 차감과 같은 패턴으로 "원자적 UPDATE 먼저 시도 → 0건이면 INSERT → 실패하면(레이스로 먼저 담겼으면) 같은 트랜잭션에서 UPDATE 재시도"로 짰는데, 실제로 동시 요청 5개를 테스트해보니 4개는 성공했지만 1개가 500으로 실패했음. 원인은 실패한 INSERT 직후 그 Hibernate 세션 자체가 오염돼서(`HHH000099: null identifier` 어설션 실패) 같은 트랜잭션 안에서의 재시도 자체가 불가능했던 것. 세션 오염 문제를 근본적으로 피하기 위해 재시도 로직을 전부 걷어내고 Postgres 네이티브 `ON CONFLICT` upsert 하나로 교체함(경쟁이 아예 성립하지 않음).
- **검증**: 같은 상품+사이즈를 동시에 5번 담아 정확히 수량이 합산되는 것(500 없음), 완전히 새 상품을 동시에 5번 처음 담아도(가장 취약했던 경로) 정확히 합산되는 것, 같은 상품을 다른 사이즈로 담으면 별도 줄로 분리되는 것, 수량 1000개 요청은 400으로 거부되는 것, `size`를 안 보내도 재요청 시 중복 행 없이 병합되는 것까지 전부 실제 API로 확인.

### 같은 상품을 동시에 두 번 찜하면 500 에러 (2026.08.13)
- **문제**: `WishlistService.addWishlist()`도 장바구니 담기와 똑같은 "확인 후 없으면 insert" 방식이었음. `wishlists` 테이블에 `UNIQUE(member_id, product_id)` 제약이 있어서, 하트를 빠르게 두 번 누르면 두 요청 모두 "아직 안 찜함"으로 보고 둘 다 insert를 시도하다 하나는 DB 제약 위반으로 500이 났음.
- **해결**: 장바구니에서 검증된 것과 동일한 패턴 적용 — `INSERT ... ON CONFLICT (member_id, product_id) DO NOTHING` 원자적 upsert로 교체(이미 찜한 경우 조용히 무시). 응답 조립에 쓰는 단건 조회에도 `@EntityGraph`를 붙여 N+1 없이 한 번에 가져오도록 함.
- **프론트**: `wishlistStore.js`의 `toggle()`에 진행중 productId를 추적하는 가드를 추가해 연타 자체가 안 나가도록 하고, 서버 실패 시 화면 상태를 바꾸지 않던 기존 로직에 실패 안내(`alert`)를 추가(기존엔 실패해도 아무 반응이 없어 사용자가 실패 여부를 알 수 없었음). 로그아웃 시 이 가드 상태도 같이 초기화해, 로그아웃 직후 다른 계정이 같은 상품을 눌러도 무시되지 않게 함.
- **검증**: 같은 상품을 동시에 5번 찜해도 500 없이 정확히 1건만 남는 것을 실제 API로 확인.

### 백엔드가 방화벽 없이 전체 인터페이스(0.0.0.0)에 직접 노출됨 (2026.08.16)
- **문제**: `application.yml`에 `server.address`가 지정돼 있지 않아 내장 Tomcat이 기본값인 `0.0.0.0:8302`로 바인딩되고 있었음. Caddy 설정(`/etc/caddy/Caddyfile`)은 `forme.dyy.kr` → `127.0.0.1:8302`로만 프록시하도록 되어 있어 로컬호스트 전용 접근을 전제로 하고 있었지만, 서버에 방화벽이 아예 없어서(`ufw inactive`, `iptables` INPUT 규칙 0개) 실제로는 `http://<서버 공인 IP>:8302`로 누구나 직접 접근할 수 있었음 — Caddy의 TLS 종료·도메인 라우팅·(향후 추가할 수 있는) 레이트리밋을 전부 우회.
- **근거**: 정기 서버 점검 중 발견. 실제로 2026-08-15 로그에 포트 8302로 TLS 핸드셰이크로 보이는 바이너리 바이트가 그대로 찍힌 "Error parsing HTTP request header"가 있어, 인터넷 스캐너가 이미 이 포트를 직접 두드린 흔적이 확인됨.
- **해결**: `application.yml`에 `server.address: 127.0.0.1` 추가. 재시작 후 `ss -tlnp`로 `8302`가 `[::ffff:127.0.0.1]:8302`(로컬호스트 전용)로만 바인딩되는 것과, `https://forme.dyy.kr/`(Caddy 경유)이 여전히 정상 응답하는 것 둘 다 확인.
- **참고**: 같은 서버의 다른 프로젝트(트래픽 분석 백엔드, 포트 8080)도 동일하게 전체 인터페이스에 노출돼 있는 것을 확인했으나, FORME과 무관한 별도 레포라 이번 수정 범위에는 포함하지 않음(별도로 점검 필요).
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
