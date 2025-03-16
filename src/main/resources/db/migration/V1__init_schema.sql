create table if not exists address
(
    id      int auto_increment comment '아이디'
        primary key,
    hcode   varchar(10) not null comment '행정동코드',
    sido    varchar(12) not null comment '시도명',
    sigungu varchar(10) not null comment '시군구명',
    hname   varchar(12) not null comment '행정동명',
    bcode   varchar(10) not null comment '법정동코드',
    bname   varchar(10) not null comment '법정동명'
)
    comment '주소 데이터' charset = utf8mb4;

create table if not exists admin
(
    id         int auto_increment comment '아이디'
        primary key,
    login_id   varchar(50)                          not null comment '로그인 아이디',
    authority  enum ('MASTER', 'MANAGER')           not null comment '권한',
    password   varchar(60)                          not null comment '비밀번호',
    name       varchar(20)                          not null comment '이름',
    tel        varchar(11)                          not null comment '전화번호',
    state      enum ('ACTIVE', 'BANNED', 'DELETED') not null comment '상태',
    created_at datetime default current_timestamp() not null comment '생성 일시'
)
    comment '관리자' charset = utf8mb3;

create table if not exists admin_auth
(
    admin_id          int        not null comment '관리자 아이디'
        primary key,
    access_user       tinyint(1) not null comment '사용자 관리',
    access_product    tinyint(1) not null comment '상품 관리',
    access_order      tinyint(1) not null comment '주문 관리',
    access_settlement tinyint(1) not null comment '정산 관리',
    access_board      tinyint(1) not null comment '게시판',
    access_promotion  tinyint(1) not null comment '프로모션',
    access_setting    tinyint(1) not null comment '설정',
    constraint FK_admin_auth_admin_id_admin_id
        foreign key (admin_id) references admin (id)
)
    comment '관리자 권한' charset = utf8mb4;

create table if not exists admin_log
(
    id         varchar(20)                                                                               not null comment '아이디'
        primary key,
    admin_id   int                                                                                       not null comment '관리자 아이디',
    type       enum ('USER', 'PARTNER', 'PRODUCT', 'ORDER', 'SETTLEMENT', 'REPORT', 'INQUIRY', 'COUPON') not null comment '타입',
    target_id  varchar(20)                                                                               not null comment '대상 아이디',
    content    varchar(300)                                                                              not null comment '내용',
    created_at datetime default current_timestamp()                                                      not null comment '생성일시'
)
    comment '관리자 로그' charset = utf8mb4;

create table if not exists admin_log_seq
(
    next_not_cached_value bigint(21)          not null,
    minimum_value         bigint(21)          not null,
    maximum_value         bigint(21)          not null,
    start_value           bigint(21)          not null comment 'start value when sequences is created or value if RESTART is used',
    increment             bigint(21)          not null comment 'increment value',
    cache_size            bigint(21) unsigned not null,
    cycle_option          tinyint(1) unsigned not null comment '0 if no cycles are allowed, 1 if the sequence should begin a new cycle when maximum_value is passed',
    cycle_count           bigint(21)          not null comment 'How many cycles have been done'
)
    charset = utf8mb4;

create table if not exists bank_code
(
    id   int auto_increment comment '아이디'
        primary key,
    code varchar(10) not null comment '코드',
    name varchar(20) not null comment '은행명'
)
    comment '은행 코드' charset = utf8mb4;

create table if not exists banner
(
    id          int auto_increment comment '아이디'
        primary key,
    state       enum ('ACTIVE', 'INACTIVE')                                                  not null comment '상태',
    type        enum ('NONE', 'CURATION', 'NOTICE', 'CATEGORY', 'PC_WEB', 'MAIN', 'MY_PAGE') not null comment '타입',
    image       text                                                                         not null comment '이미지',
    curation_id int                                                                          null comment '큐레이션 아이디',
    notice_id   int                                                                          null comment '공지사항 아이디',
    category_id int                                                                          null comment '카테고리 아이디',
    link        text                                                                         null comment '링크',
    sort_no     int                                                                          null comment '정렬 순서'
)
    comment '배너' charset = utf8mb3;

create table if not exists category
(
    id                 int auto_increment comment '아이디'
        primary key,
    parent_category_id int         null comment '부모 카테고리',
    image              text        null comment '이미지',
    name               varchar(20) not null comment '이름',
    constraint FK_category_parent_category_id_category_id
        foreign key (parent_category_id) references category (id)
)
    comment '카테고리' charset = utf8mb3;

create table if not exists compare_filter
(
    id   int auto_increment comment '아이디'
        primary key,
    name varchar(20) not null comment '이름'
)
    comment '비교하기 필터' charset = utf8mb4;

create table if not exists category_filter_map
(
    compare_filter_id int not null comment '비교하기 필터 아이디',
    category_id       int not null comment '카테고리 아이디',
    primary key (compare_filter_id, category_id),
    constraint FK_category_filter_map_category_id_category_id
        foreign key (category_id) references category (id),
    constraint FK_category_filter_map_compare_filter_id_compare_filter_id
        foreign key (compare_filter_id) references compare_filter (id)
)
    comment '카테고리 필터 매핑' charset = utf8mb4;

