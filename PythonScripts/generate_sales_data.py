# -*- coding: utf-8 -*-
"""
销售数据模拟生成脚本
生成带有规律性的30天销售数据，用于训练预测模型
"""

import pandas as pd
import numpy as np
from datetime import datetime, timedelta
import random
import pymysql
from openpyxl import Workbook

# ==================== 配置参数 ====================

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'supermarket_forecasting_db',
    'charset': 'utf8mb4'
}

# 模拟时间范围
START_DATE = datetime(2026, 2, 15)
END_DATE = datetime(2026, 3, 16)
TOTAL_DAYS = 30

# 收银机列表
POS_MACHINES = ['POS-01', 'POS-02', 'POS-03']

# 支付方式: 1现金 2微信 3支付宝
PAYMENT_TYPES = [1, 2, 3]
PAYMENT_WEIGHTS = [0.2, 0.5, 0.3]  # 微信支付最多

# 品类基础销量配置 (category_id -> (基础销量最小值, 最大值))
CATEGORY_BASE_SALES = {
    # 酒水饮料 (21-25) - 销量最高，天气敏感
    21: (15, 30),  # 碳酸饮料
    22: (20, 40),  # 饮用水
    23: (12, 25),  # 果汁茶饮
    24: (15, 30),  # 乳制品
    25: (8, 18),   # 啤酒麦酒

    # 生鲜果蔬 (31-33) - 天气敏感
    31: (12, 25),  # 新鲜水果
    32: (10, 20),  # 时令蔬菜
    33: (8, 15),   # 肉禽蛋品

    # 休闲零食 (11-14) - 周末效应明显
    11: (8, 18),   # 膨化食品
    12: (6, 15),   # 饼干糕点
    13: (5, 12),   # 坚果炒货
    14: (8, 18),   # 糖果巧克力

    # 粮油调味 (41-43) - 稳定
    41: (3, 8),    # 米面杂粮
    42: (2, 6),    # 食用油
    43: (4, 10),   # 调味品

    # 日用百货 (51-53) - 稳定
    51: (6, 15),   # 纸巾湿巾
    52: (4, 10),   # 洗护清洁
    53: (5, 12),   # 家居用品
}

# ==================== 因子定义 ====================

def get_weekday_factor(date):
    """
    星期因子
    周末销量更高
    """
    weekday = date.weekday()  # 0=周一, 6=周日
    factors = {
        0: 0.85,  # 周一
        1: 0.90,  # 周二
        2: 0.90,  # 周三
        3: 0.95,  # 周四
        4: 1.10,  # 周五
        5: 1.40,  # 周六
        6: 1.50,  # 周日
    }
    return factors[weekday]

def generate_weather_data(start_date, total_days):
    """
    生成模拟天气数据
    返回每天对应的天气类型和温度
    """
    weather_list = []
    # 天气类型: 0=晴, 1=多云, 2=小雨, 3=大雨, 4=炎热
    weather_types = [0, 1, 2, 3, 4]
    weather_weights = [0.35, 0.30, 0.20, 0.10, 0.05]  # 晴天最多

    # 温度范围 (2-3月，春季)
    temp_min_range = (5, 15)
    temp_max_range = (12, 25)

    for i in range(total_days):
        date = start_date + timedelta(days=i)
        weather = np.random.choice(weather_types, p=weather_weights)

        # 根据天气类型调整温度范围
        if weather == 4:  # 炎热
            temp = random.randint(28, 35)
        elif weather == 3:  # 大雨
            temp = random.randint(10, 18)
        else:
            temp = random.randint(temp_min_range[1], temp_max_range[1])

        weather_list.append({
            'date': date,
            'weather': weather,
            'temperature': temp
        })

    return weather_list

def get_weather_factor(weather, temperature, category_id):
    """
    天气因子
    根据天气类型和品类调整销量
    """
    base_factor = 1.0

    # 饮品类 (category_id 21-25) 天气敏感
    if category_id in [21, 22, 23, 24]:
        if weather == 4:  # 炎热
            base_factor = 2.0
        elif weather == 3:  # 大雨
            base_factor = 0.7
        elif weather == 2:  # 小雨
            base_factor = 0.85

    # 水果类 (category_id 31)
    elif category_id == 31:
        if weather == 4:  # 炎热
            base_factor = 1.5
        elif weather == 3:  # 大雨
            base_factor = 0.7

    # 其他品类整体影响
    else:
        if weather == 3:  # 大雨 - 整体客流下降
            base_factor = 0.6
        elif weather == 2:  # 小雨
            base_factor = 0.85

    return base_factor

def get_promotion_factor():
    """
    促销因子
    随机决定是否促销及促销力度
    """
    is_promotion = 1 if random.random() < 0.15 else 0  # 15%概率促销
    if is_promotion:
        return random.uniform(3.0, 5.0), 1
    return 1.0, 0

# ==================== 数据生成核心逻辑 ====================

def get_products_from_db():
    """
    从数据库获取商品数据
    """
    conn = pymysql.connect(**DB_CONFIG)
    try:
        sql = """
        SELECT
            id,
            product_code,
            name,
            category_id,
            purchase_price,
            sale_price
        FROM product
        WHERE status = 1
        ORDER BY id
        """
        df = pd.read_sql(sql, conn)
        print(f"从数据库获取到 {len(df)} 个商品")
        return df
    finally:
        conn.close()

