# 销售预测服务

中小型超市日销量预测与智能进货系统 - Python服务模块

## 重要更新：Top-Down架构

**核心改进：** 采用"顶层向下生成法"(Top-Down)，解决了Bottom-Up架构的"零和博弈"问题。

**生成流程：**
1. 计算每种商品今天的应售数量（严格按公式，不受其他商品影响）
2. 把所有商品放入池子并打乱
3. 从池子里抓取商品打包成订单，直到池子清空

**优势：**
- 各商品销量独立波动，不会互相踩踏
- 炎热天气会带动整体客流增加，而不是抢洗发水的销量
- 预测模型能精准捕捉每个影响因子

## 项目结构

```
PythonService/
├── config/                     # 配置模块
│   ├── __init__.py
│   └── products_config.py      # 101商品影响因子配置
├── data_generator/             # 数据生成模块
│   ├── __init__.py
│   ├── sales_generator.py      # 销售数据生成器核心
│   ├── db_manager.py           # 数据库连接管理
│   └── run_generator.py        # 主入口脚本
├── forecast/                   # 预测模块（待开发）
│   └── __init__.py
├── requirements.txt            # Python依赖
└── README.md
```

## 快速开始

### 1. 安装依赖

```bash
cd PythonService
pip install -r requirements.txt
```

### 2. 生成销售数据

**前提条件：**
- MySQL数据库已运行
- 数据库名：`supermarket_forecasting_db`
- 商品数据已导入（执行mock.sql）
- 日历因子数据已初始化（前端页面操作）

**运行命令：**

```bash
cd data_generator

# 生成默认日期范围数据（2025-12-01 ~ 2026-04-30）
python run_generator.py

# 清空现有数据后重新生成
python run_generator.py --clear

# 指定日期范围
python run_generator.py --start 2025-12-01 --end 2026-04-30
```

## 最终数据统计（2025-12-01 ~ 2026-04-30）

| 指标 | 数值 | 行业标准 |
|------|------|----------|
| 时间范围 | 2025-12-01 ~ 2026-04-30（151天） | - |
| 总订单数 | **55,500 条** | - |
| 总订单明细 | **229,089 条** | - |
| 总销售额 | **2,375,661.60 元** | - |
| 日均销售额 | ~15,733 元 | 15,000-30,000元 ✓ |
| 平均客单价 | 42.80 元 | 30-50元 ✓ |
| 平均每单商品数 | 4.1 件 | 3-6件 ✓ |

## 数据生成优化历程

| 阶段 | 问题 | 解决方案 |
|------|------|----------|
| 1. Bottom-Up架构 | 零和博弈：商品互相抢名额 | Top-Down架构：先算销量再打包订单 |
| 2. 池子通胀 | 抓取商品后加随机数量 | 数量完全由池子决定 |
| 3. 购物车合并 | 同一订单重复商品 | 字典合并同类项 |
| 4. 时间穿越 | 订单号与时间倒挂 | 先按时间排序再分配订单号 |
| 5. 大数定律抵消 | 总营业额曲线过于平滑 | 引入全局大盘波动(±5%) |
| 6. 小销量商品平滑 | round()导致销量固定 | 概率进位法 |

## 优化验证结果

| 优化 | 验证结果 |
|------|----------|
| 全局大盘波动 | 工作日总营业额波动范围24.4%，有真实呼吸感 |
| 概率进位法 | SP3103日均4.7,范围[3-7],波动84.8% |

## 数据生成规则

### 销量模拟公式

```
最终日销量 = 基础销量 × 星期因子 × 天气因子 × 节假日因子 × (1 + 随机波动)
```

### 参数说明

| 参数 | 说明 | 示例 |
|------|------|------|
| base_sales | 基础日销量 | 可乐: 50听/天 |
| weekday_factor | 星期因子 | 周日: 1.4 (周末效应) |
| weather_factor | 天气因子 | 炎热: 1.8 (天气敏感) |
| holiday_factor | 节假日因子 | 春节: 1.3 |
| noise_range | 随机波动 | ±15% |

### 商品特征类型

1. **天气敏感型**：饮料、冰淇淋（炎热天销量翻倍）
2. **节假日爆发型**：坚果、糖果（节假日因子1.8-2.0）
3. **周末偏好型**：零食、啤酒（周末销量+40%）
4. **稳定刚需型**：调味品、日用品（波动小）
5. **节前囤货型**：粮油、肉类（节前销量激增）

## 数据库配置

数据库连接配置在 `data_generator/db_manager.py` 中：

```python
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'supermarket_forecasting_db',
    'charset': 'utf8mb4'
}
```

## API使用示例

### 获取商品配置

```python
from config import PRODUCTS_CONFIG, get_product_config

# 获取单个商品配置
config = get_product_config("SP2101")  # 可口可乐
print(config['base_sales'])  # 50

# 获取所有商品
all_products = get_all_products()
```

### 生成销售数据

```python
from data_generator import SalesDataGenerator, get_calendar_factors, get_products

# 获取数据
calendar_factors = get_calendar_factors('2025-12-01', '2026-04-30')
products = get_products()

# 初始化生成器
generator = SalesDataGenerator(calendar_factors, products)

# 生成日期范围内所有订单
orders, items = generator.generate_all_orders('2025-12-01', '2026-04-30')
```

## 注意事项

1. **运行前确认**：确保数据库中日历因子表已有数据
2. **数据清理**：使用 `--clear` 参数会清空现有销售数据
3. **性能**：生成150天数据约需5-7秒
4. **编码**：数据库使用utf8mb4编码