create table if not exists coupon
(
    id          int auto_increment comment '아이디'
        primary key,
    state       enum ('ACTIVE', 'DELETED')           default 'ACTIVE' not null,
    title       varchar(100)                                          not null comment '제목',
    type        enum ('AMOUNT', 'RATE')                               not null comment '할인 유형',
    amount      int                                                   not null comment '할인율',
    start_at    datetime                                              not null comment '사용 시작 기간',
    end_at      datetime                                              null comment '사용 종료 기간',
    min_price   int                                  default 0        not null comment '최소 결제 금액',
    public_type enum ('PUBLIC', 'PRIVATE', 'SYSTEM') default 'PUBLIC' not null comment '발급 유형'
)
    comment '쿠폰' charset = utf8mb3;

create table if not exists curation
(
    id          int auto_increment comment '아이디'
        primary key,
    image       longtext                                null comment '이미지(S타입)',
    short_name  varchar(20)                             null comment '약어(S타입)',
    title       varchar(100)                            null comment '제목(L타입)',
    description varchar(200)                            null comment '설명(L타입)',
    type        enum ('SQUARE', 'S_SLIDER', 'L_SLIDER') null comment '표출 타입',
    sort_no     int                                     not null comment '정렬 순서',
    state       varchar(10) default 'ACTIVE'            null
)
    comment '큐레이션' charset = utf8mb4;

create table if not exists deliver_difficult_region
(
    post_code   int         not null comment '우편번호'
        primary key,
    region_name varchar(20) not null comment '지역 이름'
)
    comment '도서산간지역' charset = utf8mb4;

create table if not exists delivery_company
(
    code varchar(10) not null comment '코드'
        primary key,
    name varchar(50) not null comment '택배사 이름'
)
    comment '택배 회사' charset = utf8mb4;

create table if not exists fcm_token
(
    token   varchar(200) not null comment '토큰'
        primary key,
    user_id int          not null comment '유저 아이디'
)
    comment 'FCM 토큰' charset = utf8mb4;

create table if not exists grade
(
    id              int auto_increment comment '아이디'
        primary key,
    name            varchar(20) not null comment '이름',
    point_rate      float       not null comment '적립율',
    min_order_price int         not null comment '최소 주문 금액',
    min_order_count int         not null comment '최소 주문 횟수'
)
    charset = utf8mb4;

create table if not exists notice
(
    id                int auto_increment comment '아이디'
        primary key,
    type              enum ('NOTICE', 'FAQ')               not null comment '타입',
    title             varchar(200)                         not null comment '제목',
    content           text                                 not null comment '내용',
    is_representative tinyint(1)                           null,
    created_at        datetime default current_timestamp() not null comment '생성 일시',
    update_at         datetime                             null
)
    comment '공지사항' charset = utf8mb3;

create table if not exists order_seq
(
    next_not_cached_value bigint(21)          not null,
    minimum_value         bigint(21)          not null,
    maximum_value         bigint(21)          not null,
    start_value           bigint(21)          not null comment 'start value when sequences is created or value if RESTART is used',
    increment             bigint(21)          not null comment 'increment value',
    cache_size            bigint(21) unsigned not null,
    cycle_option          tinyint(1) unsigned not null comment '0 if no cycles are allowed, 1 if the sequence should begin a new cycle when maximum_value is passed',
    cycle_count           bigint(21)          not null comment 'How many cycles have been done'
);

create table if not exists recommend_compare_set
(
    id          int auto_increment comment '아이디'
        primary key,
    type        enum ('RECOMMEND', 'POPULAR') not null comment '타입',
    product1_id int                           not null comment '상품 1 아이디',
    product2_id int                           not null comment '상품 2 아이디',
    product3_id int                           not null comment '상품 3 아이디'
)
    comment '추천 비교하기 세트' charset = utf8mb4;

create table if not exists search_filter
(
    id   int auto_increment comment '아이디'
        primary key,
    name varchar(20) null comment '이름'
)
    comment '검색 필터' charset = utf8mb4;

create table if not exists search_filter_field
(
    id               int auto_increment comment '아이디'
        primary key,
    search_filter_id int         not null comment '검색 필터 아이디',
    field            varchar(20) not null comment '필드',
    constraint FK_search_filter_field_search_filter_id_search_filter_id
        foreign key (search_filter_id) references search_filter (id)
)
    comment '검색 필터 필드' charset = utf8mb4;

create table if not exists search_keyword
(
    keyword   varchar(100) not null comment '키워드'
        primary key,
    amount    int          not null comment '횟수',
    prev_rank int          null comment '이전 순위'
)
    comment '검색어' charset = utf8mb3;

create table if not exists site_information
(
    id          varchar(30)                not null comment '아이디. 아이디'
        primary key,
    type        enum ('ALL') default 'ALL' not null comment '타입',
    description varchar(100)               not null comment '설명. 설명',
    content     text                       not null comment '값. 값'
)
    comment '사이트 정보 테이블. 사이트 정보 테이블' charset = utf8mb3;

create table if not exists store
(
    id       int auto_increment comment '아이디'
        primary key,
    state    enum ('ACTIVE', 'BANNED', 'DELETED') not null comment '상태',
    login_id varchar(50)                          not null comment '로그인 아이디',
    password varchar(60)                          not null comment '비밀번호',
    join_at  datetime default current_timestamp() not null comment '가입 일시'
)
    comment '상점' charset = utf8mb3;

