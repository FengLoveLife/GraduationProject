# -*- coding: utf-8 -*-
"""
验证两个优化是否生效：
1. 全局大盘波动 - 总营业额曲线是否有呼吸感（不仅仅是周末效应）
2. 概率进位法 - 小销量商品是否有真实波动
"""

import sys
sys.path.insert(0, '..')

import pymysql

# 连接数据库
db = pymysql.connect(
    host='localhost',
    port=3306,
    user='root',
    password='123456',
    database='supermarket_forecasting_db',
    charset='utf8mb4'
)

cursor = db.cursor(pymysql.cursors.DictCursor)

print('=' * 70)
print('优化效果验证')
print('=' * 70)

# ==========================================
# 验证1：全局大盘波动 - 总营业额曲线
# ==========================================
print('\n【优化1：全局大盘波动】验证')
print('检查工作日（排除周末效应）的总营业额是否有波动')

# 查询工作日的总销售额
cursor.execute("""
    SELECT so.sale_date, SUM(so.total_amount) as daily_amount
    FROM sales_order so
    JOIN calendar_factor cf ON so.sale_date = cf.date
    WHERE cf.is_weekend = 0
    GROUP BY so.sale_date
    ORDER BY so.sale_date
""")

weekday_sales = cursor.fetchall()

amounts = [float(row['daily_amount']) for row in weekday_sales]
avg_amount = sum(amounts) / len(amounts)
min_amount = min(amounts)
max_amount = max(amounts)
std_dev = (sum((x - avg_amount) ** 2 for x in amounts) / len(amounts)) ** 0.5

print(f"\n工作日总营业额统计（{len(amounts)}天）：")
print(f"  平均值: {avg_amount:,.0f} 元")
print(f"  最小值: {min_amount:,.0f} 元")
print(f"  最大值: {max_amount:,.0f} 元")
print(f"  标准差: {std_dev:,.0f} 元")
print(f"  波动范围: {(max_amount - min_amount) / avg_amount * 100:.1f}%")

# 检查是否有多样化的波动（而不是平滑的曲线）
if std_dev > avg_amount * 0.05:  # 标准差超过平均值5%
    print("\n[OK] 工作日总营业额有明显的波动，全局大盘波动生效！")
else:
    print("\n[WARNING] 工作日总营业额过于平滑，全局大盘波动可能未生效")

# ==========================================
# 验证2：概率进位法 - 小销量商品波动
# ==========================================
print('\n【优化2：概率进位法】验证')
print('检查小销量商品的日销量是否有变化')

# 找一个基础销量小的商品（比如车厘子，base_sales=4）
# 先查询销量最小的几个商品
cursor.execute("""
    SELECT product_code, product_name, SUM(quantity) as total_qty,
           AVG(quantity) as avg_daily_qty,
           COUNT(DISTINCT sale_date) as days,
           STDDEV(quantity) as std_qty
    FROM sales_order_item
    GROUP BY product_code, product_name
    HAVING COUNT(DISTINCT sale_date) >= 5
    ORDER BY AVG(quantity) ASC
    LIMIT 10
""")

low_sales_products = cursor.fetchall()

print(f"\n小销量商品波动分析：")
print(f"{'商品编码':<10} {'商品名':<20} {'日均销量':>10} {'销量标准差':>10} {'天数':>6}")
print('-' * 65)

has_fluctuation = False
for row in low_sales_products:
    std_qty = float(row['std_qty']) if row['std_qty'] else 0
    avg_qty = float(row['avg_daily_qty'])
    print(f"{row['product_code']:<10} {row['product_name'][:18]:<20} {avg_qty:>10.2f} {std_qty:>10.2f} {row['days']:>6}")

    # 如果日均销量<=5且标准差>0.3，说明有波动
    if avg_qty <= 5 and std_qty > 0.3:
        has_fluctuation = True

if has_fluctuation:
    print("\n[OK] 小销量商品有明显的销量波动，概率进位法生效！")
else:
    print("\n[WARNING] 小销量商品销量过于固定，概率进位法可能未生效")

# ==========================================
# 综合结论
# ==========================================
print('\n' + '=' * 70)
print('综合验证结论')
print('=' * 70)

optimization1_ok = std_dev > avg_amount * 0.05
optimization2_ok = has_fluctuation

print(f"优化1（全局大盘波动）: {'[OK] 已生效' if optimization1_ok else '[WARNING] 可能未生效'}")
print(f"优化2（概率进位法）: {'[OK] 已生效' if optimization2_ok else '[WARNING] 可能未生效'}")

if optimization1_ok and optimization2_ok:
    print("\n[SUCCESS] 两个优化都已生效，数据生成达到完美状态！")
else:
    print("\n部分优化可能未生效，请检查代码")

db.close()