# -*- coding: utf-8 -*-
"""
验证数据生成是否正确应用影响因子
"""

import sys
sys.path.insert(0, '..')

from data_generator.sales_generator import SalesDataGenerator
from data_generator.db_manager import get_calendar_factors, get_products
from config.products_config import PRODUCTS_CONFIG
from collections import defaultdict
import pymysql

# 获取数据
calendar_factors = get_calendar_factors('2025-12-01', '2025-12-31')
products = get_products()

# 初始化生成器
generator = SalesDataGenerator(calendar_factors, products)

print('=' * 70)
print('商品销量验证（验证因子是否生效）')
print('=' * 70)

# 连接数据库获取实际销量
db = pymysql.connect(
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='supermarket_forecasting_db',
    charset='utf8mb4'
)

cursor = db.cursor(pymysql.cursors.DictCursor)

# 分析周末效应
print('\n【周末效应验证】')
print('对比工作日和周末的商品销量差异')

# 查询周末（周六周日）的商品销量
cursor.execute("""
    SELECT soi.product_code, soi.product_name,
           SUM(soi.quantity) as total_qty,
           COUNT(DISTINCT soi.sale_date) as days
    FROM sales_order_item soi
    JOIN calendar_factor cf ON soi.sale_date = cf.date
    WHERE cf.is_weekend = 1
    GROUP BY soi.product_code, soi.product_name
    ORDER BY total_qty DESC
    LIMIT 10
""")
weekend_sales = cursor.fetchall()

cursor.execute("""
    SELECT soi.product_code, soi.product_name,
           SUM(soi.quantity) as total_qty,
           COUNT(DISTINCT soi.sale_date) as days
    FROM sales_order_item soi
    JOIN calendar_factor cf ON soi.sale_date = cf.date
    WHERE cf.is_weekend = 0
    GROUP BY soi.product_code, soi.product_name
    ORDER BY total_qty DESC
    LIMIT 10
""")
weekday_sales = cursor.fetchall()

print('\n周末销量Top10:')
print(f"{'商品编码':<10} {'商品名':<20} {'销量':>8} {'天数':>6} {'日均':>8}")
for row in weekend_sales:
    avg = row['total_qty'] / row['days']
    print(f"{row['product_code']:<10} {row['product_name'][:18]:<20} {row['total_qty']:>8} {row['days']:>6} {avg:>8.1f}")

print('\n工作日销量Top10:')
print(f"{'商品编码':<10} {'商品名':<20} {'销量':>8} {'天数':>6} {'日均':>8}")
for row in weekday_sales:
    avg = row['total_qty'] / row['days']
    print(f"{row['product_code']:<10} {row['product_name'][:18]:<20} {row['total_qty']:>8} {row['days']:>6} {avg:>8.1f}")

# 计算周末系数
print('\n【周末系数对比】')
print('对比实际日均销量与配置的周末因子')

weekend_dict = {row['product_code']: row['total_qty'] / row['days'] for row in weekend_sales}
weekday_dict = {row['product_code']: row['total_qty'] / row['days'] for row in weekday_sales}

print(f"{'商品编码':<10} {'商品名':<20} {'工作日日均':>12} {'周末日均':>12} {'实际系数':>10} {'配置系数':>10}")
print('-' * 80)

for code in ['SP2101', 'SP2102', 'SP1101', 'SP2201', 'SP3101']:
    if code in weekday_dict and code in weekend_dict:
        weekday_avg = weekday_dict[code]
        weekend_avg = weekend_dict[code]

        # 实际周末系数 = 周末日均 / 工作日日均
        actual_factor = weekend_avg / weekday_avg

        # 配置的周末系数 (周六周日的平均值)
        config = PRODUCTS_CONFIG.get(code)
        if config:
            config_factor = (config['weekday_factor'].get(6, 1.0) + config['weekday_factor'].get(7, 1.0)) / 2

            # 找到商品名
            product_name = config['name']

            print(f"{code:<10} {product_name[:18]:<20} {weekday_avg:>12.1f} {weekend_avg:>12.1f} {actual_factor:>10.2f} {config_factor:>10.2f}")

# 天气因子验证
print('\n【天气因子验证】')
print('查看不同天气下商品销量差异')

cursor.execute("""
    SELECT cf.weather, soi.product_code, soi.product_name,
           SUM(soi.quantity) as total_qty,
           COUNT(DISTINCT cf.date) as days
    FROM sales_order_item soi
    JOIN calendar_factor cf ON soi.sale_date = cf.date
    WHERE soi.product_code IN ('SP2101', 'SP2102', 'SP2103', 'SP3201', 'SP3202')
    GROUP BY cf.weather, soi.product_code, soi.product_name
    ORDER BY cf.weather, total_qty DESC
""")
weather_sales = cursor.fetchall()

print(f"{'天气':<8} {'商品编码':<10} {'商品名':<20} {'销量':>8} {'天数':>6} {'日均':>8}")
for row in weather_sales:
    avg = row['total_qty'] / row['days']
    print(f"{row['weather']:<8} {row['product_code']:<10} {row['product_name'][:18]:<20} {row['total_qty']:>8} {row['days']:>6} {avg:>8.1f}")

db.close()

print('\n' + '=' * 70)
print('验证结论：')
print('如果实际系数与配置系数接近，说明Top-Down架构正确应用了影响因子')
print('=' * 70)