create table if not exists product
(
    id                           int auto_increment comment '아이디'
        primary key,
    store_id                     int                                                                                    not null comment '상점 아이디',
    category_id                  int                                                                                    null comment '카테고리 아이디',
    amount                       int                                                                                    null comment '개수',
    state                        enum ('ACTIVE', 'INACTIVE', 'INACTIVE_PARTNER', 'SOLD_OUT', 'DELETED') charset utf8mb3 not null comment '상태',
    images                       text charset utf8mb3                                                                   not null comment '이미지',
    title                        varchar(100) charset utf8mb3                                                           not null comment '제목',
    origin_price                 int                                                                                    null comment '원가',
    discount_rate                int                                                                                    null comment '할인률',
    delivery_info                text charset utf8mb3                                                                   not null comment '배송안내',
    deliver_fee                  int         default 0                                                                  null comment '배송비',
    deliver_fee_type             varchar(30) default 'FREE'                                                             not null comment '배송비 타입',
    min_order_price              int                                                                                    null comment '무료 배송 최소 금액',
    description_images           text charset utf8mb3                                                                   not null comment '상품 상세 이미지',
    expected_deliver_day         int                                                                                    not null comment '도착 예정일',
    forwarding_time              varchar(10) default '14'                                                               not null,
    need_taxation                tinyint(1)  default 0                                                                  not null comment '과세 여부',
    point_rate                   float       default 0.001                                                              not null comment '적립금 지급',
    represent_item_id            int                                                                                    null comment '대표 옵션 아이템 아이디',
    deliver_box_per_amount       int                                                                                    null,
    created_at                   datetime    default current_timestamp()                                                not null comment '생성한 일시',
    promotion_start_at           datetime                                                                               null comment '프로모션 시작 일시',
    promotion_end_at             datetime                                                                               null comment '프로모션 종료 일시',
    item_code                    varchar(10) default '19'                                                               not null,
    recommended_cooking_way      varchar(10)                                                                            null,
    the_scent_of_the_sea         float                                                                                  null,
    difficulty_level_of_trimming float                                                                                  null,
    constraint FK_product_category_id_category_id
        foreign key (category_id) references category (id),
    constraint FK_product_store_id_store_id
        foreign key (store_id) references store (id)
)
    comment '상품' charset = utf8mb4;

create table if not exists agricultural_and_livestock_products
(
    id                           int auto_increment comment '상품 정보 고시 아이디'
        primary key,
    product_id                   int          not null comment '상품 아이디',
    name_of_product              varchar(100) not null comment '품목 또는 명칭',
    volume                       varchar(100) not null comment '포장단위별 내용물의 용량(중량), 수량, 크기',
    producer                     varchar(100) not null comment '생산자,수입품의 경우 수입자를 함께 표기',
    origin_country               varchar(100) not null comment '농수산물의 원산지 표시 등에 관한 법률에 따른 원산지',
    quality_maintenance_deadline varchar(100) not null comment '제조연월일, 소비기한 또는 품질유지기한',
    genetically_modified_info    varchar(100) not null comment '농수산물-농수산물 품질관리법에 따른 유전자변형농산물 표시, 지리적 표시',
    product_grade                varchar(100) not null comment '축산물 – 축산법에 따른 등급 표시, 가축 및 축산물 이력관리에 관한 법률에 따른 이력관리대상축산물 유무',
    import_information           varchar(100) not null comment '수입 농수축산물 - “수입식품안전관리 특별법에 따른 수입신고를 필함”의 문구',
    contents_of_product          varchar(100) not null comment '상품구성',
    how_to_keep                  varchar(100) not null comment '보관방법 또는 취급방법',
    phone_number                 varchar(20)  not null comment '소비자상담관련 전화번호',
    caution_guidelines           varchar(100) not null comment '소비자안전을 위한 주의사항',
    constraint agricultural_and_livestock_products_idfk1
        foreign key (product_id) references product (id)
)
    comment '상품정보제공고시-(19)농수축산물' charset = utf8mb3;

create table if not exists curation_product_map
(
    id          int auto_increment comment '아이디'
        primary key,
    curation_id int not null comment '큐레이션 아이디',
    product_id  int not null comment '상품 아이디',
    constraint FK_curation_product_map_curation_id_curation_id
        foreign key (curation_id) references curation (id),
    constraint FK_curation_product_map_product_id_product_id
        foreign key (product_id) references product (id)
)
    comment '큐레이션 상품 매핑' charset = utf8mb3;

create table if not exists difficult_deliver_address
(
    id         int auto_increment comment '아이디'
        primary key,
    product_id int         not null comment '상품 아이디',
    bcode      varchar(10) not null comment '법정동코드',
    constraint FK_difficult_deliver_address_product_id_product_id
        foreign key (product_id) references product (id)
)
    comment '배송 불가 지역' charset = utf8mb4;

create table if not exists options
(
    id          int auto_increment comment '아이디'
        primary key,
    product_id  int                                         not null comment '상품 아이디',
    state       enum ('ACTIVE', 'DELETED') default 'ACTIVE' not null comment '상태',
    is_needed   tinyint(1)                                  not null comment '필수 여부',
    description varchar(200)                                not null comment '설명',
    constraint FK_options_product_id_product_id
        foreign key (product_id) references product (id)
)
    comment '옵션' charset = utf8mb3;

create table if not exists option_item
(
    id                     int auto_increment comment '아이디'
        primary key,
    option_id              int                                         not null comment '옵션 아이디',
    name                   varchar(100)                                not null comment '이름',
    state                  enum ('ACTIVE', 'DELETED') default 'ACTIVE' not null comment '상태',
    discount_price         int                                         not null comment '할인 가격',
    season_discount_price  int                                         null,
    amount                 int                                         null comment '잔여 개수',
    purchase_price         int                                         not null comment '매입가',
    origin_price           int                                         not null comment '정가',
    delivery_fee           int                                         not null comment '배송비',
    deliver_box_per_amount int                                         null comment '택배 책정 수량',
    max_available_amount   int                                         null comment '최대 주문 수량',
    constraint FK_option_item_option_id_options_id
        foreign key (option_id) references options (id)
)
    comment '옵션 아이템' charset = utf8mb3;

