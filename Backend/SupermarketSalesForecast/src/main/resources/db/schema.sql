-- 创建数据库
CREATE DATABASE IF NOT EXISTS supermarket_forecasting_db CHARACTER SET utf8mb4;
USE supermarket_forecasting_db;
-- 创建用户表
CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
                       username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                       password VARCHAR(100) NOT NULL COMMENT '密码 (建议存储加密后的密文)',
                       real_name VARCHAR(20) DEFAULT NULL COMMENT '真实姓名',
                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
                       create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                       PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户管理表';

 -- 商品分类表
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



-- ----------------------------
-- 创建商品信息表
-- ----------------------------
CREATE TABLE `product` (
                           `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
                           `category_id` BIGINT NOT NULL COMMENT '所属分类ID',
                           `product_code` VARCHAR(50) NOT NULL COMMENT '商品系统编码(唯一)',
                           `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
                           `specification` VARCHAR(50) DEFAULT NULL COMMENT '商品规格',
                           `unit` VARCHAR(20) NOT NULL COMMENT '计价/基本单位',
                           `image_url` VARCHAR(255) DEFAULT NULL COMMENT '商品图片URL',
                           `purchase_price` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '进货价/成本价',
                           `sale_price` DECIMAL(10,2) NOT NULL DEFAULT '0.00' COMMENT '零售价',
                           `stock` INT NOT NULL DEFAULT '0' COMMENT '当前库存量',
                           `safety_stock` INT NOT NULL DEFAULT '0' COMMENT '安全库存阈值',
                           `status` TINYINT DEFAULT '1' COMMENT '状态: 1-上架, 0-下架',
                           `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `uk_product_code` (`product_code`) COMMENT '商品编码唯一索引',
                           KEY `idx_category_id` (`category_id`) COMMENT '分类ID普通索引，提升按分类查询速度'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息中心表';

--- -库存变动流水(台账)表
CREATE TABLE `inventory_log` (
                                 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                 `log_no` varchar(64) NOT NULL COMMENT '流水单号(唯一)',
                                 `product_id` bigint(20) NOT NULL COMMENT '商品ID',
                                 `type` tinyint(4) NOT NULL COMMENT '变动类型: 1进货入库, 2销售出库, 3损耗盘亏, 4手工调整',
                                 `change_amount` int(11) NOT NULL COMMENT '变动数量(正数加, 负数减)',
                                 `before_stock` int(11) NOT NULL COMMENT '变动前库存',
                                 `after_stock` int(11) NOT NULL COMMENT '变动后库存',
                                 `reference_no` varchar(64) DEFAULT NULL COMMENT '关联业务单号(如进货单号、销售单号)',
                                 `operator` varchar(50) NOT NULL COMMENT '操作人(或系统)',
                                 `remark` varchar(255) DEFAULT NULL COMMENT '备注说明',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_log_no` (`log_no`),
                                 KEY `idx_product_id` (`product_id`),
                                 KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动流水(台账)表';


-- 销售订单主表
CREATE TABLE `sales_order` (
                               `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                               `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
                               `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单总销售金额',
                               `total_quantity` INT NOT NULL DEFAULT 0 COMMENT '商品总数量',
                               `payment_type` TINYINT NOT NULL DEFAULT 1 COMMENT '支付方式：1现金 2微信 3支付宝',
                               `sale_date` DATE NOT NULL COMMENT '销售日期',
                               `sale_time` DATETIME NOT NULL COMMENT '销售时间',
                               `operator` VARCHAR(50) DEFAULT NULL COMMENT '操作员/收银员',
                               `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
                               `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_order_no` (`order_no`),
                               KEY `idx_sale_date` (`sale_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单主表';

-- 销售订单明细表
CREATE TABLE `sales_order_item` (
                                    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                    `order_id` BIGINT NOT NULL COMMENT '关联订单主表ID',
                                    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号(冗余)',
                                    `product_id` BIGINT NOT NULL COMMENT '商品ID',
                                    `product_code` VARCHAR(50) NOT NULL COMMENT '商品编码(冗余)',
                                    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称(冗余)',
                                    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID(冗余)',
                                    `category_name` VARCHAR(50) DEFAULT NULL COMMENT '分类名称(冗余)',

                                    `purchase_price` DECIMAL(10,2) NOT NULL COMMENT '进货单价/成本价(快照)',
                                    `unit_price` DECIMAL(10,2) NOT NULL COMMENT '实际销售单价',
                                    `quantity` INT NOT NULL COMMENT '销售数量',
                                    `subtotal_amount` DECIMAL(12,2) NOT NULL COMMENT '小计销售额 (单价*数量)',
                                    `subtotal_profit` DECIMAL(12,2) NOT NULL COMMENT '小计毛利润 ((售价-成本)*数量)',

                                    `is_promotion` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否促销特价: 0否, 1是',
                                    `sale_date` DATE NOT NULL COMMENT '销售日期(冗余,便于AI聚合)',
                                    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    PRIMARY KEY (`id`),
                                    KEY `idx_order_id` (`order_id`),
                                    KEY `idx_product_id` (`product_id`),
                                    KEY `idx_sale_date` (`sale_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单明细表';

-- 日历影响因子表
CREATE TABLE `calendar_factor` (
                                   `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                   `date` DATE NOT NULL COMMENT '日期(唯一)',
                                   `day_of_week` TINYINT NOT NULL COMMENT '星期几 1-7(1=周一)',
                                   `is_weekend` TINYINT NOT NULL DEFAULT 0 COMMENT '是否周末: 0否 1是',
                                   `is_holiday` TINYINT NOT NULL DEFAULT 0 COMMENT '是否节假日: 0否 1是',
                                   `holiday_name` VARCHAR(50) DEFAULT NULL COMMENT '节假日名称: 春节等',
                                   `weather` VARCHAR(20) DEFAULT NULL COMMENT '天气: 晴/多云/雨/雪',
                                   `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日历影响因子表';


-- 极简版：销量预测结果表
CREATE TABLE `forecast_result` (
                                   `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    -- 预测目标
                                   `forecast_date` DATE NOT NULL COMMENT '预测的目标日期',
                                   `product_id` BIGINT NOT NULL COMMENT '商品ID',

    -- 冗余字段（仅用于前端展示，避免JOIN）
                                   `product_code` VARCHAR(50) NOT NULL COMMENT '商品编码',
                                   `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
                                   `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
                                   `category_name` VARCHAR(50) DEFAULT NULL COMMENT '分类名称',

    -- 核心数据
                                   `predicted_quantity` INT NOT NULL COMMENT '预测销量',
                                   `actual_quantity` INT DEFAULT NULL COMMENT '实际销量(预测日期过后填入)',
                                   `error_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '预测误差率(%)',

                                   `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_product_date` (`product_id`, `forecast_date`),
                                   KEY `idx_forecast_date` (`forecast_date`),
                                   KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销量预测结果表';

-- 进货建议表
CREATE TABLE `purchase_suggestion` (
                                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',

    -- 关联信息
                                       `product_id` BIGINT NOT NULL COMMENT '商品ID',
                                       `category_id` BIGINT NOT NULL COMMENT '分类ID',

    -- ========== 冗余字段（减少JOIN查询） ==========
                                       `product_code` VARCHAR(50) NOT NULL COMMENT '商品编码',
                                       `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
                                       `category_name` VARCHAR(50) NOT NULL COMMENT '分类名称',
                                       `purchase_price` DECIMAL(10,2) DEFAULT NULL COMMENT '进货单价',

    -- ========== 预测基础数据 ==========
                                       `predicted_quantity` INT NOT NULL COMMENT '预测销量',
                                       `daily_sales` DECIMAL(10,2) DEFAULT 0 COMMENT '日均销量(预测值或近7天均值)',

    -- ========== 库存状态 ==========
                                       `current_stock` INT NOT NULL COMMENT '当前库存',
                                       `safety_stock` INT NOT NULL COMMENT '安全库存',
                                       `target_stock` INT NOT NULL COMMENT '目标库存',

    -- ========== 灯位状态 ==========
                                       `light_status` TINYINT DEFAULT 1 COMMENT '灯位: 1红灯(必须补货) 2黄灯(顺带补货)',

    -- ========== 进货建议 ==========
                                       `suggested_quantity` INT NOT NULL COMMENT '系统建议进货量',
                                       `adjusted_quantity` INT DEFAULT NULL COMMENT '用户调整后的进货量',
                                       `final_quantity` INT NOT NULL COMMENT '最终进货量',

    -- ========== 状态管理 ==========
                                       `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待处理 1已生成进货单 2已忽略',
                                       `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',

    -- ========== 审计字段 ==========
                                       `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                                       PRIMARY KEY (`id`),
                                       KEY `idx_category_id` (`category_id`),
                                       KEY `idx_status` (`status`),
                                       KEY `idx_light_status` (`light_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进货建议表';

-- 进货单主表
CREATE TABLE `purchase_order` (
                                  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `order_no` VARCHAR(32) NOT NULL COMMENT '进货单号',

    -- ========== 订单信息 ==========
                                  `total_quantity` INT NOT NULL DEFAULT 0 COMMENT '商品总数量',
                                  `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '进货总金额',

    -- ========== 时间信息 ==========
                                  `order_date` DATE NOT NULL COMMENT '下单日期',
                                  `expected_date` DATE DEFAULT NULL COMMENT '预计到货日期',
                                  `actual_arrival_date` DATE DEFAULT NULL COMMENT '实际到货日期',

    -- ========== 状态管理 ==========
                                  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待确认 1已下单 2已完成 3已取消',

    -- ========== 其他 ==========
                                  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
                                  `operator` VARCHAR(50) DEFAULT NULL COMMENT '操作人',

                                  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_order_no` (`order_no`),
                                  KEY `idx_order_date` (`order_date`),
                                  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进货单主表';

-- 进货单明细表
CREATE TABLE `purchase_order_item` (
                                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                       `order_id` BIGINT NOT NULL COMMENT '进货单ID',
                                       `order_no` VARCHAR(32) NOT NULL COMMENT '进货单号(冗余)',

    -- ========== 商品信息 ==========
                                       `product_id` BIGINT NOT NULL COMMENT '商品ID',

    -- ========== 冗余字段 ==========
                                       `product_code` VARCHAR(50) NOT NULL COMMENT '商品编码',
                                       `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
                                       `category_name` VARCHAR(50) DEFAULT NULL COMMENT '分类名称',

    -- ========== 进货信息 ==========
                                       `purchase_price` DECIMAL(10,2) NOT NULL COMMENT '进货单价',
                                       `quantity` INT NOT NULL COMMENT '进货数量',
                                       `subtotal_amount` DECIMAL(12,2) NOT NULL COMMENT '小计金额',

    -- ========== 关联进货建议 ==========
                                       `suggestion_id` BIGINT DEFAULT NULL COMMENT '关联的进货建议ID',

                                       `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                                       PRIMARY KEY (`id`),
                                       KEY `idx_order_id` (`order_id`),
                                       KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进货单明细表';