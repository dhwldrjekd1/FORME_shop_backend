# FORME — Heritage Brand Curation Shop

> 네 개의 헤리티지 브랜드를 큐레이션하는 온라인 셀렉트숍

---

## 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 프로젝트명 | FORME |
| 유형 | 풀스택 쇼핑몰 웹 애플리케이션 |
| 개발 기간 | 2026.03.10 ~ 04.13 (5주) |
| 팀 인원 | 3명 |
| 팀 구성 | 팀장: 최동윤 / 팀원: 전보경, 고문식 |
| 담당 업무 | 기획 / 디자인 / 프론트엔드 / 백엔드 / 배포 |
| 큐레이션 브랜드 | Beanpole, Carhartt, Levi's, Dickies |

---

## 기술 스택

### Frontend
| 기술 | 버전 | 용도 |
|------|------|------|
| Vue 3 | 3.x | SPA 프레임워크 (Composition API) |
| Vite | 8.x | 빌드 도구 |
| Pinia | 2.x | 상태 관리 |
| Vue Router | 4.x | SPA 라우팅 |

### Backend
| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 21 | 서버 언어 |
| Spring Boot | 3.5 | 웹 프레임워크 |
| Spring Security | 6.x | 인증/인가 (JWT) |
| Spring Data JPA | 3.x | ORM (Hibernate) |
| PostgreSQL | 16 | 관계형 데이터베이스 |

### Infra
| 기술 | 용도 |
|------|------|
| Docker | 컨테이너 빌드/배포 |
| Nginx | 정적 파일 서빙 |
| Toss Payments | 결제 연동 |

---

## 프로젝트 규모

| 항목 | 수치 |
|------|------|
| Vue 컴포넌트 | 40개 |
| Java 클래스 | 98개 |
| 프론트엔드 코드 | 15,698줄 |
| 백엔드 코드 | 5,845줄 |
| API 엔드포인트 | 92개 |
| DB 테이블 | 15개 |
| 화면 수 | 30개 (사용자 19 + 관리자 11) |

---

## 시스템 아키텍처

```
[사용자 브라우저]
       |
       | HTTP :8302
       v
[Docker Container]
  +-- Nginx ---- Vue 3 SPA (정적 파일)
  +-- Spring Boot API
         |
         v
  [PostgreSQL 16]
```

**요청 흐름:**
```
사용자 → Vue Router(SPA) → Pinia Store → API 호출 → Spring Controller → Service → Repository → DB
```

---

## 주요 기능

### 사용자 기능

| 기능 | 설명 |
|------|------|
| 회원 시스템 | 가입, 로그인(JWT), 프로필 수정, 등급 시스템, 탈퇴 |
| 상품 탐색 | 전체/브랜드별/카테고리별 조회, 성별 필터, 할인가 표시 |
| 상품 상세 | 다중 이미지 갤러리, 사이즈 선택, BMI 사이즈 추천 (비회원 포함) |
| 장바구니 | DB 연동, 사이즈/수량 관리, 등급 할인 적용 |
| 찜 (Wishlist) | DB 연동, 하트 토글, 브랜드 컬러 하트 |
| 주문/결제 | Toss Payments 연동 (토스페이 결제코드 포함), 세일+등급 이중 할인 |
| 리뷰 | 별점(1~5), 작성/수정/삭제, 관리자 답글 |
| Q&A | 상품별/일반 문의, 비밀글, 관리자 답변 |
| 게시판 | 게시글 CRUD, 댓글 CRUD, 검색, 조회수 |
| 마이페이지 | 주문 내역/취소, 찜, 내 리뷰, 내 문의, 회원 정보, 관리자 대시보드 바로가기 버튼 (관리자 계정) |

### 관리자 기능

| 기능 | 설명 |
|------|------|
| 대시보드 | 매출, 주문 수, 회원 수, 상품 수 통계 |
| 상품 관리 | CRUD, 다중 이미지, 서버 이미지 선택, 큐레이터 지정 |
| 주문 관리 | 주문 목록, 상태 변경, 배송 등록/수정 |
| 회원 관리 | 목록/검색, 정지, 등급 변경 |
| 리뷰 관리 | 전체 리뷰, 답글 작성/삭제 |
| Q&A 관리 | 전체 Q&A, 답변 작성/삭제 |
| 카테고리 관리 | CRUD, 정렬 순서, 활성/비활성 |
| 게시판 관리 | 게시글/댓글 삭제 |
| 설정 | 히어로/매거진/스토리/브랜드/매장 정보 (DB 저장) |
| 분석 | 페이지뷰 추적, 시간대별/페이지별 통계 |

---

## 핵심 구현 사항

### 1. 등급별 할인 시스템

| 등급 | 조건 | 할인율 |
|------|------|--------|
| Bronze | 기본 | 0% |
| Silver | 누적 50만원 이상 | 5% |
| Gold | 누적 100만원 이상 | 8% |
| VIP | 누적 150만원 이상 | 12% |

- 주문 완료 시 누적 구매액 자동 계산 → 등급 자동 업그레이드
- 세일 할인 + 등급 할인 이중 적용 (백엔드 OrderService에서 계산)

### 2. BMI 기반 사이즈 추천