create table if not exists processed_food
(
    id                           int auto_increment comment '상품 정보 고시 아이디'
        primary key,
    product_id                   int          not null comment '상품 아이디',
    name_of_product              varchar(100) not null comment '제품명',
    types_of_food                varchar(100) not null comment '식품의 유형',
    producer                     varchar(100) not null comment '생산자 및 소재지 (수입품의 경우 생산자, 수입자 및 제조국)',
    quality_maintenance_deadline varchar(100) not null comment '제조연월일, 소비기한 또는 품질유지기한',
    volume                       varchar(100) not null comment '포장단위별 내용물의 용량(중량), 수량',
    raw_material_info            varchar(100) not null comment '원재료명 (농수산물의 원산지 표시 등에 관한 법률에 따른 원산지 표시 포함) 및 함량',
    nutritional_ingredients      varchar(100) not null comment '영양성분(영양성분 표시대상 식품에 한함)',
    genetically_modified_info    varchar(100) not null comment '유전자변형식품에 해당하는 경우의 표시',
    imported_phrase              varchar(100) not null comment '수입식품의 경우 “수입식품안전관리 특별법에 따른 수입신고를 필함”의 문구',
    phone_number                 varchar(20)  not null comment '소비자상담관련 전화번호',
    caution_guidelines           varchar(100) not null comment '소비자안전을 위한 주의사항',
    constraint processed_food_idfk1
        foreign key (product_id) references product (id)
)
    comment '상품정보제공고시-(20)가공식품' charset = utf8mb3;

create table if not exists product_filter_value
(
    compare_filter_id int         not null comment '비교하기 필터 아이디',
    product_id        int         not null comment '상품 아이디',
    value             varchar(50) not null comment '값',
    primary key (compare_filter_id, product_id),
    constraint FK_product_filter_value_compare_filter_id_compare_filter_id
        foreign key (compare_filter_id) references compare_filter (id),
    constraint FK_product_filter_value_product_id_product_id
        foreign key (product_id) references product (id)
)
    comment '비교하기 상품 필터 값' charset = utf8mb4;

create table if not exists product_search_filter_map
(
    field_id   int not null comment '필드 아이디',
    product_id int not null comment '상품 아이디',
    primary key (field_id, product_id),
    constraint FK_product_search_filter_map_field_id_search_filter_field_id
        foreign key (field_id) references search_filter_field (id),
    constraint FK_product_search_filter_map_product_id_product_id
        foreign key (product_id) references product (id)
)
    comment '상품, 검색 필터 매핑' charset = utf8mb4;

create table if not exists settlement
(
    id                int auto_increment comment '아이디'
        primary key,
    store_id          int                       not null comment '스토어 아이디',
    state             enum ('DONE', 'CANCELED') not null comment '상태',
    settlement_amount int                       not null comment '정산 금액',
    settled_at        datetime                  not null comment '정산 일시',
    cancel_reason     varchar(500)              null comment '취소 사유',
    constraint FK_settlement_store_id_store_id
        foreign key (store_id) references store (id)
)
    comment '정산' charset = utf8mb4;

create table if not exists store_info
(
    store_id                int                  not null comment '상점 아이디'
        primary key,
    background_image        text                 not null comment '배경 이미지',
    profile_image           text                 not null comment '프로필 이미지',
    is_reliable             tinyint(1) default 0 not null comment '믿고 구매가능 여부',
    name                    varchar(50)          not null comment '이름',
    location                varchar(50)          not null comment '위치',
    keyword                 text                 not null comment '키워드',
    visit_note              text                 null comment '방문 일지',
    is_conditional          tinyint(1) default 0 not null,
    min_store_price         int                  null,
    delivery_fee            int                  null,
    min_order_price         int                  null,
    refund_deliver_fee      int                  null comment '반품 배송비',
    one_line_description    varchar(500)         not null comment '한 줄 소개',
    settlement_rate         float                null comment '정산 비율',
    bank_name               varchar(20)          null comment '은행명',
    bank_holder             varchar(50)          null comment '예금주',
    bank_account            varchar(50)          null comment '계좌번호',
    representative_name     varchar(20)          null comment '대표자이름',
    company_id              varchar(20)          null comment '사업자 번호',
    business_type           varchar(50)          null comment '업태/종목',
    mos_registration_number varchar(50)          null comment '통신판매신고번호',
    business_address        varchar(200)         null comment '사업장 주소',
    postal_code             varchar(5)           null comment '우편번호',
    lot_number_address      varchar(200)         null comment '지번',
    street_name_address     varchar(200)         null comment '도로명',
    address_detail          varchar(200)         null comment '상세 주소',
    tel                     varchar(20)          null comment '전화번호',
    email                   varchar(300)         null comment '이메일',
    fax_number              varchar(20)          null comment '팩스 번호',
    mos_registration        text                 null comment '통신판매신고증',
    business_registration   text                 null comment '사업자등록사본',
    bank_account_copy       text                 null comment '계좌사본',
    deliver_company         varchar(20)          null comment '택배사명',
    constraint FK_store_info_store_id_store_id
        foreign key (store_id) references store (id)
)
    comment '상점 정보' charset = utf8mb4;

