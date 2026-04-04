# -*- coding: utf-8 -*-
"""
特征工程模块
从数据库提取销售数据，构造LightGBM训练所需的特征矩阵

核心功能：
1. 提取历史销量数据（用于训练）
2. 提取日历因子特征（星期、天气、节假日）
3. 构造滑动窗口特征（过去7天销量等）
"""

import sys
import os
from datetime import datetime, timedelta
from collections import defaultdict
import pandas as pd
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from data_generator.db_manager import DatabaseManager


class FeatureEngineer:
    """特征工程器"""

    # ==================== 配置参数 ====================

    # 基准日期（预测的起点）
    # 开发阶段可手动指定，生产阶段使用 datetime.now()
    BASE_DATE = "2026-04-01"  # TODO: 生产环境改为 datetime.now().strftime('%Y-%m-%d')

    # 训练数据天数（用多少历史数据训练）
    TRAINING_DAYS = 90  # 90天历史数据

    # 滑动窗口大小（用过去多少天的销量作为特征）
    WINDOW_SIZE = 7

    def __init__(self, db: DatabaseManager = None):
        """
        初始化特征工程器

        Args:
            db: 数据库管理器（可选，不传则自动创建）
        """
        self.db = db or DatabaseManager()
        self.db.connect()

        # 缓存数据
        self._sales_cache = None  # 销量数据缓存
        self._calendar_cache = None  # 日历因子缓存
        self._products_cache = None  # 商品信息缓存

    def close(self):
        """关闭数据库连接"""
        if self.db:
            self.db.close()

    # ==================== 数据加载 ====================

    def load_sales_data(self, start_date: str, end_date: str) -> pd.DataFrame:
        """
        加载指定日期范围的销量数据

        Args:
            start_date: 开始日期 'YYYY-MM-DD'
            end_date: 结束日期 'YYYY-MM-DD'

        Returns:
            DataFrame: columns=[date, product_code, product_id, product_name, category_id, quantity]
        """
        sql = """
            SELECT
                soi.sale_date as date,
                soi.product_code,
                soi.product_id,
                soi.product_name,
                p.category_id,
                SUM(soi.quantity) as quantity
            FROM sales_order_item soi
            LEFT JOIN product p ON soi.product_id = p.id
            WHERE soi.sale_date >= %s AND soi.sale_date <= %s
            GROUP BY soi.sale_date, soi.product_code, soi.product_id, soi.product_name, p.category_id
            ORDER BY soi.sale_date, soi.product_code
        """

        result = self.db.query(sql, (start_date, end_date))

        if not result:
            return pd.DataFrame()

        df = pd.DataFrame(result)
        df['date'] = pd.to_datetime(df['date'])
        df['quantity'] = df['quantity'].astype(int)
        # category_id 可能为空，填充为0
        df['category_id'] = df['category_id'].fillna(0).astype(int)

        return df

    def load_calendar_factors(self, start_date: str, end_date: str) -> pd.DataFrame:
        """
        加载日历因子数据

        Args:
            start_date: 开始日期
            end_date: 结束日期

        Returns:
            DataFrame: columns=[date, day_of_week, is_weekend, is_holiday, weather]
        """
        sql = """
            SELECT
                date,
                day_of_week,
                is_weekend,
                is_holiday,
                holiday_name,
                weather
            FROM calendar_factor
            WHERE date >= %s AND date <= %s
            ORDER BY date
        """

        result = self.db.query(sql, (start_date, end_date))

        if not result:
            return pd.DataFrame()

        df = pd.DataFrame(result)
        df['date'] = pd.to_datetime(df['date'])

        return df

    def load_products(self) -> pd.DataFrame:
        """
        加载商品信息

        Returns:
            DataFrame: columns=[product_id, product_code, name, category_id, category_name]
        """
        sql = """
            SELECT
                p.id as product_id,
                p.product_code,
                p.name,
                p.category_id,
                pc.name as category_name
            FROM product p
            LEFT JOIN product_category pc ON p.category_id = pc.id
            WHERE p.status = 1
            ORDER BY p.product_code
        """

        result = self.db.query(sql)

        if not result:
            return pd.DataFrame()

        return pd.DataFrame(result)

    # ==================== 特征构造 ====================

    def _fill_missing_dates(self, df: pd.DataFrame, start_date: str, end_date: str) -> pd.DataFrame:
        """
        填充缺失的日期（核心修复：解决时间序列断层问题）

        当某天某商品销量为0时，GROUP BY查询不会有这条记录。
        这会导致iloc取到的不是"过去7天"，而是"过去7条有销量的记录"。
        必须生成连续日期序列，将缺失日期的销量填充为0。

        Args:
            df: 原始销量数据，columns=[date, product_code, product_id, product_name, category_id, quantity]
            start_date: 开始日期 'YYYY-MM-DD'
            end_date: 结束日期 'YYYY-MM-DD'

        Returns:
            填充后的DataFrame，每个商品每天都有记录
        """
        if df.empty:
            return df

        # 生成完整的日期序列
        date_range = pd.date_range(start=start_date, end=end_date, freq='D')

        # 获取所有商品信息（包含 category_id）
        products = df[['product_code', 'product_id', 'product_name', 'category_id']].drop_duplicates()

        # 生成 商品×日期 的笛卡尔积
        from itertools import product as cartesian_product
        product_date_combinations = pd.DataFrame(
            list(cartesian_product(products['product_code'], date_range)),
            columns=['product_code', 'date']
        )

        # 合并商品信息
        product_date_combinations = product_date_combinations.merge(
            products, on='product_code', how='left'
        )

        # 合并销量数据
        filled_df = product_date_combinations.merge(
            df[['date', 'product_code', 'quantity']],
            on=['date', 'product_code'],
            how='left'
        )

        # 缺失的销量填充为0
        filled_df['quantity'] = filled_df['quantity'].fillna(0).astype(int)

        # 按日期和商品排序
        filled_df = filled_df.sort_values(['product_code', 'date']).reset_index(drop=True)

        return filled_df

    def build_training_features(self) -> tuple:
        """
        构造训练用的特征矩阵

        训练逻辑：
        - 使用 BASE_DATE 前 TRAINING_DAYS 天的数据作为训练集
        - 目标是预测"第1天"的销量
        - 特征包括：星期、天气、节假日、过去7天销量等

        Returns:
            (X, y, product_codes, dates)
            X: 特征矩阵 DataFrame
            y: 目标销量 Series
            product_codes: 商品编码列表
            dates: 日期列表
        """
        # 计算训练数据范围
        base_dt = datetime.strptime(self.BASE_DATE, '%Y-%m-%d')
        train_end = base_dt - timedelta(days=1)  # 训练截止到基准日期前一天
        train_start = train_end - timedelta(days=self.TRAINING_DAYS)

        train_start_str = train_start.strftime('%Y-%m-%d')
        train_end_str = train_end.strftime('%Y-%m-%d')

        print(f"[特征工程] 训练数据范围: {train_start_str} ~ {train_end_str}")

        # 加载数据
        sales_df = self.load_sales_data(train_start_str, train_end_str)
        calendar_df = self.load_calendar_factors(train_start_str, train_end_str)
        products_df = self.load_products()

        if sales_df.empty or calendar_df.empty:
            print("[警告] 数据加载失败，返回空数据")
            return None, None, None, None

        # ========== 核心修复：填充缺失日期 ==========
        # 某天某商品销量为0时，GROUP BY不会返回该记录
        # 必须填充缺失日期，否则滑动窗口取到的不是"过去7天"
        print(f"[特征工程] 原始销量记录数: {len(sales_df)}")
        sales_df = self._fill_missing_dates(sales_df, train_start_str, train_end_str)
        print(f"[特征工程] 填充后记录数: {len(sales_df)} (每个商品每天都有记录)")

        # 合并日历因子
        df = sales_df.merge(calendar_df, on='date', how='left')

        # 构造滑动窗口特征
        feature_rows = []

        # 获取所有商品编码
        product_codes = df['product_code'].unique()

        # 按商品分组处理
        for pcode in product_codes:
            product_data = df[df['product_code'] == pcode].sort_values('date')

            if len(product_data) < self.WINDOW_SIZE + 1:
                # 数据太少，跳过
                continue

            # 遍历每一天，构造特征
            dates = product_data['date'].values

            for i in range(self.WINDOW_SIZE, len(product_data)):
                current_row = product_data.iloc[i]
                current_date = current_row['date']

                # 目标：当天的销量
                target = current_row['quantity']

                # 过去7天销量
                past_7_days = product_data.iloc[i-self.WINDOW_SIZE:i]['quantity'].values

                # 构造特征
                features = {
                    'product_code': pcode,
                    'date': current_date,
                    # ========== 商品身份特征（核心修复：让模型识别不同商品）==========
                    'product_id': current_row['product_id'],
                    'category_id': current_row['category_id'],
                    # 日历特征
                    'day_of_week': current_row['day_of_week'],
                    'is_weekend': current_row['is_weekend'],
                    'is_holiday': current_row['is_holiday'],
                    # 天气特征（编码）
                    'weather_sunny': 1 if current_row['weather'] == '晴' else 0,
                    'weather_cloudy': 1 if current_row['weather'] == '多云' else 0,
                    'weather_rain': 1 if current_row['weather'] in ['小雨', '大雨'] else 0,
                    'weather_hot': 1 if current_row['weather'] == '炎热' else 0,
                    # 滑动窗口特征
                    'sales_1d_ago': past_7_days[-1],  # 昨天
                    'sales_2d_ago': past_7_days[-2],  # 前天
                    'sales_3d_ago': past_7_days[-3],
                    'sales_7d_ago': past_7_days[-7] if len(past_7_days) >= 7 else 0,
                    'sales_7d_avg': np.mean(past_7_days),
                    'sales_7d_max': np.max(past_7_days),
                    'sales_7d_min': np.min(past_7_days),
                    # 目标
                    'target': target
                }

                feature_rows.append(features)

        if not feature_rows:
            print("[警告] 特征构造失败，无有效数据")
            return None, None, None, None

        # 转为DataFrame
        feature_df = pd.DataFrame(feature_rows)

        # 分离特征和目标（product_id 和 category_id 作为类别特征）
        feature_cols = [
            'product_id', 'category_id',  # 商品身份特征（类别特征）
            'day_of_week', 'is_weekend', 'is_holiday',
            'weather_sunny', 'weather_cloudy', 'weather_rain', 'weather_hot',
            'sales_1d_ago', 'sales_2d_ago', 'sales_3d_ago',
            'sales_7d_ago', 'sales_7d_avg', 'sales_7d_max', 'sales_7d_min'
        ]

        X = feature_df[feature_cols]
        y = feature_df['target']
        product_codes = feature_df['product_code'].values
        dates = feature_df['date'].values

        print(f"[特征工程] 构造完成: {len(feature_df)} 条训练样本")
        print(f"[特征工程] 特征列: {feature_cols}")

        return X, y, product_codes, dates

    def build_prediction_features(self, forecast_start: str, forecast_days: int = 7) -> dict:
        """
        构造预测用的特征矩阵

        Args:
            forecast_start: 预测起始日期 'YYYY-MM-DD'
            forecast_days: 预测天数（默认7天）

        Returns:
            {
                product_code: [
                    {'date': date1, 'features': {...}},
                    {'date': date2, 'features': {...}},
                    ...
                ]
            }
        """
        # 加载商品列表
        products_df = self.load_products()

        if products_df.empty:
            print("[警告] 无商品数据")
            return {}

        # 加载日历因子（预测日期范围）
        start_dt = datetime.strptime(forecast_start, '%Y-%m-%d')
        end_dt = start_dt + timedelta(days=forecast_days - 1)
        end_date_str = end_dt.strftime('%Y-%m-%d')

        calendar_df = self.load_calendar_factors(forecast_start, end_date_str)

        if calendar_df.empty:
            print(f"[警告] 无日历因子数据: {forecast_start} ~ {end_date_str}")
            return {}

        # 加载历史销量（用于滑动窗口特征）
        hist_start = start_dt - timedelta(days=self.WINDOW_SIZE)
        hist_start_str = hist_start.strftime('%Y-%m-%d')
        hist_end_str = (start_dt - timedelta(days=1)).strftime('%Y-%m-%d')

        sales_df = self.load_sales_data(hist_start_str, hist_end_str)

        # ========== 核心修复：填充缺失日期 ==========
        # 确保每个商品在过去7天都有记录，缺失的日期填充为0
        if not sales_df.empty:
            sales_df = self._fill_missing_dates(sales_df, hist_start_str, hist_end_str)

        # 按商品构造预测特征
        prediction_features = {}

        for _, product in products_df.iterrows():
            pcode = product['product_code']

            # 获取该商品的历史销量
            product_sales = sales_df[sales_df['product_code'] == pcode].sort_values('date')

            # 初始化滑动窗口（过去7天销量）
            if len(product_sales) >= self.WINDOW_SIZE:
                past_sales = product_sales.iloc[-self.WINDOW_SIZE:]['quantity'].values.tolist()
            elif len(product_sales) > 0:
                past_sales = list(product_sales['quantity'].values)
                # 补齐到7天
                while len(past_sales) < self.WINDOW_SIZE:
                    past_sales.insert(0, 0)
            else:
                # 无历史数据，使用默认值
                past_sales = [0] * self.WINDOW_SIZE

            # 为每个预测日期构造特征
            product_features = []

            for i in range(forecast_days):
                forecast_date = start_dt + timedelta(days=i)
                forecast_date_str = forecast_date.strftime('%Y-%m-%d')

                # 获取该天的日历因子
                cal_row = calendar_df[calendar_df['date'] == forecast_date_str]

                if cal_row.empty:
                    # 无日历因子，使用默认值
                    day_of_week = forecast_date.weekday() + 1  # 周一=1
                    is_weekend = 1 if day_of_week in [6, 7] else 0
                    is_holiday = 0
                    weather = '晴'
                else:
                    cal_row = cal_row.iloc[0]
                    day_of_week = cal_row['day_of_week']
                    is_weekend = cal_row['is_weekend']
                    is_holiday = cal_row['is_holiday']
                    weather = cal_row['weather'] or '晴'

                # 构造特征
                features = {
                    'date': forecast_date_str,
                    # ========== 商品身份特征 ==========
                    'product_id': product['product_id'],
                    'category_id': product['category_id'],
                    # 日历特征
                    'day_of_week': day_of_week,
                    'is_weekend': is_weekend,
                    'is_holiday': is_holiday,
                    'weather_sunny': 1 if weather == '晴' else 0,
                    'weather_cloudy': 1 if weather == '多云' else 0,
                    'weather_rain': 1 if weather in ['小雨', '大雨'] else 0,
                    'weather_hot': 1 if weather == '炎热' else 0,
                    # 滑动窗口特征
                    'sales_1d_ago': past_sales[-1],
                    'sales_2d_ago': past_sales[-2],
                    'sales_3d_ago': past_sales[-3],
                    'sales_7d_ago': past_sales[0],
                    'sales_7d_avg': np.mean(past_sales),
                    'sales_7d_max': np.max(past_sales),
                    'sales_7d_min': np.min(past_sales),
                }

                product_features.append(features)

            prediction_features[pcode] = {
                'product_id': product['product_id'],
                'product_name': product['name'],
                'category_id': product['category_id'],
                'category_name': product['category_name'],
                'features': product_features,
                'initial_past_sales': past_sales.copy()  # 用于滚动预测时更新
            }

        print(f"[特征工程] 为 {len(prediction_features)} 个商品构造了预测特征")

        return prediction_features

    # ==================== 工具方法 ====================

    def get_base_date(self) -> str:
        """获取基准日期"""
        return self.BASE_DATE

    def set_base_date(self, date_str: str):
        """
        设置基准日期（用于测试或模拟）

        Args:
            date_str: 日期 'YYYY-MM-DD'
        """
        self.BASE_DATE = date_str
        print(f"[特征工程] 基准日期已设置为: {date_str}")


# ==================== 测试代码 ====================

if __name__ == '__main__':
    print("=" * 60)
    print("特征工程模块测试")
    print("=" * 60)

    fe = FeatureEngineer()

    try:
        # 测试1：构造训练特征
        print("\n【测试1】构造训练特征")
        X, y, product_codes, dates = fe.build_training_features()

        if X is not None:
            print(f"特征矩阵形状: {X.shape}")
            print(f"目标形状: {y.shape}")
            print(f"商品数量: {len(set(product_codes))}")
            print("\n特征示例（前3行）:")
            print(X.head(3))
            print("\n目标示例（前10个）:")
            print(y.head(10).values)

        # 测试2：构造预测特征
        print("\n【测试2】构造预测特征")
        pred_features = fe.build_prediction_features(fe.BASE_DATE, forecast_days=7)

        if pred_features:
            # 查看第一个商品的特征
            first_pcode = list(pred_features.keys())[0]
            first_product = pred_features[first_pcode]
            print(f"\n商品示例: {first_pcode} - {first_product['product_name']}")
            print(f"预测特征数量: {len(first_product['features'])}")
            print("\n第一天预测特征:")
            print(first_product['features'][0])

    finally:
        fe.close()

    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)