- **비회원 포함 누구나** 키/몸무게 입력 시 즉시 사이즈 추천 가능
- BMI 계산 → 성별 x 카테고리(상의/하의) 4종 차트 참조
- 핏 보정: slim(한 사이즈 다운), wide(한 사이즈 업)
- 브랜드별 국가 매핑: Beanpole=KR, Carhartt=UK, Levi's=EU, Dickies=US
- 4개국 사이즈 변환 차트 제공

### 3. Toss Payments 결제 연동

- 토스페이 결제코드(위젯) 결제 흐름 구현
- 결제 승인 API 연동 (`/api/payment/confirm`)
- 결제 성공/실패 페이지 처리

### 4. 큐레이터 추천

- 관리자가 브랜드당 1개 상품을 추천으로 지정
- 전용 API: `PATCH /admin/products/{id}/recommend`
- 메인 CURATOR'S PICK 섹션에 브랜드 순서대로 표시
- 큐레이터 이미지 별도 지정 가능 (우클릭)

### 5. 마이페이지 → 관리자 대시보드 바로가기

- 관리자 계정 로그인 시 마이페이지 내 **관리자 대시보드** 버튼 표시
- 버튼 클릭 시 `/admin/dashboard`로 즉시 이동

### 6. 사이트 설정 DB 저장

- 히어로 슬라이드, 매거진, 스토리, 브랜드 설정, 매장 정보
- `site_settings` 테이블 (key-value JSON)
- 관리자 설정 변경 → 모든 PC에서 즉시 반영

---

## 브랜드 컬러 시스템

| 브랜드 | 컬러 | 설립 | 도시 |
|--------|------|------|------|
| BEANPOLE | `#103728` | 1989 | Seoul |
| CARHARTT | `#9C4F18` | 1889 | Detroit |
| LEVI'S | `#8E1C28` | 1873 | San Francisco |
| DICKIES | `#1A1A1A` | 1922 | Fort Worth |

**공통:** 포인트 `#FF2D2D` / 큐레이터 골드 `#c9a86b`

---

## DB 설계 (15 Tables)

```
member ──┬── orders ──── order_items
         ├── cart_items            ── products ── product_sizes
         ├── wishlists                    │
         ├── reviews ─────────────────────┘
         ├── qna
         ├── boards ──── comments
         │
orders ──── deliveries
categories ── products
page_views (분석)
site_settings (설정)
```

---

## API 구조 (92 Endpoints)

| 모듈 | 수 | 주요 경로 |
|------|-----|-----------|
| 상품 | 14 | `/api/products`, `/api/admin/products` |
| 회원 | 5 | `/api/register`, `/api/login`, `/api/members` |
| 주문 | 6 | `/api/members/{id}/orders`, `/api/admin/orders` |
| 장바구니 | 5 | `/api/members/{id}/cart` |
| 찜 | 3 | `/api/members/{id}/wishlist` |
| 리뷰 | 9 | `/api/reviews`, `/api/admin/reviews` |
| Q&A | 10 | `/api/qna`, `/api/admin/qna` |
| 게시판+댓글 | 14 | `/api/boards`, `/api/comments` |
| 배송 | 4 | `/api/admin/deliveries` |
| 카테고리 | 6 | `/api/categories`, `/api/admin/categories` |
| 관리자 | 5 | `/api/admin/dashboard`, `/api/admin/members` |
| 분석 | 7 | `/api/analytics` |
| 결제 | 2 | `/api/payment` |
| 설정 | 2 | `/api/settings` |
| 사이즈/파일 | 2 | `/api/size`, `/api/admin/files` |

**접근 권한:** 공개 / 로그인 필요 / 관리자 전용 (Spring Security)

---

## 보안

| 항목 | 구현 |
|------|------|
| 인증 | JWT Bearer Token |
| 암호화 | BCryptPasswordEncoder |
| 접근 제어 | Spring Security (ROLE_USER / ROLE_ADMIN) |
| Soft Delete | isActive 플래그 (회원, 상품, 리뷰, Q&A, 게시판, 댓글) |

---

## 프로젝트 구조

```
forme/
├── src/                          # Frontend (Vue 3)
│   ├── views/                    # 페이지 (19 + 11 admin)
│   ├── stores/                   # Pinia 상태 관리 (6개)
│   ├── layouts/                  # Forme32Layout, AdminLayout
│   ├── components/               # 공통 컴포넌트
│   └── api/index.js              # API 클라이언트
│
├── backend/                      # Backend (Spring Boot)
│   └── src/main/java/com/forme/shop/
│       ├── member/               # 회원 (entity, dto, controller, service, repository)
│       ├── product/              # 상품
│       ├── order/                # 주문
│       ├── cart/                 # 장바구니
│       ├── wishlist/             # 찜
│       ├── review/               # 리뷰
│       ├── qna/                  # Q&A
│       ├── board/                # 게시판 + 댓글
│       ├── delivery/             # 배송
│       ├── category/             # 카테고리
│       ├── analytics/            # 분석
│       ├── payment/              # 결제 (Toss)
│       ├── settings/             # 사이트 설정
│       ├── size/                 # 사이즈 추천
│       ├── admin/                # 관리자 대시보드
│       ├── config/               # Security, JWT, CORS
│       └── common/               # 예외 처리, 파일 관리
│
├
└── public/                       # 정적 파일 (이미지)
```
