# -*- coding: utf-8 -*-
"""
数据库连接模块
"""

import pymysql
from pymysql.cursors import DictCursor
from contextlib import contextmanager
from typing import List, Dict, Any, Optional

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'supermarket_forecasting_db',
    'charset': 'utf8mb4',
    'cursorclass': DictCursor
}


class DatabaseManager:
    """数据库管理器"""

    def __init__(self, config: dict = None):
        self.config = config or DB_CONFIG
        self.connection = None

    def connect(self):
        """建立连接"""
        if not self.connection:
            self.connection = pymysql.connect(**self.config)
        return self.connection

    def close(self):
        """关闭连接"""
        if self.connection:
            self.connection.close()
            self.connection = None

    @contextmanager
    def get_cursor(self):
        """获取游标上下文管理器"""
        conn = self.connect()
        cursor = conn.cursor()
        try:
            yield cursor
            conn.commit()
        except Exception as e:
            conn.rollback()
            raise e
        finally:
            cursor.close()

    def execute(self, sql: str, params: tuple = None) -> int:
        """执行单条SQL"""
        with self.get_cursor() as cursor:
            return cursor.execute(sql, params)

    def executemany(self, sql: str, params_list: List[tuple]) -> int:
        """批量执行SQL"""
        with self.get_cursor() as cursor:
            return cursor.executemany(sql, params_list)

    def query(self, sql: str, params: tuple = None) -> List[Dict[str, Any]]:
        """查询返回字典列表"""
        with self.get_cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchall()

    def query_one(self, sql: str, params: tuple = None) -> Optional[Dict[str, Any]]:
        """查询单条"""
        with self.get_cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchone()


def get_calendar_factors(start_date: str = None, end_date: str = None) -> List[Dict]:
    """
    获取日历因子数据

    Args:
        start_date: 开始日期 'YYYY-MM-DD'
        end_date: 结束日期 'YYYY-MM-DD'

    Returns:
        日历因子列表
    """
    db = DatabaseManager()
    sql = "SELECT * FROM calendar_factor WHERE 1=1"
    params = []

    if start_date:
        sql += " AND date >= %s"
        params.append(start_date)
    if end_date:
        sql += " AND date <= %s"
        params.append(end_date)

    sql += " ORDER BY date"

    result = db.query(sql, tuple(params) if params else None)
    db.close()
    return result


def get_products() -> List[Dict]:
    """
    获取所有商品信息

    Returns:
        商品列表
    """
    db = DatabaseManager()
    sql = """
        SELECT p.*, pc.name as category_name
        FROM product p
        LEFT JOIN product_category pc ON p.category_id = pc.id
        WHERE p.status = 1
        ORDER BY p.product_code
    """
    result = db.query(sql)
    db.close()
    return result


def clear_sales_data(db: DatabaseManager = None):
    """清空销售数据"""
    close_db = False
    if db is None:
        db = DatabaseManager()
        db.connect()
        close_db = True

    try:
        # 先删除明细，再删除主表（外键约束）
        db.execute("DELETE FROM sales_order_item")
        db.execute("DELETE FROM sales_order")
        print("已清空销售数据")
    finally:
        if close_db:
            db.close()


def insert_orders(db: DatabaseManager, orders: List[Dict]) -> int:
    """批量插入订单"""
    if not orders:
        return 0

    sql = """
        INSERT INTO sales_order
        (id, order_no, total_amount, total_quantity, payment_type, sale_date, sale_time, operator, remark)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
    """

    params_list = [(
        o['id'], o['order_no'], o['total_amount'], o['total_quantity'],
        o['payment_type'], o['sale_date'], o['sale_time'], o['operator'], o['remark']
    ) for o in orders]

    return db.executemany(sql, params_list)


def insert_order_items(db: DatabaseManager, items: List[Dict]) -> int:
    """批量插入订单明细"""
    if not items:
        return 0

    sql = """
        INSERT INTO sales_order_item
        (order_id, order_no, product_id, product_code, product_name, category_id, category_name,
         purchase_price, unit_price, quantity, subtotal_amount, subtotal_profit, is_promotion, sale_date)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """

    params_list = [(
        item['order_id'], item['order_no'], item['product_id'], item['product_code'],
        item['product_name'], item['category_id'], item['category_name'], item['purchase_price'],
        item['unit_price'], item['quantity'], item['subtotal_amount'], item['subtotal_profit'],
        item['is_promotion'], item['sale_date']
    ) for item in items]

    return db.executemany(sql, params_list)


def get_max_order_id() -> int:
    """获取最大订单ID"""
    db = DatabaseManager()
    result = db.query_one("SELECT MAX(id) as max_id FROM sales_order")
    db.close()
    return result['max_id'] if result and result['max_id'] else 0