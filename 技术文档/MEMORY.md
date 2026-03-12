# 毕业设计项目记忆 - 中小型超市日销量预测与智能进货系统

## 一、项目概述

### 1.1 项目背景
为中小型超市开发一套集商品管理、库存监控、销售数据分析、销量预测、智能补货建议于一体的综合管理系统。

### 1.2 核心业务目标
1. **商品档案管理** - 商品信息与分类的维护基础
2. **库存预警监控** - 实时掌握库存健康度，防止缺货/积压
3. **销售数据中心** - 销售流水记录与统计分析，为预测提供数据支撑
4. **智能销量预测** - 基于历史销售数据与影响因子，预测未来销量
5. **智能补货建议** - 结合预测销量、库存、安全库存等，生成补货建议

### 1.3 技术栈
| 层次 | 技术选型 | 版本 |
|------|---------|------|
| 后端框架 | Spring Boot | 3.2.3 |
| ORM框架 | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.x |
| 认证授权 | JWT (jjwt) | 0.12.6 |
| Excel处理 | EasyExcel | 3.3.3 |
| 前端框架 | Vue 3 | 3.5.x |
| 构建工具 | Vite | 7.x |
| UI组件库 | Element Plus | 2.13.x |
| 图表库 | ECharts | 6.0 |
| 状态管理 | Pinia | 3.x |

---

## 二、已完成模块

### 2.1 商品档案管理 (`/base`)
- **商品分类** - 树形结构，支持多级分类
- **商品中心** - CRUD + 图片上传 + Excel导出

### 2.2 库存预警与监控 (`/inventory`)
- **实时库存盘点** - KPI看板 + ECharts图表
- **库存变动流水** - 记录入库/出库/损耗/调整

---

## 三、销售数据中心设计（当前开发）

### 3.1 数据库表设计

#### sales_order（销售订单主表）
```sql
CREATE TABLE `sales_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
    `total_amount` DECIMAL(12,2) NOT NULL COMMENT '订单总金额',
    `total_quantity` INT NOT NULL COMMENT '商品总数量',
    `payment_type` TINYINT NOT NULL COMMENT '支付方式：1现金 2微信 3支付宝',
    `sale_date` DATE NOT NULL COMMENT '销售日期',
    `sale_time` DATETIME NOT NULL COMMENT '销售时间',
    `operator` VARCHAR(50) COMMENT '操作员',
    `remark` VARCHAR(255) COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`)
);
```

#### sales_order_item（销售订单明细表）
```sql
CREATE TABLE `sales_order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT NOT NULL COMMENT '关联订单ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_code` VARCHAR(50) NOT NULL COMMENT '商品编码',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `category_id` BIGINT COMMENT '分类ID',
    `category_name` VARCHAR(50) COMMENT '分类名称',
    `unit_price` DECIMAL(10,2) NOT NULL COMMENT '销售单价',
    `quantity` INT NOT NULL COMMENT '销售数量',
    `subtotal_amount` DECIMAL(12,2) NOT NULL COMMENT '小计金额',
    `sale_date` DATE NOT NULL COMMENT '销售日期',
    PRIMARY KEY (`id`)
);
```

#### calendar_factor（日历影响因子表）
```sql
CREATE TABLE `calendar_factor` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `date` DATE NOT NULL COMMENT '日期',
    `day_of_week` TINYINT NOT NULL COMMENT '星期几 1-7',
    `is_weekend` TINYINT DEFAULT 0 COMMENT '是否周末',
    `is_holiday` TINYINT DEFAULT 0 COMMENT '是否节假日',
    `holiday_name` VARCHAR(50) COMMENT '节假日名称',
    `weather` VARCHAR(20) COMMENT '天气：晴/多云/雨/雪',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date` (`date`)
);
```

### 3.2 影响因子说明

| 因子 | 作用 | 数据来源 |
|------|------|---------|
| 是否周末 | 周末销量通常+20%~50% | 系统计算 |
| 是否节假日 | 节假日销量波动大 | 每年导入 |
| 天气 | 晴/多云/雨/雪影响出行 | API同步/手工维护 |

### 3.3 核心业务逻辑

**销售录入流程**：
1. 选择商品 → 输入数量 → 计算金额
2. 提交订单 → 生成 sales_order + sales_order_item
3. 同时扣减库存 → 生成 inventory_log（type=2 销售出库）

**数据关联**：
```
sales_order_item.sale_date ←→ calendar_factor.date
```

---

## 四、预测模块设计（后续开发）

### 4.1 预测原理
```
历史销量 + 影响因子 → 模型训练 → 预测未来销量
```

### 4.2 训练数据结构
| sale_date | product_id | daily_sales | is_weekend | is_holiday | weather |
|-----------|-----------|-------------|------------|------------|---------|
| 2024-01-01 | 1 | 45 | 0 | 1 | 晴 |

### 4.3 forecast_result（预测结果表）
```sql
CREATE TABLE `forecast_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `forecast_date` DATE NOT NULL COMMENT '预测执行日期',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `target_date` DATE NOT NULL COMMENT '预测目标日期',
    `predicted_quantity` DECIMAL(10,2) NOT NULL COMMENT '预测销量',
    `actual_quantity` DECIMAL(10,2) COMMENT '实际销量',
    PRIMARY KEY (`id`)
);
```

---

## 五、智能补货模块设计（后续开发）

### 5.1 补货决策公式
```
建议补货量 = 预测销量 - 当前库存 + 安全库存
```

### 5.2 restocking_suggestion（补货建议表）
```sql
CREATE TABLE `restocking_suggestion` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `suggestion_date` DATE NOT NULL COMMENT '建议生成日期',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `current_stock` INT NOT NULL COMMENT '当前库存',
    `safety_stock` INT NOT NULL COMMENT '安全库存',
    `predicted_sales_7d` INT COMMENT '未来7天预测销量',
    `suggested_quantity` INT NOT NULL COMMENT '建议补货数量',
    `urgency_level` TINYINT COMMENT '紧急程度：1紧急 2一般 3低',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0待处理 1已采纳 2已忽略',
    PRIMARY KEY (`id`)
);
```

---

## 六、代码规范

### 6.1 后端规范
```
Controller → Service → Mapper → Entity
     ↓          ↓
    DTO        VO
```

- 统一返回：`Result<T>` (code, msg, data)
- 命名：XxxController, IXxxService, XxxServiceImpl, XxxMapper, XxxEntity

### 6.2 前端规范
- Vue 3 Composition API (`<script setup>`)
- 样式：scoped SCSS
- 图表：ECharts
- 组件库：Element Plus

### 6.3 API路径规范
```
GET  /api/xxx/page     分页查询
GET  /api/xxx/{id}     详情
POST /api/xxx/         新增
PUT  /api/xxx/         修改
DELETE /api/xxx/{id}   删除
GET  /api/xxx/export   导出
```

---

## 七、开发计划

| 阶段 | 模块 | 状态 |
|------|------|------|
| 已完成 | 商品档案管理 | ✅ |
| 已完成 | 库存预警与监控 | ✅ |
| **进行中** | **销售数据中心** | 🔄 |
| 待开发 | 智能销量预测 | ⏳ |
| 待开发 | 智能补货建议 | ⏳ |
| 待开发 | 系统安全管理 | ⏳ |

---

## 八、详细设计文档

完整设计文档见：`memory/销售数据中心设计详解.md`