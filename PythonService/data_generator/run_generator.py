# -*- coding: utf-8 -*-
"""
销售数据生成器 - 主入口
运行此脚本生成销售数据并写入数据库

使用方法:
    python run_generator.py                  # 生成默认日期范围数据
    python run_generator.py --clear          # 清空现有数据后重新生成
    python run_generator.py --start 2025-12-01 --end 2025-12-31  # 指定日期范围
"""

import argparse
import time
from datetime import datetime, timedelta

from sales_generator import SalesDataGenerator
from db_manager import (
    DatabaseManager, get_calendar_factors, get_products,
    clear_sales_data, insert_orders, insert_order_items, get_max_order_id
)


def print_daily_statistics(db: DatabaseManager, date_str: str):
    """打印某天的统计信息"""
    result = db.query(f"""
        SELECT
            COUNT(*) as order_count,
            SUM(total_amount) as total_amount,
            AVG(total_amount) as avg_amount
        FROM sales_order
        WHERE sale_date = '{date_str}'
    """)
    if result and result[0]['order_count'] > 0:
        row = result[0]
        print(f"  {date_str}: {row['order_count']}单, "
              f"销售额{float(row['total_amount']):,.0f}元, "
              f"客单价{float(row['avg_amount']):.1f}元")
        return float(row['total_amount'])
    return 0


def main():
    # 解析命令行参数
    parser = argparse.ArgumentParser(description='销售数据生成器')
    parser.add_argument('--start', default='2025-12-01', help='开始日期 (默认: 2025-12-01)')
    parser.add_argument('--end', default='2025-12-31', help='结束日期 (默认: 2025-12-31)')
    parser.add_argument('--clear', action='store_true', help='清空现有数据后重新生成')
    parser.add_argument('--batch-size', type=int, default=1000, help='批量插入大小 (默认: 1000)')
    args = parser.parse_args()

    print("\n" + "=" * 60)
    print("        中小型超市销售数据生成器（行业标准版）")
    print("=" * 60)
    print(f"日期范围: {args.start} ~ {args.end}")
    print(f"清空数据: {'是' if args.clear else '否'}")
    print("=" * 60)

    # 1. 获取日历因子数据
    print("\n[1/5] 获取日历因子数据...")
    calendar_factors = get_calendar_factors(args.start, args.end)
    if not calendar_factors:
        print(f"错误: 未找到 {args.start} ~ {args.end} 的日历因子数据")
        print("请先在前端页面初始化日历因子")
        return

    print(f"   找到 {len(calendar_factors)} 天的日历因子数据")
    date_range = [cf['date'] for cf in calendar_factors]
    print(f"   实际日期范围: {min(date_range)} ~ {max(date_range)}")

    # 2. 获取商品数据
    print("\n[2/5] 获取商品数据...")
    products = get_products()
    if not products:
        print("错误: 未找到商品数据，请先导入商品")
        return

    print(f"   找到 {len(products)} 个商品")

    # 3. 初始化生成器
    print("\n[3/5] 初始化数据生成器...")
    generator = SalesDataGenerator(calendar_factors, products)

    # 4. 生成销售数据
    print("\n[4/5] 生成销售数据...")
    start_time = time.time()

    # 获取起始订单ID
    if args.clear:
        db = DatabaseManager()
        db.connect()
        clear_sales_data(db)
        start_order_id = 1
    else:
        start_order_id = get_max_order_id() + 1
        db = DatabaseManager()
        db.connect()

    orders, items = generator.generate_all_orders(args.start, args.end, start_order_id)

    elapsed = time.time() - start_time
    print(f"   生成完成，耗时 {elapsed:.2f} 秒")

    # 打印统计
    generator.print_statistics()

    # 预期验证
    print("\n行业标准参考:")
    print("  日均营业额: 15,000-30,000 元")
    print("  日均订单数: 400-800 单")
    print("  平均客单价: 30-50 元")
    print("  每单商品数: 3-6 件")

    # 5. 写入数据库
    print("\n[5/5] 写入数据库...")

    # 批量插入
    batch_size = args.batch_size
    total_orders = len(orders)
    total_items = len(items)

    # 插入订单
    order_count = 0
    for i in range(0, total_orders, batch_size):
        batch = orders[i:i + batch_size]
        insert_orders(db, batch)
        order_count += len(batch)
        print(f"\r   订单进度: {order_count}/{total_orders} ({order_count * 100 // total_orders}%)", end='', flush=True)
    print(f"\n   订单插入完成: {order_count} 条")

    # 插入订单明细
    item_count = 0
    for i in range(0, total_items, batch_size):
        batch = items[i:i + batch_size]
        insert_order_items(db, batch)
        item_count += len(batch)
        print(f"\r   明细进度: {item_count}/{total_items} ({item_count * 100 // total_items}%)", end='', flush=True)
    print(f"\n   明细插入完成: {item_count} 条")

    # 验证数据库中的数据
    print("\n" + "=" * 60)
    print("        数据验证")
    print("=" * 60)

    # 总体统计
    result = db.query("SELECT COUNT(*) as cnt FROM sales_order")
    db_orders = result[0]['cnt'] if result else 0

    result = db.query("SELECT COUNT(*) as cnt FROM sales_order_item")
    db_items = result[0]['cnt'] if result else 0

    result = db.query("SELECT SUM(total_amount) as total FROM sales_order")
    db_amount = float(result[0]['total']) if result and result[0]['total'] else 0

    print(f"数据库订单总数: {db_orders:,}")
    print(f"数据库明细总数: {db_items:,}")
    print(f"数据库总销售额: {db_amount:,.2f} 元")

    # 每日统计
    print("\n每日销售统计:")
    start = datetime.strptime(args.start, '%Y-%m-%d')
    end = datetime.strptime(args.end, '%Y-%m-%d')
    current = start
    total_daily = 0
    count_daily = 0

    while current <= end:
        date_str = current.strftime('%Y-%m-%d')
        amount = print_daily_statistics(db, date_str)
        if amount > 0:
            total_daily += amount
            count_daily += 1
        current += timedelta(days=1)

    if count_daily > 0:
        avg_daily = total_daily / count_daily
        print(f"\n日均销售额: {avg_daily:,.0f} 元")

    db.close()

    # 完成
    total_elapsed = time.time() - start_time
    print("\n" + "=" * 60)
    print("        数据生成完成!")
    print("=" * 60)
    print(f"总耗时: {total_elapsed:.2f} 秒")


if __name__ == '__main__':
    main()