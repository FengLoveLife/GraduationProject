create table calendar_factor
(
    id           bigint auto_increment comment '主键ID'
        primary key,
    date         date                               not null comment '日期(唯一)',
    day_of_week  tinyint                            not null comment '星期几 1-7(1=周一)',
    is_weekend   tinyint  default 0                 not null comment '是否周末: 0否 1是',
    is_holiday   tinyint  default 0                 not null comment '是否节假日: 0否 1是',
    holiday_name varchar(50)                        null comment '节假日名称: 春节等',
    weather      varchar(20)                        null comment '天气: 晴/多云/雨/雪',
    create_time  datetime default CURRENT_TIMESTAMP not null,
    update_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_date
        unique (date)
)
    comment '日历影响因子表';

create table forecast_result
(
    id                 bigint auto_increment comment '主键ID'
        primary key,
    forecast_date      date                               not null comment '预测的目标日期',
    product_id         bigint                             not null comment '商品ID',
    product_code       varchar(50)                        not null comment '商品编码',
    product_name       varchar(100)                       not null comment '商品名称',
    category_id        bigint                             null comment '分类ID',
    category_name      varchar(50)                        null comment '分类名称',
    predicted_quantity int                                not null comment '预测销量',
    actual_quantity    int                                null comment '实际销量(预测日期过后填入)',
    error_rate         decimal(5, 2)                      null comment '预测误差率(%)',
    create_time        datetime default CURRENT_TIMESTAMP not null,
    update_time        datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_product_date
        unique (product_id, forecast_date)
)
    comment '销量预测结果表';

create index idx_category_id
    on forecast_result (category_id);

create index idx_forecast_date
    on forecast_result (forecast_date);

create table inventory_log
(
    id            bigint auto_increment comment '主键ID'
        primary key,
    log_no        varchar(64)                        not null comment '流水单号(唯一)',
    product_id    bigint                             not null comment '商品ID',
    type          tinyint                            not null comment '变动类型: 1进货入库, 2销售出库, 3损耗盘亏, 4手工调整',
    change_amount int                                not null comment '变动数量(正数加, 负数减)',
    before_stock  int                                not null comment '变动前库存',
    after_stock   int                                not null comment '变动后库存',
    reference_no  varchar(64)                        null comment '关联业务单号(如进货单号、销售单号)',
    operator      varchar(50)                        not null comment '操作人(或系统)',
    remark        varchar(255)                       null comment '备注说明',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_log_no
        unique (log_no)
)
    comment '库存变动流水(台账)表';

create index idx_create_time
    on inventory_log (create_time);

create index idx_product_id
    on inventory_log (product_id);

create table product
(
    id             bigint auto_increment comment '商品ID'
        primary key,
    category_id    bigint                                   not null comment '所属分类ID',
    product_code   varchar(50)                              not null comment '商品系统编码(唯一)',
    name           varchar(100)                             not null comment '商品名称',
    specification  varchar(50)                              null comment '商品规格',
    unit           varchar(20)                              not null comment '计价/基本单位',
    image_url      varchar(255)                             null comment '商品图片URL',
    purchase_price decimal(10, 2) default 0.00              not null comment '进货价/成本价',
    sale_price     decimal(10, 2) default 0.00              not null comment '零售价',
    stock          int            default 0                 not null comment '当前库存量',
    safety_stock   int            default 0                 not null comment '安全库存阈值',
    status         tinyint        default 1                 null comment '状态: 1-上架, 0-下架',
    create_time    datetime       default CURRENT_TIMESTAMP null comment '创建时间',
    update_time    datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_product_code
        unique (product_code) comment '商品编码唯一索引'
)
    comment '商品信息中心表';

create index idx_category_id
    on product (category_id)
    comment '分类ID普通索引，提升按分类查询速度';

create table product_category
(
    id                 bigint auto_increment comment '分类ID'
        primary key,
    name               varchar(50)                        not null comment '分类名称',
    parent_id          bigint   default 0                 null comment '父级分类ID (0表示顶级)',
    level              tinyint  default 1                 null comment '层级',
    sort_order         int      default 0                 null comment '排序权重',
    restock_cycle_days int      default 7                 null comment '补货周期(天)',
    status             tinyint  default 1                 null comment '状态: 1-启用, 0-禁用',
    create_time        datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time        datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '商品分类表';

create table purchase_order
(
    id                  bigint auto_increment comment '主键ID'
        primary key,
    order_no            varchar(32)                              not null comment '进货单号',
    total_quantity      int            default 0                 not null comment '商品总数量',
    total_amount        decimal(12, 2) default 0.00              not null comment '进货总金额',
    order_date          date                                     not null comment '下单日期',
    expected_date       date                                     null comment '预计到货日期',
    actual_arrival_date date                                     null comment '实际到货日期',
    status              tinyint        default 0                 not null comment '状态: 0待确认 1已下单 2已完成 3已取消',
    remark              varchar(500)                             null comment '备注',
    operator            varchar(50)                              null comment '操作人',
    create_time         datetime       default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time         datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_order_no
        unique (order_no)
)
    comment '进货单主表';

create index idx_order_date
    on purchase_order (order_date);

create index idx_status
    on purchase_order (status);

create table purchase_order_item
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    order_id        bigint                             not null comment '进货单ID',
    order_no        varchar(32)                        not null comment '进货单号(冗余)',
    product_id      bigint                             not null comment '商品ID',
    product_code    varchar(50)                        not null comment '商品编码',
    product_name    varchar(100)                       not null comment '商品名称',
    category_name   varchar(50)                        null comment '分类名称',
    purchase_price  decimal(10, 2)                     not null comment '进货单价',
    quantity        int                                not null comment '进货数量',
    subtotal_amount decimal(12, 2)                     not null comment '小计金额',
    suggestion_id   bigint                             null comment '关联的进货建议ID',
    create_time     datetime default CURRENT_TIMESTAMP not null comment '创建时间'
)
    comment '进货单明细表';

