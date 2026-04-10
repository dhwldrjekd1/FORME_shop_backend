-- 1. 회원 (member)

CREATE TABLE member (
    id          BIGSERIAL    PRIMARY KEY,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    phone       VARCHAR(20),
    address     VARCHAR(255),
    role        VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    grade       VARCHAR(20)  NOT NULL DEFAULT 'BRONZE',
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 2. 카테고리 (categories)

CREATE TABLE categories (
    id          BIGSERIAL   PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    sort_order  INT         NOT NULL DEFAULT 0,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- 3. 상품 (products)

CREATE TABLE products (
    id           BIGSERIAL    PRIMARY KEY,
    category_id  BIGINT       NOT NULL REFERENCES categories(id),
    name         VARCHAR(100) NOT NULL,
    description  TEXT,
    price        INT          NOT NULL CHECK (price >= 0),
    stock        INT          NOT NULL DEFAULT 0 CHECK (stock >= 0),
    image_url    VARCHAR(500),
    is_new       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_best      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_recommend BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 4. 장바구니 (cart_items)

CREATE TABLE cart_items (
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT    NOT NULL REFERENCES member(id)   ON DELETE CASCADE,
    product_id BIGINT    NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity   INT       NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (member_id, product_id)
);

-- 5. 주문 (orders)

CREATE TABLE orders (
    id             BIGSERIAL    PRIMARY KEY,
    member_id      BIGINT       NOT NULL REFERENCES member(id),
    total_price    INT          NOT NULL CHECK (total_price >= 0),
    status         VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    receiver_name  VARCHAR(50)  NOT NULL,
    receiver_phone VARCHAR(20)  NOT NULL,
    address        VARCHAR(255) NOT NULL,
    memo           VARCHAR(255),
    paid_at        TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 6. 주문 상세 (order_items)

CREATE TABLE order_items (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT    NOT NULL REFERENCES orders(id)   ON DELETE CASCADE,
    product_id BIGINT    NOT NULL REFERENCES products(id),
    quantity   INT       NOT NULL CHECK (quantity > 0),
    unit_price INT       NOT NULL CHECK (unit_price >= 0)
);

-- 7. 배송 (deliveries)

CREATE TABLE deliveries (
    id              BIGSERIAL   PRIMARY KEY,
    order_id        BIGINT      NOT NULL UNIQUE REFERENCES orders(id),
    carrier         VARCHAR(50) NOT NULL DEFAULT 'CJ대한통운',
    tracking_number VARCHAR(50),
    status          VARCHAR(30) NOT NULL DEFAULT 'READY',
    shipped_at      TIMESTAMP,
    delivered_at    TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- 8. 리뷰 (reviews)

CREATE TABLE reviews (
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT    NOT NULL REFERENCES member(id)   ON DELETE CASCADE,
    product_id BIGINT    NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    order_id   BIGINT    NOT NULL REFERENCES orders(id),
    rating     SMALLINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content    TEXT      NOT NULL,
    is_active  BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (member_id, order_id, product_id)
);

-- 9. Q&A (qna)

CREATE TABLE qna (
    id          BIGSERIAL    PRIMARY KEY,
    member_id   BIGINT       NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    product_id  BIGINT       REFERENCES products(id) ON DELETE SET NULL,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    answer      TEXT,
    answered_by BIGINT       REFERENCES member(id),
    is_secret   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    answered_at TIMESTAMP
);

-- 10. 게시판 (boards)
CREATE TABLE boards (
    id         BIGSERIAL    PRIMARY KEY,
    member_id  BIGINT       NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    title      VARCHAR(200) NOT NULL,
    content    TEXT         NOT NULL,
    views      INT          NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 11. 댓글 (comments)

CREATE TABLE comments (
    id         BIGSERIAL PRIMARY KEY,
    board_id   BIGINT    NOT NULL REFERENCES boards(id)  ON DELETE CASCADE,
    member_id  BIGINT    NOT NULL REFERENCES member(id)  ON DELETE CASCADE,
    content    TEXT      NOT NULL,
    is_active  BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 시드 데이터: 카테고리 + 24개 상품 (4개 브랜드 × 6개)
-- ============================================================

INSERT INTO categories (id, name, description, sort_order) VALUES
    (1, 'T-Shirt', '티셔츠/상의', 1),
    (2, 'Pants',   '팬츠/하의',   2),
    (3, 'Denim',   '데님/청바지', 3);

-- ── 칼하트 (Carhartt) ──
INSERT INTO products (id, category_id, name, description, price, stock, image_url, is_new, is_best, is_recommend) VALUES
(104, 1, '디어본 루즈핏 포켓 티셔츠',
 '칼하트 베어본 라인의 릴렉스 포켓 티셔츠입니다. 군더더기 없는 심플한 디자인에 좌측 가슴 포켓이 포인트가 되는 기본 아이템입니다.',
 44000, 50, '/images/carhartt/104.png', TRUE, FALSE, TRUE),
(105, 1, '디어본 릴랙스드 샴록 티셔츠',
 '칼하트 베어본 라인의 샴록 그래픽 릴렉스 티셔츠입니다. 몰트 컬러 바탕에 샴록 모티프 그래픽이 캐주얼하고 유니크한 분위기를 연출합니다.',
 52300, 30, '/images/carhartt/105.png', TRUE, FALSE, FALSE),
(106, 1, '칼하트 포스 릴랙스 티셔츠',
 '칼하트 포스 기술이 적용된 릴렉스 핏 티셔츠입니다. FastDry 기능으로 땀을 빠르게 흡수·발산하며 작업 중에도 쾌적한 착용감을 유지합니다.',
 57500, 40, '/images/carhartt/106.png', FALSE, TRUE, FALSE),
(101, 2, '루즈핏 캔버스 유틸리티 워크 팬츠',
 '러기드 플렉스 원단으로 제작된 루즈핏 스트레이트 진입니다. 움직임에 유연하게 대응하는 스트레치 소재로 작업 현장부터 일상까지 편안하게 착용할 수 있습니다.',
 140000, 25, '/images/carhartt/101.png', FALSE, TRUE, TRUE),
(102, 2, '루즈 스트레이트 러기드 플렉스 덕 트라우저',
 '러기드 플렉스 덕 원단으로 제작된 슬림 테이퍼드 팬츠입니다. 타마크 컬러의 세련된 실루엣과 스트레치 소재의 편안함을 동시에 갖춘 작업복 겸 캐주얼 팬츠입니다.',
 122000, 20, '/images/carhartt/102.png', FALSE, FALSE, FALSE),
(103, 2, '아이코닉 B01 펌 덕 더블 프론트 트라우저',
 '칼하트의 아이코닉 B01 펌 덕 소재 더블프런트 팬츠입니다. 무릎 부분을 이중으로 강화하여 장시간 무릎 작업에도 뛰어난 내구성을 발휘하는 작업복의 정석입니다.',
 209000, 15, '/images/carhartt/103.png', FALSE, TRUE, TRUE);

-- ── 리바이스 (Levi''s) ──
INSERT INTO products (id, category_id, name, description, price, stock, image_url, is_new, is_best, is_recommend) VALUES
(204, 1, 'NAS 밴드 티',
 '힙합 레전드 NAS와의 협업으로 탄생한 그래픽 티셔츠입니다. 클래식한 화이트 바탕에 빈티지 감성의 그래픽이 프린트되어 있습니다.',
 45000, 35, '/images/levis/204.jpg', TRUE, FALSE, FALSE),
(205, 1, 'LEVI''S X BARBOUR 그래픽 티셔츠',
 '리바이스와 영국 헤리티지 브랜드 바버의 콜라보레이션 티셔츠입니다. 두 브랜드의 아이코닉한 로고가 결합된 특별한 그래픽이 특징입니다.',
 109000, 20, '/images/levis/205.jpg', TRUE, TRUE, FALSE),
(206, 1, '헤비웨이트 포켓 티셔츠',
 '두툼한 헤비웨이트 순면으로 제작된 기본 포켓 티셔츠입니다. 단순하지만 견고한 구조로 일상에서 가장 자주 손이 가는 기본 아이템입니다.',
 35000, 60, '/images/levis/206.jpg', FALSE, FALSE, FALSE),
(201, 3, '555 릴렉스 스트레이트 진',
 '1873년 탄생한 오리지널 진의 현대적 해석. 555는 릴렉스 핏과 스트레이트 레그로 여유로운 실루엣을 완성합니다. 클래식한 다크 인디고 워시로 어떤 스타일에도 잘 어울립니다.',
 159000, 30, '/images/levis/201.jpg', FALSE, TRUE, TRUE),
(202, 3, '565 루즈 스트레이트 진',
 '565는 허리부터 허벅지까지 넉넉한 루즈 핏을 제공하는 스트레이트 진입니다. 다크 인디고 플랫 피니쉬 워시로 깔끔하고 절제된 분위기를 연출합니다.',
 99000, 28, '/images/levis/202.jpg', FALSE, FALSE, FALSE),
(203, 3, '505 레귤러 라이트웨이트 진',
 '505 레귤러 핏의 가벼운 데님 버전입니다. 얇은 원단으로 여름철에도 쾌적하게 착용할 수 있으며, 라이트 워시가 캐주얼한 분위기를 완성합니다.',
 169000, 22, '/images/levis/203.jpg', TRUE, FALSE, TRUE);

-- ── 디키즈 (Dickies) ──
INSERT INTO products (id, category_id, name, description, price, stock, image_url, is_new, is_best, is_recommend) VALUES
(304, 1, '트리 로고 레이어드 롱슬리브',
 '디키즈 트리 로고가 새겨진 레이어드 롱슬리브 티셔츠입니다. 멜란지 소재의 자연스러운 텍스처와 레이어드 디테일이 캐주얼한 스트리트 룩을 완성합니다.',
 69000, 40, '/images/dickies/304.jpg', TRUE, FALSE, FALSE),
(305, 1, '파인 스트라이프 포켓 티셔츠',
 '파인 스트라이프 패턴이 더해진 디키즈의 포켓 티셔츠입니다. 그레이 베이스에 섬세한 스트라이프 디테일이 단정하고 세련된 캐주얼 룩을 연출합니다.',
 59000, 45, '/images/dickies/305.jpg', FALSE, FALSE, FALSE),
(306, 1, '워시드 스네이크 로고 티셔츠',
 '스네이크 텍스처 워시 가공이 적용된 디키즈 로고 티셔츠입니다. 워시드 블랙 특유의 빈티지한 질감과 스네이크 패턴 로고가 개성 있는 스트리트 룩을 완성합니다.',
 59000, 35, '/images/dickies/306.jpg', FALSE, TRUE, FALSE),
(301, 3, '루즈핏 카펜터 유틸리티 데님 팬츠',
 '디키즈의 카펜터 스타일 루즈핏 데님 팬츠입니다. 넉넉한 실루엣과 다양한 유틸리티 포켓으로 작업 현장과 일상 모두에 실용적으로 활용할 수 있습니다.',
 105000, 25, '/images/dickies/301.jpg', FALSE, TRUE, TRUE),
(302, 2, '워시드 스네이크 더블니 팬츠',
 '스네이크 텍스처 워시 처리와 더블니 구조가 결합된 디키즈의 워크 팬츠입니다. 무릎 이중 강화로 내구성을 높이고, 독특한 워시드 블랙 컬러가 스트리트 감성을 더합니다.',
 119000, 18, '/images/dickies/302.jpg', TRUE, FALSE, FALSE),
(303, 3, '더블니 카펜터 데님 팬츠',
 '카펜터 스타일과 더블니 구조가 결합된 디키즈의 블랙 데님 팬츠입니다. 무릎 이중 강화와 유틸리티 포켓으로 작업 현장에서의 실용성과 내구성을 극대화했습니다.',
 99000, 20, '/images/dickies/303.jpg', FALSE, FALSE, TRUE);

-- ── 빈폴 (Beanpole) ──
INSERT INTO products (id, category_id, name, description, price, stock, image_url, is_new, is_best, is_recommend) VALUES
(404, 1, '헤리티지클럽 스트라이프 칼라넥 티셔츠',
 '빈폴 헤리티지클럽 라인의 스트라이프 칼라넥 티셔츠입니다. 클래식한 네이비 컬러에 섬세한 스트라이프 패턴과 칼라넥 디테일이 품격 있는 캐주얼 룩을 연출합니다.',
 159000, 22, '/images/beanpole/404.jpg', TRUE, TRUE, FALSE),
(405, 1, '피케 칼라넥 반소매 티셔츠',
 '피케 소재로 제작된 빈폴의 칼라넥 반소매 티셔츠입니다. 피케 특유의 입체적인 텍스처와 칼라넥 디테일이 폴로 셔츠의 클래식한 분위기를 자아냅니다.',
 149000, 30, '/images/beanpole/405.jpg', FALSE, FALSE, FALSE),
(406, 1, '헤리티지클럽 라운드넥 반소매 티셔츠',
 '빈폴 헤리티지클럽 라인의 라운드넥 반소매 티셔츠입니다. 깔끔한 화이트 컬러와 부드러운 소재가 어떤 스타일에도 잘 어울리는 기본 아이템입니다.',
 89000, 50, '/images/beanpole/406.jpg', FALSE, FALSE, TRUE),
(401, 2, '피그먼트 워싱 컴포트핏 치노 팬츠',
 '피그먼트 워싱 처리로 자연스러운 색감을 살린 빈폴의 컴포트핏 치노 팬츠입니다. 부드러운 베이지 컬러와 편안한 핏이 일상과 격식을 모두 소화합니다.',
 239000, 15, '/images/beanpole/401.jpg', TRUE, FALSE, TRUE),
(402, 2, '리넨 혼방 옥스포드 스탠다드핏 팬츠',
 '리넨 혼방 옥스포드 소재로 제작된 빈폴의 스탠다드핏 팬츠입니다. 통기성 좋은 리넨 소재와 클래식한 블랙 컬러가 계절을 가리지 않는 기본 아이템으로 활용됩니다.',
 219000, 18, '/images/beanpole/402.jpg', FALSE, TRUE, FALSE),
(403, 2, '가먼트다잉 세미와이드 팬츠',
 '가먼트 다잉 공법으로 옷 전체를 염색한 빈폴의 세미와이드 팬츠입니다. 깊이 있는 네이비 컬러와 여유로운 세미와이드 실루엣이 세련된 캐주얼 룩을 완성합니다.',
 239000, 12, '/images/beanpole/403.jpg', FALSE, FALSE, FALSE);

-- 명시적 ID 삽입 후 BIGSERIAL sequence 를 다음 값으로 갱신
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
SELECT setval('products_id_seq',   (SELECT MAX(id) FROM products));