create table if not exists tip
(
    id           int auto_increment comment '아이디'
        primary key,
    type         enum ('COMPARE', 'BUY_TIP', 'NEW_ONE') default 'COMPARE'           not null comment '타입',
    state        enum ('ACTIVE', 'INACTIVE')            default 'ACTIVE'            not null comment '상태',
    title        varchar(100)                                                       not null comment '제목',
    description  varchar(200)                                                       not null comment '설명',
    image        text                                                               not null comment '이미지',
    image_detail text                                                               not null comment '상세 이미지',
    content      text                                                               not null comment '상세 내용',
    created_at   datetime                               default current_timestamp() not null comment '생성 일시'
)
    comment '알아두면 좋은 정보' charset = utf8mb3;

create table if not exists top_bar
(
    id   int auto_increment comment '아이디'
        primary key,
    name varchar(20) not null comment '이름'
)
    comment '메인 탑바 카테고리' charset = utf8mb3;

create table if not exists top_bar_product_map
(
    id         int auto_increment comment '아이디'
        primary key,
    top_bar_id int not null comment '탑바 아이디',
    product_id int not null comment '상품 아이디',
    constraint FK_top_bar_product_map_product_id_product_id
        foreign key (product_id) references product (id),
    constraint FK_top_bar_product_map_top_bar_id_top_bar_id
        foreign key (top_bar_id) references top_bar (id)
)
    comment '탑바 상품 매핑' charset = utf8mb3;

create table if not exists user
(
    id          int auto_increment comment '아이디'
        primary key,
    state       enum ('ACTIVE', 'BANNED', 'DELETED') not null comment '상태',
    join_at     datetime                             not null comment '가입 일시',
    withdraw_at datetime                             null comment '탈퇴 일시'
)
    comment '유저' charset = utf8mb3;

create table if not exists basket_product_info
(
    id             int auto_increment comment '아이디'
        primary key,
    user_id        int        not null comment '유저 아이디',
    store_id       int        not null,
    product_id     int        not null comment '상품 아이디',
    option_id      int        not null,
    is_needed      tinyint(1) not null,
    option_item_id int        not null,
    amount         int        not null comment '갯수',
    delivery_fee   int        not null comment '배송비',
    constraint FK_basket_product_info3
        foreign key (option_id) references options (id)
            on update cascade on delete cascade,
    constraint FK_basket_product_info4
        foreign key (option_item_id) references option_item (id)
            on update cascade on delete cascade,
    constraint FK_basket_product_info5
        foreign key (store_id) references store (id)
            on update cascade on delete cascade,
    constraint FK_basket_product_info_product_id_product_id
        foreign key (product_id) references product (id),
    constraint FK_basket_product_info_user_id_user_id
        foreign key (user_id) references user (id)
)
    comment '장바구니 상품 정보' charset = utf8mb3;

create table if not exists basket_product_option
(
    id               int auto_increment comment '아이디'
        primary key,
    order_product_id int not null comment '주문 상품 아이디',
    option_id        int not null comment '옵션 아이디',
    constraint FK_basket_product_option_option_id_option_item_id
        foreign key (option_id) references option_item (id),
    constraint FK_basket_product_option_order_product_id_basket_product_info_id
        foreign key (order_product_id) references basket_product_info (id)
)
    comment '주문 상품 옵션' charset = utf8mb3;

create table if not exists basket_tasting_note
(
    id         int auto_increment comment '테이스팅 노트 저장함 아이디'
        primary key,
    product_id int not null comment '상품 아이디',
    user_id    int not null comment '유저 아이디',
    constraint basket_tasting_note_ibfk_1
        foreign key (product_id) references product (id),
    constraint basket_tasting_note_ibfk_2
        foreign key (user_id) references user (id)
)
    comment '테이스팅노트 비교하기 저장함' charset = utf8mb4;

create table if not exists compare_set
(
    id         int auto_increment comment '아이디'
        primary key,
    user_id    int      not null comment '유저 아이디',
    created_at datetime not null comment '생성 일시',
    constraint FK_compare_set_user_id_user_id
        foreign key (user_id) references user (id)
)
    comment '비교하기 셋트' charset = utf8mb3;

create table if not exists compare_item
(
    compare_set_id int not null comment '비교하기 세트 아이디',
    product_id     int not null comment '상품 아이디',
    primary key (compare_set_id, product_id),
    constraint FK_compare_item_compare_set_id_compare_set_id
        foreign key (compare_set_id) references compare_set (id),
    constraint FK_compare_item_product_id_product_id
        foreign key (product_id) references product (id)
)
    comment '비교하기 아이템' charset = utf8mb3;

create table if not exists coupon_user_map
(
    user_id   int     not null comment '유저 아이디',
    coupon_id int     not null comment '쿠폰 아이디',
    is_used   tinyint not null comment '사용 여부',
    primary key (user_id, coupon_id),
    constraint FK_coupon_user_map_coupon_id_coupon_id
        foreign key (coupon_id) references coupon (id),
    constraint FK_coupon_user_map_user_id_user_id
        foreign key (user_id) references user (id)
)
    comment '쿠폰 보유 현황';

create table if not exists inquiry
(
    id          int auto_increment comment '아이디'
        primary key,
    type        enum ('PRODUCT', 'DELIVERY', 'CANCEL', 'ETC') not null comment '타입',
    is_secret   tinyint(1)                                    not null comment '비밀글 여부',
    product_id  int                                           not null comment '상품 아이디',
    user_id     int                                           not null comment '유저 아이디',
    content     text                                          not null comment '내용',
    created_at  datetime default current_timestamp()          not null comment '생성일시',
    answered_at datetime                                      null comment '답변 일시',
    answer      text                                          null comment '답변',
    constraint FK_inquiry_product_id_product_id
        foreign key (product_id) references product (id),
    constraint FK_inquiry_user_id_user_id
        foreign key (user_id) references user (id)
)
    comment '문의' charset = utf8mb3;

