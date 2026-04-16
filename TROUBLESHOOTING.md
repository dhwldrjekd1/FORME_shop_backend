# FORME 프로젝트 트러블슈팅 기록

## 목차
1. [빌드 & 배포](#1-빌드--배포)
2. [인증 & 보안](#2-인증--보안)
3. [상품 관리](#3-상품-관리)
4. [이미지 업로드](#4-이미지-업로드)
5. [사이즈 시스템](#5-사이즈-시스템)
6. [결제 & 주문](#6-결제--주문)
7. [프론트엔드 UI](#7-프론트엔드-ui)
8. [데이터베이스](#8-데이터베이스)
9. [기타](#9-기타)

---

## 1. 빌드 & 배포

### 1-1. Vite 빌드 시 import 중복 에러
**증상:** `Identifier 'ref' has already been declared`
```
import { ref, onMounted, onUnmounted } from "vue";
import { ref, computed, onMounted } from "vue";  // 중복!
```
**원인:** HomeView.vue에 기능 추가하면서 import를 새 줄로 추가해 기존 import와 중복됨
**해결:** 하나의 import문으로 통합
```js
import { ref, computed, onMounted, onUnmounted } from "vue";
```

### 1-2. Vite 빌드 시 escaped quotes 에러
**증상:** `font-variation-settings: \"FILL\" 1` Rolldown parser error
**원인:** DetailView.vue에서 `v-bind` 내부에 escaped quotes 사용
**해결:** HTML entities로 변경
```html
<!-- Before -->
:style="isWished ? 'font-variation-settings: \"FILL\" 1' : ''"
<!-- After -->
:style="isWished ? 'font-variation-settings: &quot;FILL&quot; 1' : ''"
```

### 1-3. Spring Boot 빌드 시 getPrice() 메서드 없음
**증상:** `symbol: method getPrice(), location: variable item of type OrderItem`
**원인:** AdminService에서 OrderItem의 필드명이 `unitPrice`인데 `getPrice()`로 호출
**해결:** `item.getPrice()` → `item.getUnitPrice()`로 변경

### 1-4. Docker "Starting Docker Engine" 멈춤
**증상:** Docker Desktop 실행 시 무한 로딩
**원인:** WSL2 커널 미설치
**해결:** 
```bash
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart
# WSL2 커널 업데이트 설치 후 재부팅
```

### 1-5. DB 시드(shoptm.sql) 자동 로드 안 됨
**증상:** `docker compose up` 해도 초기 데이터가 안 들어감
**원인:** PostgreSQL init scripts는 볼륨이 비어있을 때만 실행됨
**해결:** 볼륨 삭제 후 재빌드
```bash
docker compose down -v  # 볼륨 삭제
docker compose up -d --build  # 재빌드 → shoptm.sql 자동 실행
```

### 1-6. pro 폴더 자동 생성
**증상:** 서버 실행 시 `C:\pro\uploads` 폴더가 생성됨
**원인:** `application.yml`의 `file.upload-dir: ./uploads`가 상대 경로라서 실행 위치에 생성
**해결:** Docker 환경에서는 `FILE_UPLOAD_DIR: /app/uploads` 환경변수로 덮어쓰기. 로컬 빈 폴더는 삭제해도 무방

---

## 2. 인증 & 보안

### 2-1. 일반 회원이 관리자 페이지 접근 가능
**증상:** `/admin` 경로에 누구나 접근 가능
**원인:** 프론트 라우터에 권한 체크 없음
**해결:** 라우터 가드에 `meta: { requiresAdmin: true }` 추가
```js
router.beforeEach((to) => {
  if (to.meta.requiresAdmin) {
    if (!authStore.isLoggedIn) return { name: "Login" };
    if (authStore.user?.role !== "ROLE_ADMIN") {
      alert("관리자 권한이 필요합니다.");
      return { name: "Home" };
    }
  }
});
```

### 2-2. 상품 삭제 시 401 에러
**증상:** `Request failed: 401`
**원인:** JWT 토큰 만료 (24시간) 또는 일반 회원으로 로그인
**해결:** 
- 에러 메시지 상세화: 401 → "인증 만료", 403 → "관리자 권한 필요"
- 관리자 계정으로 재로그인 (admin@forme.com / admin1234)

### 2-3. 회원정보 수정 안 됨
**증상:** 마이페이지에서 저장 버튼이 동작하지 않음
**원인:** `saveProfile()` 함수가 alert만 띄우고 API 호출 안 함
**해결:** `PUT /api/members/{id}` API 호출 추가 + localStorage 유저 정보 동시 갱신

### 2-4. 회원정보에 키/몸무게/핏 수정 불가
**증상:** 마이페이지에 키/몸무게/핏 필드 없음
**원인:** Update DTO에 height/weight/fit 필드 없음
**해결:** MemberRequestDto.Update + MemberService.updateMember에 필드 추가

---

## 3. 상품 관리

### 3-1. 상품 등록 시 "서버 오류" 발생
**증상:** 상품 등록 시 500 에러
**원인:** DB에 카테고리가 없어서 `categoryId` 참조 실패
**해결:** DataInitializer에 기본 카테고리 4개 자동 생성 추가 (상의/하의/아우터/액세서리)

### 3-2. 상품 등록 시 "입력값이 올바르지 않습니다"
**증상:** 어떤 필드가 잘못되었는지 알 수 없음
**원인:** GlobalExceptionHandler가 필드별 에러를 반환하지만, 프론트에서 `message`만 표시
**해결:** 프론트에서 `errors` 객체를 파싱하여 필드별 에러 메시지 표시
```js
if (errBody?.errors) {
  const msgs = Object.entries(errBody.errors).map(([field, msg]) => `• ${field}: ${msg}`);
  errorMsg.value = msgs.join('\n');
}
```

### 3-3. 상품 삭제해도 목록에 남아있음
**증상:** 삭제 후에도 상품이 보임
**원인:** 소프트 삭제(`isActive = false`)만 하고 프론트에서 새로고침 안 함
**해결:** `productRepository.delete(product)` 완전 삭제로 변경 + 프론트 목록 갱신

### 3-4. 상품 수정 시 이미지 1장만 표시
**증상:** 상세 페이지에서 3장인데 수정 모달에서 1장만 보임
**원인:** 
1. 상품 목록 API에 `images` 배열이 불완전
2. Update DTO에 `imageUrl`/`imageUrls` 필드 없음
**해결:** 
- 수정 시 `GET /api/products/{id}` 상세 API 호출하여 전체 데이터 로드
- Update DTO에 `imageUrl`/`imageUrls` 필드 추가

### 3-5. 상품 수정 시 카테고리 비활성화
**증상:** 카테고리 셀렉트가 값이 선택 안 됨
**원인:** DB에서 `categoryName`으로 내려오는데 `category` 필드를 우선 사용
**해결:** `categoryName` 우선 사용 + 카테고리 옵션을 DB에서 동적 로드

### 3-6. 사이즈 6개 등록하면 상품 ID 6개 소모
**증상:** 같은 상품인데 사이즈별로 ID가 다름
**원인:** 사이즈별로 별도 상품으로 등록하는 구조
**해결:** `ProductSize` 엔티티 생성 (1:N 관계) → 상품 1개에 사이즈 여러 개 관리

### 3-7. 상품 ID 수정 불가
**증상:** 한번 등록한 ID를 변경할 수 없음
**원인:** JPA에서 @Id 필드는 일반적으로 변경 불가
**해결:** 네이티브 SQL로 직접 변경하는 API 추가
```sql
UPDATE product_sizes SET product_id = :newId WHERE product_id = :oldId;
UPDATE products SET id = :newId WHERE id = :oldId;
```

### 3-8. 브랜드 페이지에 새 상품 안 보임
**증상:** Admin에서 등록한 상품이 브랜드 페이지에 표시 안 됨
**원인:** 상품 필터가 ID 범위(101-199 등)로 하드코딩
**해결:** `product.brand === 'CARHARTT'` 필드 기반 필터로 변경 (ID 범위는 폴백으로 유지)

### 3-9. 사이즈 자동생성 시 국가별 사이즈 불일치
**증상:** 자동생성 버튼이 브랜드 사이즈와 다른 값을 넣음
**원인:** `autoFillSizes`가 하드코딩 사이즈를 사용, `sizeOptions` computed를 무시
**해결:** `sizeOptions.value`를 직접 사용하도록 변경

---

## 4. 이미지 업로드

### 4-1. 로컬 이미지 업로드 후 표시 안 됨
**증상:** Admin에서 이미지 업로드 성공하지만 화면에 안 보임
**원인:** 
- `file.upload-dir`이 `/root/uploads`(Docker용)인데, WebConfig에서 `file:./uploads/`로 서빙 → 경로 불일치
**해결:** WebConfig에서 `@Value("${file.upload-dir}")` 주입받아 동일 경로 사용

### 4-2. 서버 이미지 목록 비어있음
**증상:** Admin 상품등록에서 "서버 이미지" 탭에 이미지 없음
**원인:** Docker 컨테이너에서 `public/new/`, `public/images/` 경로가 JAR 안에 있어서 `File`로 접근 불가
**해결:** 
- Dockerfile에서 이미지 폴더를 컨테이너에 별도 복사
```dockerfile
COPY --from=frontend /build/public/images /app/static-images
COPY --from=frontend /build/public/new /app/static-new
```
- FileController에서 `/app/static-images`, `/app/static-new` 경로도 스캔

### 4-3. 서버 이미지 필터 "전체" 클릭 시 등록 동작
**증상:** 필터 버튼을 누르면 이미지 선택 이벤트 발생
**원인:** 버튼 클릭 이벤트가 부모로 전파
**해결:** `type="button"` + `@click.stop` 추가

---

## 5. 사이즈 시스템

### 5-1. 숫자 사이즈(28, 30 등)에 추천 테두리 안 나옴
**증상:** 문자 사이즈(S, M, L)는 추천 표시되지만 숫자 사이즈는 안 됨
**원인:** 추천 사이즈 `"L"`과 상품 사이즈 `"32"`가 매칭 안 됨
**해결:** `recommendedSize`, `krSize`, `brandSize` 3가지를 모두 비교
```js
const candidates = [res.recommendedSize, res.krSize, res.brandSize];
const recAvail = availableSizes.value.find(s => candidates.includes(s.size));
```

### 5-2. 상의/하의 사이즈 차트 동일하게 표시
**증상:** 하의 상품인데 상의 사이즈 차트가 보임
**원인:** 카테고리 감지 로직이 일부 키워드만 체크
**해결:** 상의/하의 키워드 확장 + `isBottomCategory` computed 통합
```js
const isBottomCategory = computed(() => {
  const cat = (product.value?.category || '').toLowerCase();
  return cat.includes('하의') || cat.includes('팬츠') || cat.includes('데님') || cat.includes('바지');
});
```

### 5-3. 하드코딩 사이즈 ["S","M","L","XL"] 계속 표시
**증상:** DB에 사이즈를 등록 안 했는데 기본 사이즈가 보임
**원인:** `productStore.js`의 mock 데이터 폴백
**해결:** mock 데이터 전체 제거, `sizeStocks`가 없으면 빈 배열 반환

---

## 6. 결제 & 주문

### 6-1. 결제 실패 원인 불명
**증상:** "결제에 실패했습니다" 만 표시되고 구체적 원인 모름
**원인:** 유효성 검사가 없어서 빈 필드로 API 호출
**해결:** 필드별 상세 유효성 검사 추가
```js
if (!form.value.name.trim()) { alert('받는 사람 이름을 입력해주세요.'); return; }
if (!form.value.phone.trim()) { alert('연락처를 입력해주세요.'); return; }
```

### 6-2. 주문 완료 페이지에 상품 목록 안 보임
**증상:** 결제 후 주문 완료 페이지가 비어있음
**원인:** `cartStore.clearCart()` 후 장바구니가 비어서 데이터 없음
**해결:** 결제 완료 시 `localStorage`에 주문 데이터 저장 → 주문완료 페이지에서 로드

---

## 7. 프론트엔드 UI

### 7-1. 헤더 아이콘 색상 문제
**증상:** 아이콘이 의도와 다른 색상으로 표시
**원인:** `v-html`로 생성된 DOM이 scoped CSS 범위 밖
**해결:** `:deep()` 셀렉터 사용 또는 인라인 스타일 적용

### 7-2. 글로벌 색상 치환으로 큐레이터 섹션 깨짐
**증상:** 큐레이터 골드(#c9a86b) 색상이 레드로 변경됨
**원인:** `replace_all #c9a86b → #FF2D2D` 실행 시 큐레이터 CSS도 변경됨
**해결:** 5개 큐레이터 CSS 규칙 수동 복원

### 7-3. 모달 외곽 클릭 시 닫힘
**증상:** 상품 등록 중 실수로 외곽 클릭하면 모달이 닫힘
**원인:** `@click.self="showModal = false"` 설정
**해결:** `@click.self` 제거 → X 버튼으로만 닫기 가능

### 7-4. 히어로 좌우 버튼 위치 안 맞음
**증상:** `<` `>` 버튼이 원 안에서 중앙이 아님
**원인:** 텍스트 문자의 기본 baseline이 중앙이 아님
**해결:** SVG 아이콘으로 변경하여 정확한 중앙 정렬

### 7-5. 검색 시 할인 표시 안 나옴
**증상:** 상품 목록에서 할인 상품인데 할인 뱃지/할인가 없음
**원인:** ListView에 `discountRate` 관련 UI 코드 없음
**해결:** 할인 뱃지 (`-20%`) + 할인가(빨강)/원가(취소선) 조건부 표시 추가

---

## 8. 데이터베이스

### 8-1. DB 백업 시 UTF-16 인코딩 문제
**증상:** `ERROR: invalid byte sequence for encoding "UTF8": 0xff`
**원인:** PowerShell의 `>` 리다이렉션이 UTF-16으로 파일 생성
**해결:** 컨테이너 안에서 직접 백업 생성 (인코딩 문제 없음)
```bash
# ❌ PowerShell > 사용 → UTF-16
docker exec forme_postgres pg_dump -U postgres shoptm > backup.sql

# ✅ 컨테이너 안에서 -f 사용 → UTF-8
docker exec forme_postgres pg_dump -U postgres -f /tmp/backup.sql shoptm
docker cp forme_postgres:/tmp/backup.sql C:\forme\backend\shoptm.sql
```

### 8-2. DB 복원 시 테이블 충돌
**증상:** `ERROR: relation "products" already exists`
**원인:** Spring Boot가 이미 테이블을 생성한 상태에서 복원 시도
**해결:** 백엔드 먼저 중지 → DB 삭제 → 재생성 → 복원
```bash
docker stop forme_backend
docker exec forme_postgres psql -U postgres -c "DROP DATABASE shoptm;"
docker exec forme_postgres psql -U postgres -c "CREATE DATABASE shoptm;"
docker cp backup.sql forme_postgres:/tmp/restore.sql
docker exec forme_postgres psql -U postgres shoptm -f /tmp/restore.sql
docker start forme_backend
```

### 8-3. DB DROP 시 "being accessed by other users"
**증상:** `ERROR: database "shoptm" is being accessed by other users`
**원인:** Spring Boot가 DB 연결을 잡고 있음
**해결:** `docker stop forme_backend` 먼저 실행 후 DROP

### 8-4. features/composition에 `\n`이 문자 그대로 저장
**증상:** 페이지에 `코튼 100%\n릴렉스 핏` 으로 표시
**원인:** SQL에서 `'...\n...'`은 문자열 그대로 저장됨
**해결:** PostgreSQL의 `E'...\n...'` 이스케이프 문법 사용
```sql
-- ❌
'코튼 100%\n릴렉스 핏'
-- ✅
E'코튼 100%\n릴렉스 핏'
```

### 8-5. 시드 SQL 실행 경로 오류
**증상:** `GetFileAttributesEx C:\pro: The system cannot find the file specified`
**원인:** 프로젝트 경로가 `C:\pro\forme\`이 아닌 `C:\forme\`
**해결:** 정확한 경로 사용
```bash
docker cp C:\forme\backend\seed_products.sql forme_postgres:/tmp/seed.sql
```

---

## 9. 기타

### 9-1. SPA 폴백 시 정적 자산 HTML 반환
**증상:** JS/CSS 파일 요청에 HTML이 반환되어 화면 흰색
**원인:** SpaFallbackController가 모든 요청을 index.html로 리다이렉트
**해결:** SpaFallbackController 제거 → WebConfig의 PathResourceResolver로 대체

### 9-2. 페이지뷰 트래킹에 admin 데이터 포함
**증상:** 분석 페이지에 admin 페이지 체류 데이터가 표시
**원인:** 모든 페이지에서 무차별 트래킹
**해결:** 
- 프론트: `pageTracker.js`에서 admin 경로 제외
- 백엔드: 모든 Analytics 쿼리에 `WHERE page_path NOT LIKE '/admin%'` 추가

### 9-3. 추천 상품 브랜드 중복 등록
**증상:** 같은 브랜드에 추천 상품 2개 등록 가능
**원인:** 추천 등록 시 브랜드 중복 체크 없음
**해결:** `existsByBrandAndIsRecommendTrueAndIsActiveTrue()` 쿼리로 중복 검증

---

## 유용한 명령어 모음

### DB 관리
```bash
# 상품 목록 조회
docker exec forme_postgres psql -U postgres shoptm -c "SELECT id, name, brand, price FROM products ORDER BY id;"

# 특정 상품 삭제
docker exec forme_postgres psql -U postgres shoptm -c "DELETE FROM product_sizes WHERE product_id=411; DELETE FROM products WHERE id=411;"

# DB 백업 (안전)
docker exec forme_postgres pg_dump -U postgres -f /tmp/backup.sql shoptm
docker cp forme_postgres:/tmp/backup.sql C:\forme\backend\shoptm.sql

# DB 복원
docker stop forme_backend
docker exec forme_postgres psql -U postgres -c "DROP DATABASE shoptm;"
docker exec forme_postgres psql -U postgres -c "CREATE DATABASE shoptm;"
docker cp C:\forme\backend\shoptm.sql forme_postgres:/tmp/restore.sql
docker exec forme_postgres psql -U postgres shoptm -f /tmp/restore.sql
docker start forme_backend
```

### Docker 관리
```bash
# 빌드 + 실행
cd C:\forme\backend && docker compose up -d --build

# 로그 확인
docker logs forme_backend --tail 50

# 중지
docker compose down

# 전체 초기화 (DB 포함)
docker compose down -v
docker compose up -d --build
```
