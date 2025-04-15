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