create table if not exists notification
(
    id         int auto_increment comment '아이디'
        primary key,
    user_id    int                                                     not null comment '유저 아이디',
    type       enum ('DELIVERY', 'ADMIN', 'REVIEW', 'COUPON', 'ORDER') not null comment '타입',
    title      varchar(100)                                            not null comment '제목',
    content    varchar(300)                                            not null comment '내용',
    created_at datetime default current_timestamp()                    not null comment '생성 일시',
    constraint FK_notification_user_id_user_id
        foreign key (user_id) references user (id)
)
    comment '알림' charset = utf8mb3;

create table if not exists orders
(
    id                 varchar(20)                                                                                                                  not null comment '아이디'
        primary key,
    user_id            int                                                                                                                          not null comment '유저 아이디',
    orderer_name       varchar(20)                                                                                                                  not null comment '오더자 이름',
    orderer_tel        varchar(11)                                                                                                                  not null comment '오더자 연락처',
    payment_way        enum ('CARD', 'KEY_IN', 'NAVER', 'KAKAO_PAY', 'PHONE', 'DEPOSIT', 'VIRTUAL_ACCOUNT', 'TOSS_PAY') default 'CARD'              not null comment '결제 방식',
    state              varchar(30)                                                                                                                  null comment '상태',
    total_price        int                                                                                                                          not null comment '최종 주문가격 (쿠폰, 포인트, 배송비 반영)',
    origin_total_price int                                                                                                                          null comment '총 상품 가격 (택배비 x)',
    ordered_at         datetime                                                                                         default current_timestamp() not null comment '주문 일시',
    imp_uid            varchar(30)                                                                                                                  null comment '포트원 고유 결제 아이디',
    coupon_id          int                                                                                                                          null comment '사용 쿠폰 아이디',
    coupon_discount    int                                                                                                                          null comment '쿠폰 할인 금액',
    use_point          int                                                                                                                          null comment '사용 적립금',
    bank_holder        varchar(20)                                                                                                                  null comment '환불_예금주명',
    bank_code          varchar(10)                                                                                                                  null comment '환불_입금 은행 코드',
    bank_name          varchar(10)                                                                                                                  null comment '환불_입금 은행명',
    bank_account       varchar(30)                                                                                                                  null comment '환불_계좌번호',
    constraint FK_orders_user_id_user_id
        foreign key (user_id) references user (id)
)
    comment '주문' charset = utf8mb3;

create table if not exists order_deliver_place
(
    order_id        varchar(20)  not null comment '주문 아이디'
        primary key,
    name            varchar(50)  not null comment '배송지명',
    receiver_name   varchar(20)  not null comment '수령인',
    tel             varchar(11)  not null comment '연락처',
    postal_code     varchar(20)  not null comment '우편번호',
    address         varchar(100) not null comment '주소',
    address_detail  varchar(100) not null comment '상세 주소',
    bcode           varchar(10)  null comment '법정동 코드',
    deliver_message varchar(100) not null comment '배송 메시지',
    constraint FK_order_deliver_place_order_id_orders_id
        foreign key (order_id) references orders (id)
)
    charset = utf8mb3;

create table if not exists order_product_info
(
    id                    int auto_increment comment '아이디'
        primary key,
    order_id              varchar(20)          not null comment '주문 아이디',
    product_id            int                  not null comment '상품 아이디',
    store_id              int                  null comment '스토어 아이디',
    option_item_id        int                  not null comment '상품 옵셩 아이템 아이디',
    state                 varchar(30)          not null comment '상태',
    settle_price          int                  null,
    origin_price          int                  null comment '상품 가격',
    price                 int                  not null comment '상품 가격 * 수량',
    amount                int                  not null comment '갯수',
    delivery_fee          int                  not null comment '배송비',
    delivery_fee_type     varchar(30)          null,
    cancel_reason         varchar(30)          null comment '취소 사유',
    cancel_reason_content text                 null comment '취소 사유 내용',
    deliver_company_code  varchar(5)           null comment '택배사 코드',
    invoice_code          varchar(30)          null comment '운송장 번호',
    is_settled            tinyint(1) default 0 not null comment '정산 여부',
    settled_at            datetime             null,
    delivery_done_at      datetime             null comment '배송 완료 일시',
    final_confirmed_at    datetime             null comment '구매 확정 일시',
    tax_free_amount       int                  null comment '면세 금액',
    is_tax_free           tinyint(1) default 0 not null,
    constraint FK_order_product_info1
        foreign key (store_id) references store (id)
            on update cascade on delete cascade,
    constraint FK_order_product_info_order_id_orders_id
        foreign key (order_id) references orders (id),
    constraint FK_order_product_info_product_id_product_id
        foreign key (product_id) references product (id),
    constraint order_product_info_option_item_id_fk
        foreign key (option_item_id) references option_item (id)
)
    comment '주문 상품 정보' charset = utf8mb3;

create table if not exists order_product_option
(
    id               int auto_increment comment '아이디'
        primary key,
    order_product_id int          not null comment '주문 상품 아이디',
    name             varchar(100) not null comment '이름',
    price            int          not null comment '가격',
    amount           int          not null comment '수량',
    constraint FK_order_product_option_order_product_id_order_product_info_id
        foreign key (order_product_id) references order_product_info (id)
)
    comment '주문 상품 옵션' charset = utf8mb3;

