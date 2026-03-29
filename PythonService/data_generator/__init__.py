# -*- coding: utf-8 -*-
"""
数据生成模块

提供销售数据模拟生成功能
"""

from .sales_generator import SalesDataGenerator
from .db_manager import (
    DatabaseManager,
    get_calendar_factors,
    get_products,
    clear_sales_data,
    insert_orders,
    insert_order_items,
    get_max_order_id
)

__all__ = [
    'SalesDataGenerator',
    'DatabaseManager',
    'get_calendar_factors',
    'get_products',
    'clear_sales_data',
    'insert_orders',
    'insert_order_items',
    'get_max_order_id'
]