create table if not exists `barofish-refactor`.product_filter_counts
(
    category1   varchar(50)  not null,
    category2   varchar(50)  not null,
    filter_key  varchar(255) not null,
    product_cnt int          not null,
    primary key (category1, category2, filter_key)
);

create table if not exists `barofish-refactor`.product_filter_value
(
    compare_filter_id int         not null comment '비교하기 필터 아이디',
    product_id        int         not null comment '상품 아이디',
    value             varchar(50) not null comment '값',
    primary key (compare_filter_id, product_id),
    constraint FK_product_filter_value_compare_filter_id_compare_filter_id
        foreign key (compare_filter_id) references `barofish-refactor`.compare_filter (id),
    constraint FK_product_filter_value_product_id_product_id
        foreign key (product_id) references `barofish-refactor`.product (id)
)
    comment '비교하기 상품 필터 값';
