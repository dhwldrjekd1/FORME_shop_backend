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