create index idx_order_id
    on purchase_order_item (order_id);

create index idx_product_id
    on purchase_order_item (product_id);

create table purchase_suggestion
(
    id                 bigint auto_increment comment '主键ID'
        primary key,
    product_id         bigint                                   not null comment '商品ID',
    category_id        bigint                                   not null comment '分类ID',
    product_code       varchar(50)                              not null comment '商品编码',
    product_name       varchar(100)                             not null comment '商品名称',
    category_name      varchar(50)                              not null comment '分类名称',
    purchase_price     decimal(10, 2)                           null comment '进货单价',
    predicted_quantity int                                      not null comment '预测销量',
    daily_sales        decimal(10, 2) default 0.00              null comment '日均销量(预测值或近7天均值)',
    current_stock      int                                      not null comment '当前库存',
    safety_stock       int                                      not null comment '安全库存',
    target_stock       int                                      not null comment '目标库存',
    light_status       tinyint        default 1                 null comment '灯位: 1红灯(必须补货) 2黄灯(顺带补货)',
    suggested_quantity int                                      not null comment '系统建议进货量',
    adjusted_quantity  int                                      null comment '用户调整后的进货量',
    final_quantity     int                                      not null comment '最终进货量',
    status             tinyint        default 0                 not null comment '状态: 0待处理 1已生成进货单 2已忽略',
    remark             varchar(255)                             null comment '备注',
    create_time        datetime       default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time        datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '进货建议表';

create index idx_category_id
    on purchase_suggestion (category_id);

create index idx_light_status
    on purchase_suggestion (light_status);

create index idx_status
    on purchase_suggestion (status);

create table sales_order
(
    id             bigint auto_increment comment '主键ID'
        primary key,
    order_no       varchar(32)                              not null comment '订单编号',
    total_amount   decimal(12, 2) default 0.00              not null comment '订单总销售金额',
    total_quantity int            default 0                 not null comment '商品总数量',
    payment_type   tinyint        default 1                 not null comment '支付方式：1现金 2微信 3支付宝',
    sale_date      date                                     not null comment '销售日期',
    sale_time      datetime                                 not null comment '销售时间',
    operator       varchar(50)                              null comment '操作员/收银员',
    remark         varchar(255)                             null comment '备注',
    create_time    datetime       default CURRENT_TIMESTAMP not null,
    update_time    datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_order_no
        unique (order_no)
)
    comment '销售订单主表';

create index idx_sale_date
    on sales_order (sale_date);

create table sales_order_item
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    order_id        bigint                               not null comment '关联订单主表ID',
    order_no        varchar(32)                          not null comment '订单编号(冗余)',
    product_id      bigint                               not null comment '商品ID',
    product_code    varchar(50)                          not null comment '商品编码(冗余)',
    product_name    varchar(100)                         not null comment '商品名称(冗余)',
    category_id     bigint                               null comment '分类ID(冗余)',
    category_name   varchar(50)                          null comment '分类名称(冗余)',
    purchase_price  decimal(10, 2)                       not null comment '进货单价/成本价(快照)',
    unit_price      decimal(10, 2)                       not null comment '实际销售单价',
    quantity        int                                  not null comment '销售数量',
    subtotal_amount decimal(12, 2)                       not null comment '小计销售额 (单价*数量)',
    subtotal_profit decimal(12, 2)                       not null comment '小计毛利润 ((售价-成本)*数量)',
    is_promotion    tinyint(1) default 0                 not null comment '是否促销特价: 0否, 1是',
    sale_date       date                                 not null comment '销售日期(冗余,便于AI聚合)',
    create_time     datetime   default CURRENT_TIMESTAMP not null
)
    comment '销售订单明细表';

create index idx_order_id
    on sales_order_item (order_id);

create index idx_product_id
    on sales_order_item (product_id);

create index idx_sale_date
    on sales_order_item (sale_date);

create table sys_operation_log
(
    id             bigint auto_increment comment '主键'
        primary key,
    user_id        bigint                             not null comment '操作用户ID',
    user_name      varchar(50)                        not null comment '操作用户名',
    operation_type varchar(20)                        not null comment '操作类型：LOGIN/PASSWORD/PRODUCT/SALES/INVENTORY',
    operation_desc varchar(200)                       null comment '操作描述',
    ip_address     varchar(50)                        null comment 'IP地址',
    create_time    datetime default CURRENT_TIMESTAMP null comment '操作时间'
)
    comment '操作日志表';

create index idx_create_time
    on sys_operation_log (create_time);

create index idx_type
    on sys_operation_log (operation_type);

create index idx_user_id
    on sys_operation_log (user_id);

create table sys_user
(
    id              bigint auto_increment comment '用户 ID'
        primary key,
    username        varchar(50)                           not null comment '用户名',
    password        varchar(100)                          not null comment '密码 (建议存储加密后的密文)',
    real_name       varchar(20)                           null comment '真实姓名',
    status          varchar(20) default 'ACTIVE'          not null comment '状态：ACTIVE/INACTIVE',
    create_time     datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    last_login_time datetime                              null comment '最后登录时间',
    phone           varchar(20)                           null comment '联系电话',
    update_time     datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint username
        unique (username)
)
    comment '用户管理表';