create table if not exists order_product_option_item
(
    id                      int auto_increment comment '아이디'
        primary key,
    name                    varchar(100) not null comment '이틈',
    price                   int          not null comment '가격',
    amount                  int          not null comment '수량',
    order_product_option_id int          not null comment '주문 상품 옵션 아이디',
    constraint order_product_option_item_idkf1
        foreign key (order_product_option_id) references order_product_option (id)
)
    comment '주문 상품 옵션 아이템' charset = utf8mb3;

create table if not exists payment_method
(
    id                 int auto_increment comment '아이디'
        primary key,
    user_id            int          not null comment '유저 아이디',
    name               varchar(20)  not null comment '이름',
    card_name          varchar(50)  not null comment '카드 이름',
    card_no            varchar(300) not null comment '카드번호',
    expiry_at          varchar(5)   not null comment '유효기간',
    birth              varchar(6)   not null comment '생년월일',
    password_two_digit varchar(100) not null comment '비밀번호 두자리',
    customer_uid       varchar(100) not null comment '포트원 고유 아이디',
    constraint FK_payment_method_user_id_user_id
        foreign key (user_id) references user (id)
)
    comment '결제 수단' charset = utf8mb4;

create table if not exists payments
(
    id              int auto_increment comment '아이디'
        primary key,
    order_id        varchar(20)                                  null comment '주문 아이디',
    imp_uid         varchar(30)                                  not null comment '포트원 고유 결제 번호',
    merchant_uid    varchar(20)                                  not null comment '주문번호',
    pay_method      varchar(10)                                  not null comment '결제수단 구분 코드',
    paid_amount     int                                          not null comment '결제 금액',
    status          enum ('READY', 'PAID', 'FAILED', 'CANCELED') not null comment '결제 상태',
    name            varchar(255)                                 null comment '주문 이름',
    pg_provider     varchar(50)                                  null comment 'pg사 구분코드',
    emb_pg_provider varchar(50)                                  null comment '간편결제 구분코드',
    pg_tid          varchar(50)                                  null comment 'pg사에서 거래당 고유하게 부여하는 거래번호',
    buyer_name      varchar(20)                                  null comment '주문자명',
    buyer_email     varchar(300)                                 null comment '주문자 이메일',
    buyer_tel       varchar(20)                                  null comment '주문자 휴대폰번호',
    buyer_address   varchar(100)                                 null comment '주문자 우편번호',
    paid_at         timestamp default current_timestamp()        null on update current_timestamp() comment '결제 승인 시각',
    receipt_url     varchar(255)                                 null comment '거래 매출전표 url',
    apply_num       varchar(30)                                  null comment '신용카드 승인번호',
    vbank_num       varchar(30)                                  null comment '가상계좌 입금 계좌번호',
    vbank_name      varchar(10)                                  null comment '가상계좌 입금은행 명',
    vbank_holder    varchar(10)                                  null comment '가상계좌 예금주',
    vbank_code      varchar(10)                                  null comment '가상계좌 은행코드',
    vbank_date      timestamp                                    null comment '가상계좌 입금기한',
    constraint FK_payments_order_id_order_id
        foreign key (order_id) references orders (id)
)
    comment '결제' charset = utf8mb3;

create table if not exists product_like
(
    product_id int not null comment '상품 아이디',
    user_id    int not null comment '유저 아이디',
    primary key (product_id, user_id),
    constraint FK_product_like_product_id_product_id
        foreign key (product_id) references product (id),
    constraint FK_product_like_user_id_user_id
        foreign key (user_id) references user (id)
)
    comment '상품 좋아요' charset = utf8mb3;

create table if not exists review
(
    id                    int auto_increment comment '아이디'
        primary key,
    product_id            int                                    not null comment '상품 아이디',
    store_id              int                                    not null comment '상점 아이디',
    user_id               int                                    not null comment '유저 아이디',
    order_product_info_id int                                    not null,
    images                mediumtext                             not null comment '이미지',
    content               mediumtext                             not null comment '내용',
    is_deleted            tinyint(1) default 0                   not null,
    created_at            datetime   default current_timestamp() not null comment '생성한 일시',
    constraint FK_review_product_id_product_id
        foreign key (product_id) references product (id),
    constraint FK_review_store_id_store_id
        foreign key (store_id) references store (id),
    constraint FK_review_user_id_user_id
        foreign key (user_id) references user (id),
    constraint review_order_product_info_id_fk
        foreign key (order_product_info_id) references order_product_info (id)
)
    comment '리뷰' collate = utf8mb4_unicode_ci;

create table if not exists report
(
    id         int auto_increment comment '아이디'
        primary key,
    user_id    int          not null comment '유저 아이디',
    review_id  int          not null comment '리뷰 아이디',
    content    varchar(300) not null comment '내용',
    created_at datetime     not null comment '생성 일시',
    confirm_at datetime     null comment '확인 일시',
    constraint FK_report_review_id_review_id
        foreign key (review_id) references review (id),
    constraint FK_report_user_id_user_id
        foreign key (user_id) references user (id)
)
    comment '신고' charset = utf8mb4;

create table if not exists review_evaluation
(
    review_id  int                                                   not null comment '리뷰 아이디',
    evaluation enum ('TASTE', 'FRESH', 'PRICE', 'PACKAGING', 'SIZE') not null comment '평가 타입',
    primary key (review_id, evaluation),
    constraint FK_review_evaluation_review_id_review_id
        foreign key (review_id) references review (id)
)
    comment '리뷰 평가' charset = utf8mb4;

