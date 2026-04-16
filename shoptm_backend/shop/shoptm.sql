--
-- PostgreSQL database dump
--

\restrict 5VJWAOdOJmPwR7Uoaa1DIIDdYS6WbHEOfbHNKEvEZEjoN42LxWTLfIgPKNolS7S

-- Dumped from database version 16.13 (Debian 16.13-1.pgdg13+1)
-- Dumped by pg_dump version 16.13 (Debian 16.13-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: boards; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.boards (
    id bigint NOT NULL,
    member_id bigint NOT NULL,
    title character varying(200) NOT NULL,
    content text NOT NULL,
    views integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.boards OWNER TO postgres;

--
-- Name: boards_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.boards_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.boards_id_seq OWNER TO postgres;

--
-- Name: boards_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.boards_id_seq OWNED BY public.boards.id;


--
-- Name: cart_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.cart_items (
    id bigint NOT NULL,
    member_id bigint NOT NULL,
    product_id bigint NOT NULL,
    quantity integer DEFAULT 1 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    size character varying(20),
    CONSTRAINT cart_items_quantity_check CHECK ((quantity > 0))
);


ALTER TABLE public.cart_items OWNER TO postgres;

--
-- Name: cart_items_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.cart_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.cart_items_id_seq OWNER TO postgres;

--
-- Name: cart_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.cart_items_id_seq OWNED BY public.cart_items.id;


--
-- Name: categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.categories (
    id bigint NOT NULL,
    name character varying(50) NOT NULL,
    description character varying(200),
    sort_order integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.categories OWNER TO postgres;

--
-- Name: categories_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.categories_id_seq OWNER TO postgres;

--
-- Name: categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.categories_id_seq OWNED BY public.categories.id;


--
-- Name: comments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.comments (
    id bigint NOT NULL,
    board_id bigint NOT NULL,
    member_id bigint NOT NULL,
    content text NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.comments OWNER TO postgres;

--
-- Name: comments_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.comments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.comments_id_seq OWNER TO postgres;

--
-- Name: comments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.comments_id_seq OWNED BY public.comments.id;


--
-- Name: deliveries; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.deliveries (
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    carrier character varying(50) DEFAULT 'CJ대한통운'::character varying NOT NULL,
    tracking_number character varying(50),
    status character varying(30) DEFAULT 'READY'::character varying NOT NULL,
    shipped_at timestamp without time zone,
    delivered_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.deliveries OWNER TO postgres;

--
-- Name: deliveries_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.deliveries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.deliveries_id_seq OWNER TO postgres;

--
-- Name: deliveries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.deliveries_id_seq OWNED BY public.deliveries.id;


--
-- Name: member; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.member (
    id bigint NOT NULL,
    email character varying(100) NOT NULL,
    password character varying(255) NOT NULL,
    name character varying(50) NOT NULL,
    phone character varying(20),
    address character varying(255),
    role character varying(20) DEFAULT 'ROLE_USER'::character varying NOT NULL,
    grade character varying(20) DEFAULT 'BRONZE'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    fit character varying(20),
    height double precision,
    weight double precision
);


ALTER TABLE public.member OWNER TO postgres;

--
-- Name: member_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.member_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.member_id_seq OWNER TO postgres;

--
-- Name: member_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.member_id_seq OWNED BY public.member.id;


--
-- Name: order_items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_items (
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    product_id bigint NOT NULL,
    quantity integer NOT NULL,
    unit_price integer NOT NULL,
    size character varying(20),
    CONSTRAINT order_items_quantity_check CHECK ((quantity > 0)),
    CONSTRAINT order_items_unit_price_check CHECK ((unit_price >= 0))
);


ALTER TABLE public.order_items OWNER TO postgres;

--
-- Name: order_items_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.order_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.order_items_id_seq OWNER TO postgres;

--
-- Name: order_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.order_items_id_seq OWNED BY public.order_items.id;


--
-- Name: orders; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.orders (
    id bigint NOT NULL,
    member_id bigint NOT NULL,
    total_price integer NOT NULL,
    status character varying(30) DEFAULT 'PENDING'::character varying NOT NULL,
    receiver_name character varying(50) NOT NULL,
    receiver_phone character varying(20) NOT NULL,
    address character varying(255) NOT NULL,
    memo character varying(255),
    paid_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT orders_total_price_check CHECK ((total_price >= 0))
);


ALTER TABLE public.orders OWNER TO postgres;

--
-- Name: orders_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.orders_id_seq OWNER TO postgres;

--
-- Name: orders_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.orders_id_seq OWNED BY public.orders.id;


--
-- Name: page_views; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.page_views (
    id integer NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    duration integer NOT NULL,
    login_id character varying(50),
    page_name character varying(100) NOT NULL,
    page_path character varying(255) NOT NULL
);


ALTER TABLE public.page_views OWNER TO postgres;

--
-- Name: page_views_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.page_views ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.page_views_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: product_sizes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_sizes (
    id bigint NOT NULL,
    size character varying(20) NOT NULL,
    stock integer NOT NULL,
    product_id bigint NOT NULL
);


ALTER TABLE public.product_sizes OWNER TO postgres;

--
-- Name: product_sizes_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.product_sizes ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.product_sizes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: products; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.products (
    id bigint NOT NULL,
    category_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    price integer NOT NULL,
    stock integer DEFAULT 0 NOT NULL,
    image_url character varying(500),
    is_new boolean DEFAULT false NOT NULL,
    is_best boolean DEFAULT false NOT NULL,
    is_recommend boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    brand character varying(50),
    color_hex character varying(10),
    color_name character varying(50),
    composition text,
    curator_image_url character varying(500),
    discount_rate integer,
    features text,
    gender character varying(10),
    image_urls character varying(2000),
    original_price integer,
    size character varying(50),
    thumbnail_url character varying(500),
    CONSTRAINT products_price_check CHECK ((price >= 0)),
    CONSTRAINT products_stock_check CHECK ((stock >= 0))
);


ALTER TABLE public.products OWNER TO postgres;

--
-- Name: products_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.products_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.products_id_seq OWNER TO postgres;

--
-- Name: products_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.products_id_seq OWNED BY public.products.id;


--
-- Name: qna; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.qna (
    id bigint NOT NULL,
    member_id bigint NOT NULL,
    product_id bigint,
    title character varying(200) NOT NULL,
    content text NOT NULL,
    answer text,
    answered_by bigint,
    is_secret boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    answered_at timestamp without time zone
);


ALTER TABLE public.qna OWNER TO postgres;

--
-- Name: qna_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.qna_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.qna_id_seq OWNER TO postgres;

--
-- Name: qna_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.qna_id_seq OWNED BY public.qna.id;


--
-- Name: reviews; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.reviews (
    id bigint NOT NULL,
    member_id bigint NOT NULL,
    product_id bigint NOT NULL,
    order_id bigint,
    rating integer NOT NULL,
    content text NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    replied_at timestamp(6) without time zone,
    reply text,
    CONSTRAINT reviews_rating_check CHECK (((rating >= 1) AND (rating <= 5)))
);


ALTER TABLE public.reviews OWNER TO postgres;

--
-- Name: reviews_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.reviews_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.reviews_id_seq OWNER TO postgres;

--
-- Name: reviews_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.reviews_id_seq OWNED BY public.reviews.id;


--
-- Name: site_settings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.site_settings (
    setting_key character varying(100) NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    value text
);


ALTER TABLE public.site_settings OWNER TO postgres;

--
-- Name: wishlists; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.wishlists (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    member_id bigint NOT NULL,
    product_id bigint NOT NULL
);


ALTER TABLE public.wishlists OWNER TO postgres;

--
-- Name: wishlists_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.wishlists ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.wishlists_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: boards id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.boards ALTER COLUMN id SET DEFAULT nextval('public.boards_id_seq'::regclass);


--
-- Name: cart_items id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cart_items ALTER COLUMN id SET DEFAULT nextval('public.cart_items_id_seq'::regclass);


--
-- Name: categories id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories ALTER COLUMN id SET DEFAULT nextval('public.categories_id_seq'::regclass);


--
-- Name: comments id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comments ALTER COLUMN id SET DEFAULT nextval('public.comments_id_seq'::regclass);


--
-- Name: deliveries id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.deliveries ALTER COLUMN id SET DEFAULT nextval('public.deliveries_id_seq'::regclass);


--
-- Name: member id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.member ALTER COLUMN id SET DEFAULT nextval('public.member_id_seq'::regclass);


--
-- Name: order_items id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_items ALTER COLUMN id SET DEFAULT nextval('public.order_items_id_seq'::regclass);


--
-- Name: orders id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders ALTER COLUMN id SET DEFAULT nextval('public.orders_id_seq'::regclass);


--
-- Name: products id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products ALTER COLUMN id SET DEFAULT nextval('public.products_id_seq'::regclass);


--
-- Name: qna id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.qna ALTER COLUMN id SET DEFAULT nextval('public.qna_id_seq'::regclass);


--
-- Name: reviews id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews ALTER COLUMN id SET DEFAULT nextval('public.reviews_id_seq'::regclass);


--
-- Data for Name: boards; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.boards (id, member_id, title, content, views, is_active, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: cart_items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.cart_items (id, member_id, product_id, quantity, created_at, updated_at, size) FROM stdin;
\.


--
-- Data for Name: categories; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.categories (id, name, description, sort_order, is_active, created_at) FROM stdin;
1	T-Shirt	티셔츠/상의	1	t	2026-04-13 03:44:57.801265
2	Pants	팬츠/하의	2	t	2026-04-13 03:44:57.801265
3	Denim	데님/청바지	3	t	2026-04-13 03:44:57.801265
4	액세서리	모자, 가방, 벨트 등	4	t	2026-04-13 03:47:31.826346
\.


--
-- Data for Name: comments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.comments (id, board_id, member_id, content, is_active, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: deliveries; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.deliveries (id, order_id, carrier, tracking_number, status, shipped_at, delivered_at, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: member; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.member (id, email, password, name, phone, address, role, grade, is_active, created_at, updated_at, fit, height, weight) FROM stdin;
1	admin@forme.com	$2a$10$9cXkYg6UEinQMYHBH01cje1wusqMn8aO3AAadUZYeYVl3TZiizEMa	관리자	\N	\N	ROLE_ADMIN	VIP	t	2026-04-13 03:45:07.688065	2026-04-13 03:45:07.688103	\N	\N	\N
\.


--
-- Data for Name: order_items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.order_items (id, order_id, product_id, quantity, unit_price, size) FROM stdin;
\.


--
-- Data for Name: orders; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.orders (id, member_id, total_price, status, receiver_name, receiver_phone, address, memo, paid_at, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: page_views; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.page_views (id, created_at, duration, login_id, page_name, page_path) FROM stdin;
1	2026-04-13 03:45:35.669334	26	admin@forme.com	home	/
2	2026-04-13 03:45:39.039317	3	admin@forme.com	new	/new
3	2026-04-13 03:45:45.130601	2	admin@forme.com	admin	/admin
4	2026-04-13 03:46:19.661552	34	admin@forme.com	admin-settings	/admin/settings
5	2026-04-13 03:47:33.889096	75	admin@forme.com	admin-products	/admin/products
6	2026-04-13 03:47:40.086484	119	admin@forme.com	beanpole	/beanpole
7	2026-04-13 03:49:54.66358	134	admin@forme.com	beanpole	/beanpole
8	2026-04-13 03:49:59.280628	5	admin@forme.com	product-detail	/products/406
9	2026-04-13 03:55:38.81114	339	admin@forme.com	product-detail	/products/105
10	2026-04-13 03:55:41.984402	3	admin@forme.com	products	/products
11	2026-04-13 03:55:44.559715	2	admin@forme.com	new	/new
12	2026-04-13 03:55:46.162155	2	admin@forme.com	best	/best
13	2026-04-13 03:55:49.664554	3	admin@forme.com	beanpole	/beanpole
14	2026-04-13 03:55:51.97498	3	admin@forme.com	product-detail	/products/405
15	2026-04-13 03:55:55.076903	3	admin@forme.com	products	/products
16	2026-04-13 03:56:22.909403	24	admin@forme.com	product-detail	/products/406
17	2026-04-13 03:56:24.590744	2	admin@forme.com	sale	/sale
18	2026-04-13 03:56:44.5249	18	admin@forme.com	product-detail	/products/306
19	2026-04-13 03:57:00.649749	16	admin@forme.com	product-detail	/products/405
20	2026-04-13 03:57:05.618914	2	admin@forme.com	home	/
21	2026-04-13 03:57:26.091438	23	admin@forme.com	sale	/sale
22	2026-04-13 03:57:38.617059	9	admin@forme.com	product-detail	/products/405
23	2026-04-13 03:57:44.088461	4	admin@forme.com	new	/new
24	2026-04-13 03:57:45.569239	2	admin@forme.com	best	/best
25	2026-04-13 03:57:47.171087	2	admin@forme.com	beanpole	/beanpole
26	2026-04-13 03:58:00.810405	14	admin@forme.com	beanpole	/beanpole
27	2026-04-13 03:58:06.576339	4	admin@forme.com	products	/products
28	2026-04-13 03:58:38.761375	31	admin@forme.com	product-detail	/products/406
29	2026-04-13 03:59:28.711671	47	admin@forme.com	product-detail	/products/104
30	2026-04-13 03:59:31.777109	3	admin@forme.com	dickies	/dickies
31	2026-04-13 03:59:42.64557	12	admin@forme.com	products	/products
32	2026-04-13 04:00:09.122167	752	admin@forme.com	admin-products	/admin/products
33	2026-04-13 04:00:42.453089	59	admin@forme.com	home	/
34	2026-04-13 04:02:17.445667	93	admin@forme.com	product-detail	/products/405
35	2026-04-13 04:03:04.060976	174	admin@forme.com	admin-analytics	/admin/analytics
36	2026-04-13 04:03:19.231586	59	admin@forme.com	product-detail	/products/405
37	2026-04-13 04:03:41.068009	37	admin@forme.com	admin-settings	/admin/settings
38	2026-04-13 04:03:58.332772	40	admin@forme.com	beanpole	/beanpole
39	2026-04-13 04:04:06.186053	21	admin@forme.com	admin-analytics	/admin/analytics
40	2026-04-13 04:04:13.707063	8	admin@forme.com	admin	/admin
41	2026-04-13 04:04:46.199901	31	admin@forme.com	admin-products	/admin/products
42	2026-04-13 04:04:52.22445	52	admin@forme.com	product-detail	/products/405
43	2026-04-13 04:06:17.222181	87	admin@forme.com	home	/
44	2026-04-13 04:06:25.643544	9	admin@forme.com	mypage	/mypage
45	2026-04-13 04:09:26.823119	179	admin@forme.com	home	/
46	2026-04-13 04:09:36.064755	10	admin@forme.com	home	/
47	2026-04-13 04:09:37.848687	2	admin@forme.com	dickies	/dickies
48	2026-04-13 04:11:16.574517	99	admin@forme.com	product-detail	/products/407
49	2026-04-13 04:12:57.723164	489	admin@forme.com	admin-products	/admin/products
50	2026-04-13 05:14:20.70256	502	admin@forme.com	admin-products	/admin/products
51	2026-04-13 05:14:28.659406	3	admin@forme.com	home	/
52	2026-04-13 05:14:37.902357	10	admin@forme.com	home	/
53	2026-04-13 05:14:58.418593	23	admin@forme.com	home	/
54	2026-04-13 05:15:05.958043	8	admin@forme.com	products	/products
55	2026-04-13 05:15:09.514481	4	admin@forme.com	home	/
56	2026-04-13 05:15:38.994627	26	admin@forme.com	product-detail	/products/407
57	2026-04-13 05:21:58.958871	381	admin@forme.com	home	/
58	2026-04-13 05:24:03.912055	124	admin@forme.com	home	/
59	2026-04-13 05:27:15.315068	8143	admin@forme.com	admin-faq	/admin/faq
60	2026-04-13 05:27:17.083052	192	admin@forme.com	home	/
61	2026-04-13 05:30:55.646017	220	admin@forme.com	home	/
62	2026-04-13 05:39:48.996723	96	admin@forme.com	home	/
63	2026-04-13 05:40:19.232955	30	admin@forme.com	home	/
64	2026-04-13 05:43:59.914823	216561	admin@forme.com	admin-products	/admin/products
65	2026-04-13 05:44:20.71491	2	admin@forme.com	home	/
66	2026-04-13 05:44:27.304693	7	admin@forme.com	home	/
67	2026-04-13 05:44:37.358046	11	admin@forme.com	product-detail	/products/409
68	2026-04-13 05:45:06.513423	286	admin@forme.com	new	/new
69	2026-04-13 05:45:10.770571	5	admin@forme.com	new	/new
70	2026-04-13 05:45:20.694427	11	admin@forme.com	products	/products
71	2026-04-13 05:45:33.065878	10	admin@forme.com	home	/
72	2026-04-13 05:46:11.193505	39	admin@forme.com	home	/
73	2026-04-13 05:46:19.719224	9	admin@forme.com	home	/
74	2026-04-13 05:46:25.291509	4	admin@forme.com	products	/products
75	2026-04-13 05:46:41.591264	14	admin@forme.com	best	/best
76	2026-04-13 05:46:48.707198	7	admin@forme.com	beanpole	/beanpole
77	2026-04-13 05:46:53.473569	5	admin@forme.com	carhartt	/carhartt
78	2026-04-13 05:47:01.691183	7	admin@forme.com	sale	/sale
79	2026-04-13 05:47:12.683943	5	admin@forme.com	mypage	/mypage
80	2026-04-13 05:47:27.883459	16	admin@forme.com	product-detail	/products/105
81	2026-04-13 05:49:39.844097	132	admin@forme.com	home	/
82	2026-04-13 05:49:46.635878	7	admin@forme.com	home	/
83	2026-04-13 05:49:57.117089	8	admin@forme.com	new	/new
84	2026-04-13 05:52:49.48152	171	admin@forme.com	home	/
85	2026-04-13 05:52:50.990036	2	admin@forme.com	new	/new
86	2026-04-13 10:46:52.660203	1078	admin@forme.com	home	/
87	2026-04-13 10:47:35.083657	541	admin@forme.com	product-detail	/products/407
88	2026-04-13 10:47:42.273874	4	admin@forme.com	home	/
89	2026-04-13 10:48:07.002096	27	admin@forme.com	home	/
90	2026-04-13 10:48:23.207034	14	admin@forme.com	products	/products
91	2026-04-13 10:48:28.39144	6	admin@forme.com	new	/new
92	2026-04-13 10:48:34.86866	7	admin@forme.com	best	/best
93	2026-04-13 10:48:37.786557	3	admin@forme.com	beanpole	/beanpole
94	2026-04-13 10:48:48.119285	7	admin@forme.com	beanpole	/beanpole
95	2026-04-13 10:48:52.501362	5	admin@forme.com	carhartt	/carhartt
96	2026-04-13 10:48:59.302189	8	admin@forme.com	levis	/levis
97	2026-04-13 10:49:04.371789	6	admin@forme.com	dickies	/dickies
98	2026-04-13 10:49:21.837073	16	admin@forme.com	sale	/sale
99	2026-04-13 10:53:04.45647	225	admin@forme.com	home	/
100	2026-04-13 13:55:35.24896	5146	admin@forme.com	beanpole	/beanpole
101	2026-04-13 13:55:40.388132	5	admin@forme.com	beanpole	/beanpole
102	2026-04-13 13:55:44.71317	5	admin@forme.com	products	/products
103	2026-04-13 13:55:54.124102	10	admin@forme.com	home	/
104	2026-04-13 13:55:55.652565	2	admin@forme.com	sale	/sale
105	2026-04-13 13:56:03.524542	5	admin@forme.com	home	/
106	2026-04-13 13:58:26.058031	144	admin@forme.com	levis	/levis
107	2026-04-13 13:58:29.28084	3	admin@forme.com	levis	/levis
108	2026-04-13 13:58:37.260086	7	admin@forme.com	beanpole	/beanpole
109	2026-04-13 13:58:41.149496	4	admin@forme.com	carhartt	/carhartt
110	2026-04-13 13:58:47.299425	3	admin@forme.com	levis	/levis
111	2026-04-13 13:59:06.086684	21	admin@forme.com	dickies	/dickies
112	2026-04-13 13:59:21.289461	12	admin@forme.com	levis	/levis
113	2026-04-13 13:59:22.84125	2	admin@forme.com	home	/
114	2026-04-13 13:59:40.503315	20	admin@forme.com	beanpole	/beanpole
115	2026-04-13 14:00:00.502606	18	admin@forme.com	beanpole	/beanpole
116	2026-04-13 14:00:18.562335	20	admin@forme.com	beanpole	/beanpole
117	2026-04-13 14:00:28.329533	7	admin@forme.com	beanpole	/beanpole
118	2026-04-13 14:00:29.920822	2	admin@forme.com	beanpole	/beanpole
119	2026-04-13 14:00:34.990937	6	admin@forme.com	carhartt	/carhartt
120	2026-04-13 14:00:41.38412	7	admin@forme.com	levis	/levis
121	2026-04-13 14:00:55.298285	15	admin@forme.com	dickies	/dickies
122	2026-04-13 14:13:34.754092	499	admin@forme.com	product-detail	/products/407
\.


--
-- Data for Name: product_sizes; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.product_sizes (id, size, stock, product_id) FROM stdin;
320	28	99	301
321	30	99	301
322	32	99	301
323	34	99	301
324	36	99	301
325	38	99	301
326	28	99	302
327	30	99	302
328	32	99	302
329	34	99	302
330	36	99	302
331	38	99	302
332	28	99	303
333	30	99	303
334	32	99	303
335	34	99	303
336	36	99	303
337	38	99	303
338	XS	99	306
339	S	99	306
340	M	99	306
341	L	99	306
342	XL	99	306
343	XXL	99	306
163	XS	99	407
164	S	99	407
165	M	99	407
166	L	99	407
167	XL	99	407
168	XXL	99	407
169	XS	99	408
170	S	99	408
171	M	99	408
172	L	99	408
173	XL	99	408
174	XXL	99	408
175	32-34	99	409
176	36	99	409
177	38-40	99	409
178	40-42	99	409
179	42-44	99	409
180	46-48	99	409
181	XS	99	410
182	S	99	410
183	M	99	410
184	L	99	410
185	XL	99	410
186	XXL	99	410
187	32	99	411
188	34	99	411
189	36	99	411
190	38	99	411
191	40	99	411
192	42	99	411
193	44	99	411
194	44	99	412
195	55	99	412
196	66	99	412
197	77	99	412
198	88	99	412
199	99	99	412
200	28	99	401
201	30	99	401
202	32	99	401
203	34	99	401
204	36	99	401
205	38	99	401
206	28	99	402
207	30	99	402
208	32	99	402
209	34	99	402
210	36	99	402
211	38	99	402
212	90	99	404
213	95	99	404
214	100	99	404
215	105	99	404
216	110	99	404
217	115	99	404
218	90	99	405
219	95	99	405
220	100	99	405
221	105	99	405
222	110	99	405
223	115	99	405
224	90	99	406
225	95	99	406
226	100	99	406
227	105	99	406
228	110	99	406
229	115	99	406
230	28	99	403
231	30	99	403
232	32	99	403
233	34	99	403
234	36	99	403
235	38	99	403
236	28	99	101
237	30	99	101
238	32	99	101
239	34	99	101
240	36	99	101
241	38	99	101
242	XS	99	106
243	S	99	106
244	M	99	106
245	L	99	106
246	XL	99	106
247	XXL	99	106
248	28	99	102
249	30	99	102
250	32	99	102
251	34	99	102
252	36	99	102
253	38	99	102
254	28	99	103
255	30	99	103
256	32	99	103
257	34	99	103
258	36	99	103
259	38	99	103
260	XS	99	105
261	S	99	105
262	M	99	105
263	L	99	105
264	XL	99	105
265	XXL	99	105
266	XS	99	104
267	S	99	104
268	M	99	104
269	L	99	104
270	XL	99	104
271	XXL	99	104
272	44	99	204
273	46	99	204
274	48-50	99	204
275	52	99	204
276	54-56	99	204
277	58-60	99	204
278	44	99	205
279	46	99	205
280	48-50	99	205
281	52	99	205
282	54-56	99	205
283	58-60	99	205
284	44	99	206
285	46	99	206
286	48-50	99	206
287	52	99	206
288	54-56	99	206
289	58-60	99	206
290	44	99	203
291	46	99	203
292	48	99	203
293	50	99	203
294	52	99	203
295	54	99	203
296	44	99	202
297	46	99	202
298	48	99	202
299	50	99	202
300	52	99	202
301	54	99	202
302	44	99	201
303	46	99	201
304	48	99	201
305	50	99	201
306	52	99	201
307	54	99	201
308	XS	99	304
309	S	99	304
310	M	99	304
311	L	99	304
312	XL	99	304
313	XXL	99	304
314	XS	99	305
315	S	99	305
316	M	99	305
317	L	99	305
318	XL	99	305
319	XXL	99	305
\.


--
-- Data for Name: products; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.products (id, category_id, name, description, price, stock, image_url, is_new, is_best, is_recommend, is_active, created_at, updated_at, brand, color_hex, color_name, composition, curator_image_url, discount_rate, features, gender, image_urls, original_price, size, thumbnail_url) FROM stdin;
402	2	리넨 혼방 옥스포드 스탠다드핏 팬츠	리넨 혼방 옥스포드 소재로 제작된 빈폴의 스탠다드핏 팬츠입니다. 통기성 좋은 리넨 소재와 클래식한 블랙 컬러가 계절을 가리지 않는 기본 아이템으로 활용됩니다.	219000	594	/images/beanpole/402.jpg	t	f	f	t	2026-04-13 03:44:57.814456	2026-04-13 05:40:38.950822	BEANPOLE	#1a1a1a	Black	55% Linen, 45% Cotton\nMade in South Korea	/images/beanpole/402.jpg	\N	리넨 혼방 옥스포드 소재 — 통기성 우수\n스탠다드핏 — 균형 잡힌 실루엣\n앞면 슬랜트 포켓 + 뒷면 웰트 포켓\n벨트 루프 포함\n드라이클리닝 권장	남성	/images/beanpole/402.jpg,/images/beanpole/402_2.jpg,/images/beanpole/402_3.jpg	\N		/images/beanpole/402.jpg
404	1	헤리티지클럽 스트라이프 칼라넥 티셔츠	빈폴 헤리티지클럽 라인의 스트라이프 칼라넥 티셔츠입니다. 클래식한 네이비 컬러에 섬세한 스트라이프 패턴과 칼라넥 디테일이 품격 있는 캐주얼 룩을 연출합니다.	159000	594	/images/beanpole/404.jpg	t	f	f	t	2026-04-13 03:44:57.814456	2026-04-13 05:40:42.119778	BEANPOLE	#1e2d4e	Navy	100% Cotton\nMade in South Korea	/images/beanpole/404.jpg	\N	헤리티지클럽 스트라이프 패턴\n칼라넥 디자인\n고밀도 면 소재\n레귤러 핏\n기계 세탁 가능	공용	/images/beanpole/404.jpg,/images/beanpole/404_2.jpg,/images/beanpole/404_3.jpg	\N		/images/beanpole/404.jpg
405	1	피케 칼라넥 반소매 티셔츠	피케 소재로 제작된 빈폴의 칼라넥 반소매 티셔츠입니다. 피케 특유의 입체적인 텍스처와 칼라넥 디테일이 폴로 셔츠의 클래식한 분위기를 자아냅니다.	149000	594	/images/beanpole/405.jpg	f	t	f	t	2026-04-13 03:44:57.814456	2026-04-13 05:40:47.131117	BEANPOLE	#8a9a6a	Khaki	100% Cotton Piqué\nMade in South Korea	/images/beanpole/405.jpg	\N	피케 소재 — 입체적인 텍스처\n칼라넥 디자인\n반소매 — 여름 착용 최적화\n레귤러 핏\n기계 세탁 가능	공용	/images/beanpole/405.jpg,/images/beanpole/405_2.jpg,/images/beanpole/405_3.jpg	\N		/images/beanpole/405.jpg
101	2	루즈핏 캔버스 유틸리티 워크 팬츠	러기드 플렉스 원단으로 제작된 루즈핏 스트레이트 진입니다. 움직임에 유연하게 대응하는 스트레치 소재로 작업 현장부터 일상까지 편안하게 착용할 수 있습니다.	140000	594	/images/carhartt/101.png	t	f	f	t	2026-04-13 03:44:57.804035	2026-04-13 05:41:13.169242	CARHARTT	#b8a96a	Duck Brown	98% Cotton, 2% Elastane\nMade in Mexico	/images/carhartt/101.png	\N	Rugged Flex 원단 — 스트레치 편안함\n루즈핏 — 넉넉한 허벅지 공간\n5포켓 디자인\n내구성 강한 YKK 지퍼\n허리 밴드 내부 바텍 처리	남성	/images/carhartt/101.png,/images/carhartt/101_2.png,/images/carhartt/101_3.png,/images/carhartt/101_4.png	\N		/images/carhartt/101.png
204	1	NAS 밴드 티	힙합 레전드 NAS와의 협업으로 탄생한 그래픽 티셔츠입니다. 클래식한 화이트 바탕에 빈티지 감성의 그래픽이 프린트되어 있습니다.	45000	594	/images/levis/204.jpg	t	f	f	t	2026-04-13 03:44:57.80884	2026-04-13 05:41:41.15641	LEVI'S	#f5f5f5	White	100% Cotton\nMade in Bangladesh	/images/levis/204.jpg	\N	NAS 협업 한정 그래픽\n100% 순면 소재\n레귤러 핏\n크루넥 디자인\n스크린 프린트 그래픽	공용	/images/levis/204.jpg,/images/levis/204_2.jpg,/images/levis/204_3.jpg	\N		/images/levis/204.jpg
205	1	LEVI'S® X BARBOUR 그래픽 티셔츠	리바이스와 영국 헤리티지 브랜드 바버의 콜라보레이션 티셔츠입니다. 두 브랜드의 아이코닉한 로고가 결합된 특별한 그래픽이 특징입니다.	109000	594	/images/levis/205.jpg	t	f	f	t	2026-04-13 03:44:57.80884	2026-04-13 05:41:44.213601	LEVI'S	#f0ede0	Ivory	100% Cotton\nMade in Portugal	/images/levis/205.jpg	\N	리바이스 × 바버 콜라보 그래픽\n아이보리 컬러 순면 소재\n레귤러 핏\n크루넥\n양면 프린트	공용	/images/levis/205.jpg,/images/levis/205_2.jpg,/images/levis/205_3.jpg	\N		/images/levis/205.jpg
304	1	트리 로고 레이어드 롱슬리브	디키즈 트리 로고가 새겨진 레이어드 롱슬리브 티셔츠입니다. 멜란지 소재의 자연스러운 텍스처와 레이어드 디테일이 캐주얼한 스트리트 룩을 완성합니다.	69000	594	/images/dickies/304.jpg	t	f	f	t	2026-04-13 03:44:57.811313	2026-04-13 05:42:10.996647	DICKIES	#9e9e9e	Melange	60% Cotton, 40% Polyester\nMade in Cambodia	/images/dickies/304.jpg	\N	멜란지 혼방 소재 — 자연스러운 텍스처\n트리 로고 그래픽\n레이어드 디테일\n롱슬리브 디자인\n릴렉스 핏	공용	/images/dickies/304.jpg,/images/dickies/304_2.jpg,/images/dickies/304_3.jpg	\N		/images/dickies/304.jpg
305	1	파인 스트라이프 포켓 티셔츠	파인 스트라이프 패턴이 더해진 디키즈의 포켓 티셔츠입니다. 그레이 베이스에 섬세한 스트라이프 디테일이 단정하고 세련된 캐주얼 룩을 연출합니다.	59000	594	/images/dickies/305.jpg	t	f	f	t	2026-04-13 03:44:57.811313	2026-04-13 05:42:13.366935	DICKIES	#9e9e9e	Gray	100% Cotton\nMade in Bangladesh	/images/dickies/305.jpg	\N	파인 스트라이프 패턴\n좌측 가슴 포켓\n100% 순면 소재\n레귤러 핏\n기계 세탁 가능	공용	/images/dickies/305.jpg,/images/dickies/305_2.jpg,/images/dickies/305_3.jpg	\N		/images/dickies/305.jpg
301	3	루즈핏 카펜터 유틸리티 데님 팬츠	디키즈의 카펜터 스타일 루즈핏 데님 팬츠입니다. 넉넉한 실루엣과 다양한 유틸리티 포켓으로 작업 현장과 일상 모두에 실용적으로 활용할 수 있습니다.	105000	594	/images/dickies/301.jpg	f	t	f	t	2026-04-13 03:44:57.811313	2026-04-13 05:42:16.475164	DICKIES	#5b7fa6	Medium Blue	100% Cotton\nMade in Bangladesh	/images/dickies/301.jpg	\N	루즈핏 — 넉넉한 허리·허벅지 공간\n카펜터 스타일 유틸리티 포켓\n미디엄 블루 워시 데님\n강화 스티칭 처리\n기계 세탁 가능	남성	/images/dickies/301.jpg,/images/dickies/301_2.jpg,/images/dickies/301_3.jpg	\N		/images/dickies/301.jpg
302	2	워시드 스네이크 더블니 팬츠	스네이크 텍스처 워시 처리와 더블니 구조가 결합된 디키즈의 워크 팬츠입니다. 무릎 이중 강화로 내구성을 높이고, 독특한 워시드 블랙 컬러가 스트리트 감성을 더합니다.	119000	594	/images/dickies/302.jpg	f	t	f	t	2026-04-13 03:44:57.811313	2026-04-13 05:42:18.952618	DICKIES	#1a1a1a	Black	100% Cotton\nMade in Bangladesh	/images/dickies/302.jpg	\N	워시드 스네이크 텍스처 가공\n더블니 — 무릎 이중 강화\n루즈핏 실루엣\n멀티 포켓 구성\n강화 스티칭 처리	남성	/images/dickies/302.jpg,/images/dickies/302_2.jpg,/images/dickies/302_3.jpg	\N		/images/dickies/302.jpg
106	1	칼하트 포스 릴랙스 티셔츠	칼하트 포스 기술이 적용된 릴렉스 핏 티셔츠입니다. FastDry 기능으로 땀을 빠르게 흡수·발산하며 작업 중에도 쾌적한 착용감을 유지합니다.	57500	594	/images/carhartt/106.png	t	f	f	t	2026-04-13 03:44:57.804035	2026-04-13 05:41:16.318331	CARHARTT	#f5f5f5	White	65% Cotton, 35% Polyester\nMade in Cambodia	/images/carhartt/106.png	\N	Carhartt Force™ FastDry 기술 — 빠른 흡습 발산\n릴렉스 핏 — 편안한 실루엣\n통기성 강화 소재\n냄새 방지 처리\n기계 세탁 가능	공용	/images/carhartt/106.png,/images/carhartt/106_2.png,/images/carhartt/106_3.png	\N		/images/carhartt/106.png
102	2	루즈 스트레이트 러기드 플렉스 덕 트라우저	러기드 플렉스 덕 원단으로 제작된 슬림 테이퍼드 팬츠입니다. 타마크 컬러의 세련된 실루엣과 스트레치 소재의 편안함을 동시에 갖춘 작업복 겸 캐주얼 팬츠입니다.	122000	594	/images/carhartt/102.png	f	t	f	t	2026-04-13 03:44:57.804035	2026-04-13 05:41:19.728526	CARHARTT	#3a3a3a	Shadow	100% Cotton Duck Canvas\nMade in Mexico	/images/carhartt/102.png	\N	Rugged Flex 덕 원단 — 스트레치 편안함\n슬림 테이퍼드 핏\n내구성 강화 무릎 구조\n다용도 포켓 구성\n허리 밴드 내부 바텍 처리	남성	/images/carhartt/102.png,/images/carhartt/102_2.png,/images/carhartt/102_3.png,/images/carhartt/102_4.png	\N		/images/carhartt/102.png
103	2	아이코닉 B01 펌 덕 더블 프론트 트라우저	칼하트의 아이코닉 B01 펌 덕 소재 더블프런트 팬츠입니다. 무릎 부분을 이중으로 강화하여 장시간 무릎 작업에도 뛰어난 내구성을 발휘하는 작업복의 정석입니다.	209000	594	/images/carhartt/103.png	f	t	f	t	2026-04-13 03:44:57.804035	2026-04-13 05:41:22.251364	CARHARTT	#5c3318	Carhartt Brown	100% Cotton Duck\nMade in USA	/images/carhartt/103.png	\N	펌 덕 소재 — 고강도 내구성\n더블프런트 무릎 이중 강화\n루즈핏 — 넉넉한 착용감\n멀티 포켓 구성\n허머 루프 및 룰 포켓 포함	남성	/images/carhartt/103.png,/images/carhartt/103_2.png,/images/carhartt/103_3.png,/images/carhartt/103_4.png	\N		/images/carhartt/103.png
105	1	디어본 릴랙스드 샴록 티셔츠	칼하트 베어본 라인의 샴록 그래픽 릴렉스 티셔츠입니다. 몰트 컬러 바탕에 샴록 모티프 그래픽이 캐주얼하고 유니크한 분위기를 연출합니다.	52300	594	/images/carhartt/105.png	f	f	f	t	2026-04-13 03:44:57.804035	2026-04-13 05:41:26.31613	CARHARTT	#c9a96e	Malt	100% Cotton\nMade in Bangladesh	/images/carhartt/105.png	25	100% 순면 소재\n릴렉스 핏 — 여유로운 실루엣\n샴록 그래픽 프린트\n크루넥 디자인\n기계 세탁 가능	공용	/images/carhartt/105.png,/images/carhartt/105_2.png,/images/carhartt/105_3.png	\N		/images/carhartt/105.png
104	1	디어본 루즈핏 포켓 티셔츠	칼하트 베어본 라인의 릴렉스 포켓 티셔츠입니다. 군더더기 없는 심플한 디자인에 좌측 가슴 포켓이 포인트가 되는 기본 아이템입니다.	44000	594	/images/carhartt/104.png	f	f	f	t	2026-04-13 03:44:57.804035	2026-04-13 05:41:34.784091	CARHARTT	#1a1a1a	Black	100% Cotton\nMade in Bangladesh	/images/carhartt/104.png	30	100% 순면 소재\n릴렉스 핏 — 여유로운 실루엣\n좌측 가슴 포켓\n크루넥 디자인\n강화 솔기 처리	공용	/images/carhartt/104.png,/images/carhartt/104_2.png,/images/carhartt/104_3.png	\N		/images/carhartt/104.png
206	1	헤비웨이트 포켓 티셔츠	두툼한 헤비웨이트 순면으로 제작된 기본 포켓 티셔츠입니다. 단순하지만 견고한 구조로 일상에서 가장 자주 손이 가는 기본 아이템입니다.	35000	594	/images/levis/206.jpg	f	t	f	t	2026-04-13 03:44:57.80884	2026-04-13 05:41:48.679236	LEVI'S	#ffffff	White	100% Cotton\nMade in Bangladesh	/images/levis/206.jpg	\N	헤비웨이트 순면 소재\n좌측 가슴 포켓\n레귤러 핏\n크루넥\n강화 솔기 처리	공용	/images/levis/206.jpg,/images/levis/206_2.jpg,/images/levis/206_3.jpg	\N		/images/levis/206.jpg
203	3	505™ 레귤러 라이트웨이트 진	505™ 레귤러 핏의 가벼운 데님 버전입니다. 얇은 원단으로 여름철에도 쾌적하게 착용할 수 있으며, 라이트 워시가 캐주얼한 분위기를 완성합니다.	169000	594	/images/levis/203.jpg	f	t	f	t	2026-04-13 03:44:57.80884	2026-04-13 05:41:51.451683	LEVI'S	#a8c4d8	Light Wash	100% Cotton\nMade in Mexico	/images/levis/203.jpg	\N	레귤러 핏 — 편안한 표준 실루엣\n라이트웨이트 데님 원단\nLight Wash\n스트레이트 레그\n클래식 5포켓 디자인	남성	/images/levis/203.jpg,/images/levis/203_2.jpg,/images/levis/203_3.jpg	\N		/images/levis/203.jpg
202	3	565™ 루즈 스트레이트 진	565™는 허리부터 허벅지까지 넉넉한 루즈 핏을 제공하는 스트레이트 진입니다. 다크 인디고 플랫 피니쉬 워시로 깔끔하고 절제된 분위기를 연출합니다.	99000	594	/images/levis/202.jpg	f	f	f	t	2026-04-13 03:44:57.80884	2026-04-13 05:41:55.688434	LEVI'S	#1e2a3a	Dark Indigo Flat Finish	100% Cotton\nMade in Bangladesh	/images/levis/202.jpg	10	루즈 핏 — 넉넉한 허리와 허벅지\n스트레이트 레그\n다크 인디고 플랫 피니쉬 워시\n클래식 5포켓\n100% 코튼	남성	/images/levis/202.jpg,/images/levis/202_2.jpg,/images/levis/202_3.jpg	\N		/images/levis/202.jpg
201	3	555™ 릴렉스 스트레이트 진	1873년 탄생한 오리지널 진의 현대적 해석. 555™는 릴렉스 핏과 스트레이트 레그로 여유로운 실루엣을 완성합니다. 클래식한 다크 인디고 워시로 어떤 스타일에도 잘 어울립니다.	159000	594	/images/levis/201.jpg	f	f	f	t	2026-04-13 03:44:57.80884	2026-04-13 05:42:00.865673	LEVI'S	#1e2d4e	Darkindigo	99% Cotton, 1% Elastane\nMade in Bangladesh	/images/levis/201.jpg	15	릴렉스 핏 — 허벅지부터 여유로운 실루엣\n스트레이트 레그 라인\n클래식 5포켓 디자인\n다크 인디고 워시\n99% 코튼 / 1% 엘라스테인	남성	/images/levis/201.jpg,/images/levis/201_2.jpg,/images/levis/201_3.jpg	\N		/images/levis/201.jpg
303	3	더블니 카펜터 데님 팬츠	카펜터 스타일과 더블니 구조가 결합된 디키즈의 블랙 데님 팬츠입니다. 무릎 이중 강화와 유틸리티 포켓으로 작업 현장에서의 실용성과 내구성을 극대화했습니다.	99000	594	/images/dickies/303.jpg	f	f	f	t	2026-04-13 03:44:57.811313	2026-04-13 05:42:24.615548	DICKIES	#1a1a1a	Black	100% Cotton\nMade in Bangladesh	/images/dickies/303.jpg	20	더블니 — 무릎 이중 강화\n카펜터 유틸리티 포켓\n블랙 데님 소재\n강화 스티칭 처리\n기계 세탁 가능	남성	/images/dickies/303.jpg,/images/dickies/303_2.jpg,/images/dickies/303_3.jpg	\N		/images/dickies/303.jpg
407	1	올오버 스컬 프린트 반팔 티셔츠	전면에 격자무늬와 해골 로고가 반복적으로 들어간 디자인입니다.	89000	594	/new/dickies/Tops/dt01.jpg	f	f	t	t	2026-04-13 04:09:24.224987	2026-04-13 04:09:24.225004	DICKIES	#1a1a1a	Black	\N	/new/dickies/Tops/dt05.jpg	\N	\N	공용	/new/dickies/Tops/dt01.jpg,/new/dickies/Tops/dt02.jpg,/new/dickies/Tops/dt03.jpg,/new/dickies/Tops/dt04.jpg,/new/dickies/Tops/dt05.jpg	\N		/new/dickies/Tops/dt01.jpg
408	1	올오버 스컬 프린트 반팔 티셔츠		89000	594	/new/dickies/Tops/du_13.jpg	f	f	f	t	2026-04-13 05:21:17.75958	2026-04-13 05:21:17.759609	DICKIES	#f2eeee	Ivory	\N	/new/dickies/Etc/dd_01.jpg	\N	\N	공용	/new/dickies/Tops/du_13.jpg,/new/dickies/Tops/du_12.jpg,/new/dickies/Tops/du_11.jpg,/new/dickies/Etc/dd_01.jpg	\N		/new/dickies/Tops/du_13.jpg
409	1	리바이스 x 키코 코스타디노브	최근 큰 화제가 되었던 2024년 협업 라인업입니다.	569000	594	/new/levis/Etc/cc16.jpg	f	f	t	t	2026-04-13 05:24:01.622674	2026-04-13 05:24:01.622711	LEVI'S	#16389c	Navy	\N	/new/levis/Etc/cc21.jpg	\N	\N	여성	/new/levis/Etc/cc16.jpg,/new/levis/Etc/cc19.jpg,/new/levis/Etc/cc21.jpg	\N		/new/levis/Etc/cc16.jpg
410	1	OG 액티브 자켓 해밀턴 브라운 스톤 캔버스		259000	594	/new/carhartt/Tops/ku_01.jpg	f	f	t	t	2026-04-13 05:27:14.58972	2026-04-13 05:27:14.589749	CARHARTT	#b05111	one	\N	/new/carhartt/Etc/kk_01.jpg	\N	\N	공용	/new/carhartt/Tops/ku_01.jpg,/new/carhartt/Tops/ku_02.JPG,/new/carhartt/Tops/ku_03.JPG,/new/carhartt/Tops/ku_04.JPG,/new/carhartt/Etc/kk_01.jpg	\N		/new/carhartt/Tops/ku_01.jpg
411	2	싱글 니 팬츠 카마노 블루 린스드		179000	693	/new/levis/Bottoms/02/lb29.jpg	f	f	f	t	2026-04-13 05:29:36.922391	2026-04-13 05:29:36.922409	LEVI'S	#0f0d36	Navy	\N	/new/levis/Bottoms/02/lb24.jpg	\N	\N	여성	/new/levis/Bottoms/02/lb21.jpg,/new/levis/Bottoms/02/lb22.jpg,/new/levis/Bottoms/02/lb23.jpg,/new/levis/Bottoms/02/lb24.jpg,/new/levis/Bottoms/02/lb25.jpg,/new/levis/Bottoms/02/lb26.jpg,/new/levis/Bottoms/02/lb27.jpg,/new/levis/Bottoms/02/lb28.jpg,/new/levis/Bottoms/02/lb29.jpg,/new/levis/Bottoms/02/lb30.jpg,/new/levis/Bottoms/02/lb31.jpg,/new/levis/Bottoms/02/lb32.jpg,/new/levis/Bottoms/02/lb33.jpg	\N		/new/levis/Bottoms/02/lb29.jpg
412	1	여성 가이드 클럽 티셔츠		199000	594	/new/beanpole/b_01.jpg	f	f	t	t	2026-04-13 05:30:53.97951	2026-04-13 05:30:53.979523	BEANPOLE	#1a1e84	Navy	\N	/new/beanpole/b_02.jpg	\N	\N	여성	/new/beanpole/b_01.jpg,/new/beanpole/b_02.jpg,/new/beanpole/b_03.jpg	\N		/new/beanpole/b_01.jpg
401	2	피그먼트 워싱 컴포트핏 치노 팬츠	피그먼트 워싱 처리로 자연스러운 색감을 살린 빈폴의 컴포트핏 치노 팬츠입니다. 부드러운 베이지 컬러와 편안한 핏이 일상과 격식을 모두 소화합니다.	239000	594	/images/beanpole/401.jpg	t	f	f	t	2026-04-13 03:44:57.814456	2026-04-13 05:40:34.723661	BEANPOLE	#d4c5a9	Beige	97% Cotton, 3% Elastane\nMade in Vietnam	/images/beanpole/401.jpg	\N	피그먼트 워싱 가공 — 자연스러운 색감\n컴포트핏 — 여유로운 착용감\n치노 소재\n앞면 슬랜트 포켓 + 뒷면 웰트 포켓\n기계 세탁 가능	남성	/images/beanpole/401.jpg,/images/beanpole/401_2.jpg,/images/beanpole/401_3.jpg	\N		/images/beanpole/401.jpg
406	1	헤리티지클럽 라운드넥 반소매 티셔츠	빈폴 헤리티지클럽 라인의 라운드넥 반소매 티셔츠입니다. 깔끔한 화이트 컬러와 부드러운 소재가 어떤 스타일에도 잘 어울리는 기본 아이템입니다.	89000	594	/images/beanpole/406.jpg	f	f	f	t	2026-04-13 03:44:57.814456	2026-04-13 05:40:54.739952	BEANPOLE	#f5f5f5	White	100% Cotton\nMade in South Korea	/images/beanpole/406.jpg	15	헤리티지클럽 라인\n라운드넥 디자인\n고밀도 면 소재\n레귤러 핏\n기계 세탁 가능	공용	/images/beanpole/406.jpg,/images/beanpole/406_2.jpg,/images/beanpole/406_3.jpg	\N		/images/beanpole/406.jpg
403	2	가먼트다잉 세미와이드 팬츠	가먼트 다잉 공법으로 옷 전체를 염색한 빈폴의 세미와이드 팬츠입니다. 깊이 있는 네이비 컬러와 여유로운 세미와이드 실루엣이 세련된 캐주얼 룩을 완성합니다.	239000	594	/images/beanpole/403.jpg	f	f	f	t	2026-04-13 03:44:57.814456	2026-04-13 05:41:06.512499	BEANPOLE	#1e2d4e	Navy	100% Cotton\nMade in South Korea	/images/beanpole/403.jpg	20	가먼트 다잉 공법 — 깊이 있는 발색\n세미와이드 실루엣\n편안한 허리 밴드 구조\n앞면 포켓 + 뒷면 웰트 포켓\n드라이클리닝 권장	남성	/images/beanpole/403.jpg,/images/beanpole/403_2.jpg,/images/beanpole/402_3.jpg	\N		/images/beanpole/403.jpg
306	1	워시드 스네이크 로고 티셔츠	스네이크 텍스처 워시 가공이 적용된 디키즈 로고 티셔츠입니다. 워시드 블랙 특유의 빈티지한 질감과 스네이크 패턴 로고가 개성 있는 스트리트 룩을 완성합니다.	59000	594	/images/dickies/306.jpg	f	f	f	t	2026-04-13 03:44:57.811313	2026-04-13 05:42:28.811229	DICKIES	#1a1a1a	Black	100% Cotton\nMade in Bangladesh	/images/dickies/306.jpg	25	워시드 스네이크 텍스처 가공\n디키즈 로고 프린트\n100% 순면 소재\n릴렉스 핏\n기계 세탁 가능	공용	/images/dickies/306.jpg,/images/dickies/306_2.jpg,/images/dickies/306_3.jpg	\N		/images/dickies/306.jpg
\.


--
-- Data for Name: qna; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.qna (id, member_id, product_id, title, content, answer, answered_by, is_secret, is_active, status, created_at, updated_at, answered_at) FROM stdin;
1	1	407	안녕하세요	심심해요	\N	\N	t	t	PENDING	2026-04-13 14:01:53.591498	2026-04-13 14:01:53.591512	\N
\.


--
-- Data for Name: reviews; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.reviews (id, member_id, product_id, order_id, rating, content, is_active, created_at, updated_at, replied_at, reply) FROM stdin;
11	1	407	\N	5	좋아요	t	2026-04-13 14:13:01.433148	2026-04-13 14:13:01.433177	\N	\N
\.


--
-- Data for Name: site_settings; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.site_settings (setting_key, updated_at, value) FROM stdin;
magazine	2026-04-13 10:44:48.913173	[{"cat":"STYLE GUIDE","title":"봄을 입는 가장 쉬운 방법","excerpt":"칼하트 워크웨어부터 빈폴 클래식까지.","lg":true,"img":"/new/home/hero_21.jpg"},{"cat":"LOOKBOOK","title":"2026 SS Collection","excerpt":"","lg":false,"img":"/new/home/hero_27.jpg"},{"cat":"BRAND STORY","title":"워크웨어의 정직함","excerpt":"","lg":false,"img":"/new/home/hero_01.jpg"}]
stories	2026-04-13 10:45:18.344554	[{"cat":"— ARCHIVE","title":"워크웨어, 노동의 미학","excerpt":"한 세기를 견뎌온 소재와 봉제. 시간이 만든 단단한 정직함.","img":"/new/home/hero_04.jpg"},{"cat":"— STYLE","title":"2026, 절제의 봄","excerpt":"덜어낼수록 깊어지는 봄의 옷장.","img":"/new/home/hero_24.jpg"},{"cat":"— BRAND STORY","title":"데님, 150년의 푸른 역사","excerpt":"광부의 옷에서 세기의 클래식이 되기까지.","img":"/new/home/hero_22.jpg"}]
store_info	2026-04-13 10:45:52.117418	{"name":"FORME","ceo":"FORME","bizNo":"000-00-00000","phone":"1588-0000","email":"info@forme.kr","address":"서울특별시 강남구 테헤란로 000"}
hero_slides	2026-04-13 10:47:32.325023	[{"url":"/new/home/hero_16.jpg","alt":"히어로 7"},{"url":"/new/home/hero_13.jpg","alt":"히어로 5"},{"url":"/new/home/hero_18.jpg","alt":"히어로 8"},{"url":"/new/home/hero_15.jpg","alt":"히어로 6"},{"url":"/new/home/hero_48.jpg","alt":"히어로 15"},{"url":"/new/home/hero_01.jpg","alt":"히어로 1"},{"url":"/new/home/hero_04.jpg","alt":"히어로 2"},{"url":"/new/home/hero_21.jpg","alt":"히어로 9"},{"url":"/new/home/hero_11.jpg","alt":"히어로 3"},{"url":"/new/home/hero_12.jpg","alt":"히어로 4"},{"url":"/new/home/hero_22.jpg","alt":"히어로 10"},{"url":"/new/home/hero_24.jpg","alt":"히어로 11"},{"url":"/new/home/hero_27.jpg","alt":"히어로 12"},{"url":"/new/home/hero_28.jpg","alt":"히어로 13"},{"url":"/new/home/hero_40.jpg","alt":"히어로 14"}]
brand_settings	2026-04-13 14:00:27.27463	[{"key":"beanpole","label":"BEANPOLE","country":"한국 🇰🇷","color":"#103728","teamImage":"/main1.jpg","heroImage":"/new/brand/22.jpg","heroHeight":700,"heroPosition":"center center","gridCols":3},{"key":"carhartt","label":"CARHARTT","country":"영국 🇬🇧","color":"#9C4F18","teamImage":"/main2.jpg","heroImage":"/new/brand/11.jpg","heroHeight":700,"heroPosition":"center top","gridCols":3},{"key":"levis","label":"LEVI'S","country":"유럽 🇪🇺","color":"#8E1C28","teamImage":"/main3.jpg","heroImage":"/new/brand/44.jpg","heroHeight":700,"heroPosition":"center top","gridCols":3},{"key":"dickies","label":"DICKIES","country":"미국 🇺🇸","color":"#1A1A1A","teamImage":"/main4.jpg","heroImage":"/new/brand/55.jpg","heroHeight":700,"heroPosition":"center top","gridCols":3}]
\.


--
-- Data for Name: wishlists; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.wishlists (id, created_at, member_id, product_id) FROM stdin;
\.


--
-- Name: boards_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.boards_id_seq', 1, false);


--
-- Name: cart_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.cart_items_id_seq', 1, false);


--
-- Name: categories_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.categories_id_seq', 3, true);


--
-- Name: comments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.comments_id_seq', 1, false);


--
-- Name: deliveries_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.deliveries_id_seq', 1, false);


--
-- Name: member_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.member_id_seq', 1, true);


--
-- Name: order_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.order_items_id_seq', 1, false);


--
-- Name: orders_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.orders_id_seq', 1, false);


--
-- Name: page_views_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.page_views_id_seq', 122, true);


--
-- Name: product_sizes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.product_sizes_id_seq', 343, true);


--
-- Name: products_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.products_id_seq', 412, true);


--
-- Name: qna_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.qna_id_seq', 1, true);


--
-- Name: reviews_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.reviews_id_seq', 12, true);


--
-- Name: wishlists_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.wishlists_id_seq', 1, true);


--
-- Name: boards boards_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.boards
    ADD CONSTRAINT boards_pkey PRIMARY KEY (id);


--
-- Name: cart_items cart_items_member_id_product_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_member_id_product_id_key UNIQUE (member_id, product_id);


--
-- Name: cart_items cart_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id);


--
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);


--
-- Name: comments comments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);


--
-- Name: deliveries deliveries_order_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT deliveries_order_id_key UNIQUE (order_id);


--
-- Name: deliveries deliveries_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT deliveries_pkey PRIMARY KEY (id);


--
-- Name: member member_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.member
    ADD CONSTRAINT member_email_key UNIQUE (email);


--
-- Name: member member_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.member
    ADD CONSTRAINT member_pkey PRIMARY KEY (id);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: page_views page_views_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.page_views
    ADD CONSTRAINT page_views_pkey PRIMARY KEY (id);


--
-- Name: product_sizes product_sizes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_sizes
    ADD CONSTRAINT product_sizes_pkey PRIMARY KEY (id);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: qna qna_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.qna
    ADD CONSTRAINT qna_pkey PRIMARY KEY (id);


--
-- Name: reviews reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);


--
-- Name: site_settings site_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.site_settings
    ADD CONSTRAINT site_settings_pkey PRIMARY KEY (setting_key);


--
-- Name: reviews uk4gic5ufv2meksy3fve0ycs691; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT uk4gic5ufv2meksy3fve0ycs691 UNIQUE (member_id, order_id, product_id);


--
-- Name: cart_items ukl1p51k88sk9mauxatwfjafdee; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT ukl1p51k88sk9mauxatwfjafdee UNIQUE (member_id, product_id);


--
-- Name: wishlists uks9eoubbmmdosfursnp0q0vjcf; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wishlists
    ADD CONSTRAINT uks9eoubbmmdosfursnp0q0vjcf UNIQUE (member_id, product_id);


--
-- Name: wishlists wishlists_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wishlists
    ADD CONSTRAINT wishlists_pkey PRIMARY KEY (id);


--
-- Name: boards boards_member_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.boards
    ADD CONSTRAINT boards_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id) ON DELETE CASCADE;


--
-- Name: cart_items cart_items_member_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id) ON DELETE CASCADE;


--
-- Name: cart_items cart_items_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id) ON DELETE CASCADE;


--
-- Name: comments comments_board_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_board_id_fkey FOREIGN KEY (board_id) REFERENCES public.boards(id) ON DELETE CASCADE;


--
-- Name: comments comments_member_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id) ON DELETE CASCADE;


--
-- Name: deliveries deliveries_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT deliveries_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- Name: product_sizes fk4isa0j51hpdn7cx04m831jic4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_sizes
    ADD CONSTRAINT fk4isa0j51hpdn7cx04m831jic4 FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: wishlists fk9sje0cyq2g0mtuoy1hafchhqx; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wishlists
    ADD CONSTRAINT fk9sje0cyq2g0mtuoy1hafchhqx FOREIGN KEY (member_id) REFERENCES public.member(id);


--
-- Name: wishlists fkl7ao98u2bm8nijc1rv4jobcrx; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wishlists
    ADD CONSTRAINT fkl7ao98u2bm8nijc1rv4jobcrx FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: order_items order_items_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;


--
-- Name: order_items order_items_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: orders orders_member_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id);


--
-- Name: products products_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.categories(id);


--
-- Name: qna qna_answered_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.qna
    ADD CONSTRAINT qna_answered_by_fkey FOREIGN KEY (answered_by) REFERENCES public.member(id);


--
-- Name: qna qna_member_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.qna
    ADD CONSTRAINT qna_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id) ON DELETE CASCADE;


--
-- Name: qna qna_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.qna
    ADD CONSTRAINT qna_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id) ON DELETE SET NULL;


--
-- Name: reviews reviews_member_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_member_id_fkey FOREIGN KEY (member_id) REFERENCES public.member(id) ON DELETE CASCADE;


--
-- Name: reviews reviews_order_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- Name: reviews reviews_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict 5VJWAOdOJmPwR7Uoaa1DIIDdYS6WbHEOfbHNKEvEZEjoN42LxWTLfIgPKNolS7S