def generate_daily_sales(products, weather_data, date, date_index):
    """
    生成某一天的销售数据
    """
    daily_records = []
    weather = weather_data[date_index]['weather']
    temperature = weather_data[date_index]['temperature']

    # 星期因子
    weekday_factor = get_weekday_factor(date)

    # 生成订单
    # 每天生成 25-45 个订单
    order_count = random.randint(25, 45)
    # 周末订单更多
    if date.weekday() in [5, 6]:
        order_count = int(order_count * 1.3)

    for order_idx in range(order_count):
        order_no = f"ORD-{date.strftime('%Y%m%d')}-{order_idx + 1:03d}"
        pos_machine = random.choice(POS_MACHINES)
        payment_type = np.random.choice(PAYMENT_TYPES, p=PAYMENT_WEIGHTS)

        # 每个订单包含 1-5 个商品
        items_in_order = random.randint(1, 5)

        # 随机选择商品
        selected_products = products.sample(n=min(items_in_order, len(products)))

        # 生成销售时间 (9:00 - 21:00)
        hour = random.randint(9, 20)
        minute = random.randint(0, 59)
        second = random.randint(0, 59)
        sale_time = date.replace(hour=hour, minute=minute, second=second)

        for _, product in selected_products.iterrows():
            category_id = product['category_id']

            # 基础销量
            base_min, base_max = CATEGORY_BASE_SALES.get(category_id, (5, 15))
            base_sales = random.randint(base_min, base_max)

            # 应用各因子
            weather_factor = get_weather_factor(weather, temperature, category_id)
            promotion_factor, is_promotion = get_promotion_factor()

            # 最终销量计算
            final_sales = base_sales * weekday_factor * weather_factor * promotion_factor

            # 添加随机扰动 (±15%)
            noise = random.uniform(-0.15, 0.15)
            final_sales = final_sales * (1 + noise)

            # 四舍五入取整，最小为0
            final_sales = max(0, round(final_sales))

            # 单个订单中该商品的数量通常是 1-3 个
            # 这里 final_sales 是该商品当天总销量预期
            # 我们把它分配到各个订单中
            quantity = max(1, min(final_sales // order_count + random.randint(0, 2), 10))

            # 实际售价 (可能有小幅波动，或促销价)
            unit_price = float(product['sale_price'])
            if is_promotion:
                # 促销时价格打8-9折
                unit_price = round(unit_price * random.uniform(0.8, 0.9), 2)

            record = {
                '订单编号': order_no,
                '商品编码': product['product_code'],
                '实际售价': unit_price,
                '销售数量': quantity,
                '是否促销': is_promotion,
                '支付方式': payment_type,
                '销售时间': sale_time.strftime('%Y-%m-%d %H:%M:%S'),
                '收银机编号': pos_machine
            }
            daily_records.append(record)

    return daily_records

def generate_all_sales_data():
    """
    生成所有模拟销售数据
    """
    print("=" * 50)
    print("开始生成模拟销售数据...")
    print("=" * 50)

    # 1. 获取商品数据
    products = get_products_from_db()

    # 2. 生成天气数据
    weather_data = generate_weather_data(START_DATE, TOTAL_DAYS)
    print(f"生成 {TOTAL_DAYS} 天天气数据完成")

    # 3. 逐日生成销售数据
    all_records = []

    for day_idx in range(TOTAL_DAYS):
        date = START_DATE + timedelta(days=day_idx)
        weather = weather_data[day_idx]

        daily_records = generate_daily_sales(products, weather_data, date, day_idx)
        all_records.extend(daily_records)

        # 打印进度
        weather_text = ['晴', '多云', '小雨', '大雨', '炎热'][weather['weather']]
        print(f"  {date.strftime('%Y-%m-%d')} ({['一','二','三','四','五','六','日'][date.weekday()]}) "
              f"天气:{weather_text} 温度:{weather['temperature']}°C - 生成 {len(daily_records)} 条记录")

    print("-" * 50)
    print(f"总计生成 {len(all_records)} 条销售记录")

    # 4. 转换为DataFrame
    df = pd.DataFrame(all_records)

    # 5. 保存为Excel
    output_file = '模拟销售数据_30天.xlsx'
    df.to_excel(output_file, index=False, sheet_name='销售数据')

    print("=" * 50)
    print(f"数据生成完成！文件保存至: {output_file}")
    print("=" * 50)

    # 6. 输出统计信息
    print("\n数据统计:")
    print(f"  - 日期范围: {START_DATE.strftime('%Y-%m-%d')} ~ {END_DATE.strftime('%Y-%m-%d')}")
    print(f"  - 总记录数: {len(df)}")
    print(f"  - 订单数量: {df['订单编号'].nunique()}")
    print(f"  - 商品种类: {df['商品编码'].nunique()}")
    print(f"  - 促销商品: {df[df['是否促销']==1]['商品编码'].nunique()} 种")

    # 按日期统计
    df['日期'] = pd.to_datetime(df['销售时间']).dt.date
    daily_stats = df.groupby('日期').agg({
        '订单编号': 'nunique',
        '销售数量': 'sum',
        '实际售价': lambda x: (x * df.loc[x.index, '销售数量']).sum()
    }).rename(columns={
        '订单编号': '订单数',
        '销售数量': '商品件数',
        '实际售价': '销售额'
    })
    print("\n每日销售概览:")
    print(daily_stats.to_string())

    return df

# ==================== 主程序入口 ====================

if __name__ == '__main__':
    # 设置随机种子，保证可复现
    random.seed(42)
    np.random.seed(42)

    # 生成数据
    df = generate_all_sales_data()

    print("\n[OK] 完成！请将生成的 Excel 文件导入系统。")