create table if not exists review_like
(
    review_id int not null comment '리뷰 아이디',
    user_id   int not null comment '유저 아이디',
    primary key (review_id, user_id),
    constraint FK_review_like_review_id_review_id
        foreign key (review_id) references review (id),
    constraint FK_review_like_user_id_user_id
        foreign key (user_id) references user (id)
)
    comment '리뷰 좋아요' charset = utf8mb3;

create table if not exists save_product
(
    user_id    int not null comment '유저 아이디',
    product_id int not null comment '상품 아이디',
    primary key (user_id, product_id),
    constraint FK_save_product_product_id_product_id
        foreign key (product_id) references product (id),
    constraint FK_save_product_user_id_user_id
        foreign key (user_id) references user (id)
)
    comment '저장한 상품' charset = utf8mb3;

create table if not exists store_scrap
(
    store_id int not null comment '상점아이디',
    user_id  int not null comment '유저 아이디',
    primary key (store_id, user_id),
    constraint FK_store_scrap_store_id_store_id
        foreign key (store_id) references store (id),
    constraint FK_store_scrap_user_id_user_id
        foreign key (user_id) references user (id)
)
    comment '상점 스크랩' charset = utf8mb3;

create table if not exists tasting_note
(
    id                    int auto_increment comment '테이스팅 노트 아이디'
        primary key,
    order_product_info_id int   not null comment '주문상품번호',
    product_id            int   not null comment '상품 아이디',
    user_id               int   not null comment '유저 아이디',
    taste_1               float not null comment '기름짐',
    taste_2               float not null,
    taste_3               float not null,
    taste_4               float not null,
    taste_5               float not null,
    texture_1             float null,
    texture_2             float null,
    texture_3             float null,
    texture_4             float null,
    texture_5             float null,
    texture_6             float null comment '값',
    texture_7             float null,
    texture_8             float null,
    texture_9             float null,
    texture_10            float null,
    constraint tasting_note_ibfk_1
        foreign key (order_product_info_id) references order_product_info (id),
    constraint tasting_note_ibfk_2
        foreign key (product_id) references product (id),
    constraint tasting_note_ibfk_3
        foreign key (user_id) references user (id)
)
    comment '테이스팅노트' charset = utf8mb4;

create table if not exists user_auth
(
    login_Type enum ('IDPW', 'GOOGLE', 'NAVER', 'KAKAO', 'APPLE') not null comment '유저 타입',
    login_id   varchar(150)                                       not null comment '아이디',
    user_id    int                                                not null comment '유저 아이디',
    password   varchar(60)                                        null comment '비밀번호',
    created_at datetime                                           null,
    updated_at datetime                                           null,
    primary key (login_Type, login_id),
    constraint FK_user_auth_user_id_user_id
        foreign key (user_id) references user (id)
)
    comment '사용자 인증 테이블' charset = utf8mb3;

create table if not exists user_info
(
    user_id            int                         not null comment '유저 아이디'
        primary key,
    profile_image      text charset utf8mb3        not null comment '프로필 이미지',
    email              varchar(300)                null comment '이메일 (not null로 하면 카카오, 네이버 회원기입 안될 수도 있음',
    name               varchar(20) charset utf8mb3 not null comment '이름',
    nickname           varchar(50) charset utf8mb3 not null comment '닉네임',
    phone              varchar(11) charset utf8mb3 null comment '휴대폰 번호',
    is_agree_marketing tinyint(1)                  not null comment '마케팅 수신 동의',
    grade_id           int default 1               not null comment '등급 아이디',
    point              int default 0               not null comment '적립금',
    constraint user_info_pk
        unique (phone),
    constraint FK_user_info_user_id_user_id
        foreign key (user_id) references user (id),
    constraint user_info_grade_id_fk
        foreign key (grade_id) references grade (id)
)
    comment '유저 정보' charset = utf8mb4;

create table if not exists deliver_place
(
    id              int auto_increment comment '아이디'
        primary key,
    user_id         int                  not null comment '유저 아이디',
    name            varchar(50)          not null comment '배송지명',
    receiver_name   varchar(20)          not null comment '수령인',
    tel             varchar(11)          not null comment '연락처',
    postal_code     varchar(20)          not null comment '우편번호',
    address         varchar(100)         not null comment '주소',
    address_detail  varchar(100)         not null comment '상세 주소',
    bcode           varchar(10)          not null comment '법정동 코드',
    deliver_message varchar(100)         not null comment '배송 메시지',
    is_default      tinyint(1) default 0 not null comment '기본 배송지 여부',
    constraint FK_deliver_place_user_id_user_info_user_id
        foreign key (user_id) references user_info (user_id)
)
    comment '배송지 목록' charset = utf8mb3;

create table if not exists verification
(
    id                  int auto_increment comment '아이디'
        primary key,
    target              varchar(300) not null comment '타겟',
    verification_number char(6)      not null comment '인증번호',
    expired_at          datetime     null comment '만료 일자',
    create_at           datetime     not null comment '생성 일시'
)
    comment '인증' charset = utf8mb3;

create table if not exists weeks_date
(
    date                        varchar(8)  not null comment '날짜'
        primary key,
    is_delivery_company_holiday tinyint(1)  not null comment '택배사가 쉬는지 여부',
    description                 varchar(20) not null comment '날짜 설명 (추석, 대체공휴일 등)'
)
    comment '택배사가 쉬는지 여부를 판단하기 위한 테이블' charset = utf8mb4;

