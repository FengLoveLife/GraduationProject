# -*- coding: utf-8 -*-
"""
销售数据生成器（Top-Down架构 - 顶层向下生成法）
基于真实中小型超市业务数据设计

核心改进：
1. 先计算每种商品今天的应售数量（严格按公式，不受其他商品影响）
2. 把这些商品放入池子，随机打乱
3. 从池子里抓取商品打包成订单，直到池子清空

这样避免了Bottom-Up架构的"零和博弈"问题：
- 各商品销量独立波动，不会互相踩踏
- 炎热天气会带动整体客流增加，而不是抢洗发水的销量
- 预测模型能精准捕捉每个影响因子

参考指标（来源：社区型中小型超市行业基准）：
- 日均营业额：5,000-15,000 元（中小型超市）
- 日均订单数：80-150 单
- 平均客单价：30-50 元
- 每单商品数：3-6 件（长尾分布）
"""

import sys
import os
import random
from datetime import datetime, timedelta
from decimal import Decimal
from collections import defaultdict

# 添加项目根目录到路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from config.products_config import PRODUCTS_CONFIG


class SalesDataGenerator:
    """销售数据生成器 - Top-Down架构"""

    # 支付方式
    PAYMENT_TYPES = [1, 2, 3]  # 1现金 2微信 3支付宝
    PAYMENT_WEIGHTS = [0.15, 0.50, 0.35]  # 现金15% 微信50% 支付宝35%

    # POS机编号
    OPERATORS = ['POS-01', 'POS-02', 'POS-03']

    # ==================== 行业标准参数 ====================

    # 订单商品数量分布（长尾分布）
    ORDER_SIZE_DISTRIBUTION = [
        (1, 2, 0.35),   # 1-2件，占35%（口渴/嘴馋型）
        (3, 5, 0.45),   # 3-5件，占45%（今晚做饭型）
        (6, 10, 0.15),  # 6-10件，占15%（周末小采购型）
        (11, 20, 0.05)  # 11-20件，占5%（家庭大采购型）
    ]

    # 时间段分布（双峰曲线，营业时间 8:00-21:00）
    TIME_DISTRIBUTION = [
        (8,  0, 10,  0, 0.12),  # 早高峰 12%
        (10, 0, 12,  0, 0.08),  # 中午前 8%
        (12, 0, 14,  0, 0.15),  # 午餐时段 15%
        (14, 0, 17, 30, 0.20),  # 下午时段 20%
        (17, 30, 20, 0, 0.35),  # 晚高峰 35%
        (20, 0, 21,  0, 0.10),  # 闭店前 10%
    ]

    def __init__(self, calendar_factors: list, products: list):
        """
        初始化生成器

        Args:
            calendar_factors: 日历因子列表 [{date, day_of_week, is_holiday, weather, ...}]
            products: 商品列表 [{id, product_code, name, category_id, purchase_price, sale_price, ...}]
        """
        # 将日期转换为字符串作为key
        self.calendar_factors = {}
        for cf in calendar_factors:
            date_key = cf['date'].strftime('%Y-%m-%d') if hasattr(cf['date'], 'strftime') else str(cf['date'])
            self.calendar_factors[date_key] = cf

        self.products = products
        self.products_by_code = {p['product_code']: p for p in products}

        # 统计信息
        self.total_orders = 0
        self.total_items = 0
        self.total_amount = Decimal('0')

    def calculate_daily_sales_target(self, date_str: str) -> dict:
        """
        计算今天每个商品的应售数量（核心：独立计算，不受其他商品影响）

        公式：最终日销量 = 基础销量 × 星期因子 × 天气因子 × 节假日因子 × (1 + 全局波动 + 单品波动)

        优化点：
        1. 全局大盘波动：解决大数定律抵消效应，让总营业额曲线有真实呼吸感
        2. 概率进位法：解决小销量商品销量永远是固定值的问题

        Args:
            date_str: 日期 'YYYY-MM-DD'

        Returns:
            {product_code: target_quantity} 每个商品今天应售数量
        """
        cf = self.calendar_factors.get(date_str)
        if not cf:
            return {}

        day_of_week = cf.get('day_of_week', 1)
        weather = cf.get('weather', '晴') or '晴'
        is_holiday = cf.get('is_holiday', 0)

        targets = {}

        # ==================== 优化点1：全局大盘波动 ====================
        # 每天先生成一个"全局客流大盘波动"，模拟整体客流的无差别波动
        # 比如：今天隔壁小区停水了，导致整个超市客流都少了5%
        # 这解决了101个商品独立波动加总时正负抵消的问题（大数定律）
        global_noise = random.uniform(-0.05, 0.05)

        for code, config in PRODUCTS_CONFIG.items():
            base = config['base_sales']

            # 获取因子
            wf = config['weekday_factor'].get(day_of_week, 1.0)
            wtf = config['weather_factor'].get(weather, 1.0)
            hf = config['holiday_factor'].get(is_holiday, 1.0)

            # ==================== 优化点2：单品专属波动 ====================
            # 单品专属波动稍微调小一点（因为要加上全局波动）
            # 原本±15%，现在单品波动±10%，加上全局波动±5%，总共还是约±15%
            product_noise_limit = config.get('noise_range', 0.15) - 0.05
            local_noise = random.uniform(-product_noise_limit, product_noise_limit)

            # 最终综合波动 = 全局大盘波动(大家同进退) + 单品自身波动(自己瞎跳)
            total_noise = global_noise + local_noise

            # 计算绝对浮点数目标
            exact_target = base * wf * wtf * hf * (1 + total_noise)

            if exact_target <= 0:
                continue

            # ==================== 优化点3：概率进位法 ====================
            # 解决小销量商品使用round()四舍五入后销量永远是固定值的问题
            # 例如：base_sales=4的车厘子，计算出来3.2，有20%概率变成4，80%是3
            # 这样即使是小销量商品，折线图也会有真实的波动
            int_part = int(exact_target)          # 整数部分 (如 3.2 -> 3)
            frac_part = exact_target - int_part   # 小数部分 (如 3.2 -> 0.2)

            # 以小数部分作为概率，决定是否进位 +1
            target = int_part + 1 if random.random() < frac_part else int_part

            if target > 0:
                targets[code] = target

        return targets

    def build_product_pool(self, targets: dict) -> list:
        """
        构建今天的商品池（把每个商品按目标数量拆成购买事件放入池子）

        每个池子槽位代表一次"购买事件"，事件内携带数量（1-4件）。
        总数量之和严格等于 targets 目标，营业额不变，但明细行会出现多件购买。

        Args:
            targets: {product_code: target_quantity}

        Returns:
            商品池列表 [{'product_code': xxx, 'product_info': xxx, 'qty': N}, ...]
        """
        pool = []

        for code, quantity in targets.items():
            product = self.products_by_code.get(code)
            if not product:
                continue

            remaining = quantity
            while remaining > 0:
                # 决定本次购买事件买几件：不超过剩余量
                if remaining == 1:
                    qty = 1
                elif remaining == 2:
                    qty = random.choices([1, 2], weights=[0.65, 0.35])[0]
                elif remaining == 3:
                    qty = random.choices([1, 2, 3], weights=[0.60, 0.30, 0.10])[0]
                else:
                    qty = random.choices([1, 2, 3, 4], weights=[0.65, 0.25, 0.08, 0.02])[0]
                    qty = min(qty, remaining)

                pool.append({
                    'product_code': code,
                    'product_info': product,
                    'qty': qty
                })
                remaining -= qty

        # 打乱池子（随机化）
        random.shuffle(pool)

        return pool

    def generate_order_size(self) -> int:
        """
        根据长尾分布生成订单商品数量

        Returns:
            商品数量
        """
        rand = random.random()
        cumulative = 0

        for min_size, max_size, prob in self.ORDER_SIZE_DISTRIBUTION:
            cumulative += prob
            if rand < cumulative:
                return random.randint(min_size, max_size)

        return random.randint(3, 5)

    def generate_order_time(self) -> datetime:
        """
        根据时间分布生成订单时间

        Returns:
            时间对象
        """
        rand = random.random()
        cumulative = 0

        for start_h, start_m, end_h, end_m, prob in self.TIME_DISTRIBUTION:
            cumulative += prob
            if rand < cumulative:
                start_minutes = start_h * 60 + start_m
                end_minutes = end_h * 60 + end_m
                random_minutes = random.randint(start_minutes, end_minutes)

                hour = random_minutes // 60
                minute = random_minutes % 60
                second = random.randint(0, 59)

                return datetime(2000, 1, 1, hour, minute, second)

        return datetime(2000, 1, 1, 18, random.randint(0, 59), random.randint(0, 59))

    def generate_orders_from_pool(self, product_pool: list, date_str: str, order_start_id: int) -> tuple:
        """
        从商品池中抓取商品打包成订单（完美修复版）

        修复三大漏洞：
        1. 池子通胀悖论：商品数量完全由池子决定，不再额外添加随机数量
        2. 购物车合并：同一订单中相同商品合并为一条记录
        3. 时间穿越：按时间排序后再分配订单号

        Args:
            product_pool: 商品池
            date_str: 日期 'YYYY-MM-DD'
            order_start_id: 订单起始ID

        Returns:
            (订单列表, 订单明细列表, 下一个订单ID)
        """
        if not product_pool:
            return [], [], order_start_id

        # ==========================================
        # Step 1: 抓取池中商品，生成无序号的"原始订单"，并合并同类项
        # ==========================================
        raw_orders = []
        pool_index = 0
        pool_size = len(product_pool)
        base_date = datetime.strptime(date_str, '%Y-%m-%d')

        while pool_index < pool_size:
            remaining = pool_size - pool_index
            items_count = min(self.generate_order_size(), remaining)

            cart_dict = {}
            # 抓取并合并购物车明细
            for i in range(items_count):
                product = product_pool[pool_index + i]['product_info']
                pid = product['id']

                # 数量由池子槽位预分配的 qty 决定，保证总量与 target 一致
                slot_qty = product_pool[pool_index + i].get('qty', 1)
                if pid in cart_dict:
                    cart_dict[pid]['quantity'] += slot_qty
                else:
                    cart_dict[pid] = {
                        'product': product,
                        'quantity': slot_qty
                    }

            pool_index += items_count

            # 赋予随机时间
            order_time = self.generate_order_time()
            sale_time = base_date.replace(
                hour=order_time.hour,
                minute=order_time.minute,
                second=order_time.second
            )

            raw_orders.append({
                'sale_time': sale_time,
                'cart_dict': cart_dict
            })

        # ==========================================
        # Step 2: 解决"时间穿越"，按销售时间从早到晚排序
        # ==========================================
        raw_orders.sort(key=lambda x: x['sale_time'])

        # ==========================================
        # Step 3: 顺序分配订单号，正式写入数据结构
        # ==========================================
        orders = []
        order_items = []
        current_order_id = order_start_id

        for raw in raw_orders:
            order_no = f"ORD{date_str.replace('-', '')}{current_order_id:05d}"
            sale_time = raw['sale_time']
            cart_dict = raw['cart_dict']

            order_total = Decimal('0')
            order_quantity = 0
            order_items_data = []

            for pid, item_data in cart_dict.items():
                product = item_data['product']
                qty = item_data['quantity']

                unit_price = Decimal(str(product['sale_price']))
                purchase_price = Decimal(str(product['purchase_price']))
                subtotal = unit_price * qty
                profit = (unit_price - purchase_price) * qty

                order_items_data.append({
                    'order_id': current_order_id,
                    'order_no': order_no,
                    'product_id': product['id'],
                    'product_code': product['product_code'],
                    'product_name': product['name'],
                    'category_id': product['category_id'],
                    'category_name': product.get('category_name', ''),
                    'purchase_price': purchase_price,
                    'unit_price': unit_price,
                    'quantity': qty,
                    'subtotal_amount': subtotal,
                    'subtotal_profit': profit,
                    'is_promotion': 0,
                    'sale_date': date_str
                })

                order_total += subtotal
                order_quantity += qty

            payment_type = random.choices(self.PAYMENT_TYPES, weights=self.PAYMENT_WEIGHTS)[0]
            operator = random.choice(self.OPERATORS)

            orders.append({
                'id': current_order_id,
                'order_no': order_no,
                'total_amount': order_total,
                'total_quantity': order_quantity,
                'payment_type': payment_type,
                'sale_date': date_str,
                'sale_time': sale_time,
                'operator': operator,
                'remark': None
            })

            order_items.extend(order_items_data)
            current_order_id += 1

        return orders, order_items, current_order_id

    def generate_orders_for_day(self, date_str: str, order_start_id: int) -> tuple:
        """
        生成某一天的所有订单（Top-Down流程）

        Args:
            date_str: 日期 'YYYY-MM-DD'
            order_start_id: 订单起始ID

        Returns:
            (订单列表, 订单明细列表, 下一个订单ID)
        """
        # Step 1: 计算今天每个商品的应售数量
        targets = self.calculate_daily_sales_target(date_str)
        if not targets:
            return [], [], order_start_id

        # Step 2: 构建商品池并打乱
        product_pool = self.build_product_pool(targets)

        # Step 3: 从池中抓取商品打包成订单
        orders, items, next_id = self.generate_orders_from_pool(product_pool, date_str, order_start_id)

        return orders, items, next_id

    def generate_all_orders(self, start_date: str, end_date: str, start_order_id: int = 1) -> tuple:
        """
        生成日期范围内所有订单

        Args:
            start_date: 开始日期 'YYYY-MM-DD'
            end_date: 结束日期 'YYYY-MM-DD'
            start_order_id: 起始订单ID

        Returns:
            (所有订单列表, 所有订单明细列表)
        """
        all_orders = []
        all_items = []
        current_order_id = start_order_id

        start = datetime.strptime(start_date, '%Y-%m-%d')
        end = datetime.strptime(end_date, '%Y-%m-%d')

        current = start
        while current <= end:
            date_str = current.strftime('%Y-%m-%d')
            orders, items, current_order_id = self.generate_orders_for_day(date_str, current_order_id)
            all_orders.extend(orders)
            all_items.extend(items)
            current += timedelta(days=1)

        self.total_orders = len(all_orders)
        self.total_items = len(all_items)
        self.total_amount = sum(o['total_amount'] for o in all_orders)

        return all_orders, all_items

    def print_statistics(self):
        """打印统计信息"""
        print("\n" + "=" * 50)
        print("销售数据生成统计")
        print("=" * 50)
        print(f"总订单数: {self.total_orders:,}")
        print(f"总订单明细: {self.total_items:,}")
        print(f"总销售额: {float(self.total_amount):,.2f} 元")
        avg_order = float(self.total_amount) / self.total_orders if self.total_orders > 0 else 0
        print(f"平均客单价: {avg_order:.2f} 元")
        avg_items = self.total_items / self.total_orders if self.total_orders > 0 else 0
        print(f"平均每单商品数: {avg_items:.1f} 件")
        print("=" * 50)

    def print_daily_detail(self, date_str: str, orders: list, items: list):
        """打印某天的详细统计（用于验证）"""
        # 计算当天各商品实际销量
        product_sales = defaultdict(int)
        for item in items:
            if item['sale_date'] == date_str:
                product_sales[item['product_code']] += item['quantity']

        # 计算目标销量
        targets = self.calculate_daily_sales_target(date_str)

        print(f"\n{date_str} 销量验证:")
        print(f"{'商品编码':<10} {'商品名':<20} {'目标':>6} {'实际':>6} {'差异':>6}")
        print("-" * 50)

        total_target = 0
        total_actual = 0

        for code in sorted(targets.keys()):
            target = targets[code]
            actual = product_sales.get(code, 0)
            diff = actual - target
            total_target += target
            total_actual += actual

            product = self.products_by_code.get(code)
            name = product['name'][:18] if product else code

            print(f"{code:<10} {name:<20} {target:>6} {actual:>6} {diff:>+6}")

        print("-" * 50)
        print(f"合计: 目标 {total_target}件, 实际 {total_actual}件")

        # 订单统计
        day_orders = [o for o in orders if o['sale_date'] == date_str]
        if day_orders:
            day_amount = sum(float(o['total_amount']) for o in day_orders)
            print(f"订单数: {len(day_orders)}, 销售额: {day_amount:.2f}元")