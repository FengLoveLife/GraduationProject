# -*- coding: utf-8 -*-
"""
滚动预测模块
使用单日预测模型，循环调用实现多日预测

核心功能：
1. 滚动预测未来1/3/7天销量
2. 预测结果写入数据库
3. 支持批量预测全部商品
"""

import sys
import os
from datetime import datetime, timedelta
import numpy as np
import pandas as pd

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from forecast.feature_engineering import FeatureEngineer
from forecast.model_trainer import SalesPredictor
from data_generator.db_manager import DatabaseManager


class RollingPredictor:
    """滚动预测器"""

    def __init__(self, predictor: SalesPredictor = None, fe: FeatureEngineer = None, connect_db: bool = True):
        """
        初始化滚动预测器

        Args:
            predictor: 销量预测模型（可选，不传则自动加载）
            fe: 特征工程器（可选，不传则自动创建）
            connect_db: 是否立即连接数据库（默认True，API服务应设为False）
        """
        self.predictor = predictor or SalesPredictor()
        self.fe = fe
        self.db = None

        # 确保模型已加载
        if self.predictor.model is None:
            model_path = os.path.join(
                os.path.dirname(__file__), 'model', 'sales_forecast_model.pkl'
            )
            if not self.predictor.load_model(model_path):
                raise ValueError("模型加载失败，请先运行 model_trainer.py 训练模型")

        # 按需连接数据库（核心修复：避免长期持有连接）
        if connect_db:
            self.fe = fe or FeatureEngineer()
            self.db = DatabaseManager()
            self.db.connect()

    @classmethod
    def load_model_only(cls):
        """
        只加载模型，不连接数据库（用于API服务）

        返回一个只持有模型的预测器实例，数据库连接在需要时才创建。
        这解决了 FastAPI 全局单例长期持有数据库连接导致超时的问题。
        """
        predictor = SalesPredictor()
        model_path = os.path.join(
            os.path.dirname(__file__), 'model', 'sales_forecast_model.pkl'
        )
        if not predictor.load_model(model_path):
            raise ValueError("模型加载失败，请先运行 model_trainer.py 训练模型")

        # 创建不连接数据库的实例
        instance = cls.__new__(cls)
        instance.predictor = predictor
        instance.fe = None
        instance.db = None
        return instance

    def _ensure_db_connection(self):
        """确保数据库已连接（延迟连接）"""
        if self.db is None:
            self.db = DatabaseManager()
            self.db.connect()
        if self.fe is None:
            self.fe = FeatureEngineer()

    def close(self):
        """关闭资源"""
        if self.fe:
            self.fe.close()
        if self.db:
            self.db.close()

    # ==================== 滚动预测核心 ====================

    def predict_rolling_for_product(
        self,
        product_data: dict,
        forecast_days: int = 7
    ) -> list:
        """
        滚动预测单个商品未来多天销量（使用预加载的特征）

        Args:
            product_data: 商品预测特征数据（来自 build_prediction_features）
            forecast_days: 预测天数

        Returns:
            [{'date': '2026-04-01', 'predicted': 25}, ...]
        """
        features_list = product_data['features']
        initial_past_sales = product_data['initial_past_sales']

        # 获取商品身份特征
        product_id = product_data['product_id']
        category_id = product_data['category_id']

        # 滚动预测
        predictions = []
        past_sales = list(initial_past_sales)  # 复制一份用于滚动更新

        for day_idx, day_features in enumerate(features_list):
            # 构造当前预测特征（使用最新的past_sales）
            current_features = {
                # ========== 商品身份特征 ==========
                'product_id': product_id,
                'category_id': category_id,
                # 日历特征
                'day_of_week': day_features['day_of_week'],
                'is_weekend': day_features['is_weekend'],
                'is_holiday': day_features['is_holiday'],
                'weather_sunny': day_features['weather_sunny'],
                'weather_cloudy': day_features['weather_cloudy'],
                'weather_rain': day_features['weather_rain'],
                'weather_hot': day_features['weather_hot'],
                # 使用滚动更新的past_sales
                'sales_1d_ago': past_sales[-1],
                'sales_2d_ago': past_sales[-2],
                'sales_3d_ago': past_sales[-3],
                'sales_7d_ago': past_sales[0],
                'sales_7d_avg': np.mean(past_sales),
                'sales_7d_max': np.max(past_sales),
                'sales_7d_min': np.min(past_sales),
            }

            # 预测当天销量
            predicted_qty = self.predictor.predict_single_day(current_features)

            predictions.append({
                'date': day_features['date'],
                'predicted': predicted_qty
            })

            # 更新滑动窗口（滚动预测的关键）
            # 将预测值加入历史，用于预测下一天
            past_sales.append(predicted_qty)
            past_sales.pop(0)  # 移除最老的一天

        return predictions

    def predict_all_products(self, forecast_start: str, forecast_days: int = 7) -> dict:
        """
        批量预测所有商品

        Args:
            forecast_start: 预测起始日期
            forecast_days: 预测天数

        Returns:
            {
                product_code: {
                    'product_id': xxx,
                    'product_name': xxx,
                    'category_id': xxx,
                    'category_name': xxx,
                    'predictions': [{'date': xxx, 'predicted': xxx}, ...]
                }
            }
        """
        # 确保数据库已连接（延迟连接）
        self._ensure_db_connection()

        print(f"\n[滚动预测] 开始预测所有商品: {forecast_start} 起 {forecast_days} 天")

        # 一次性获取所有商品的预测特征
        all_features = self.fe.build_prediction_features(forecast_start, forecast_days)

        if not all_features:
            print("[警告] 无预测特征数据")
            return {}

        results = {}
        total = len(all_features)

        for idx, (product_code, product_data) in enumerate(all_features.items(), 1):
            # 滚动预测
            predictions = self.predict_rolling_for_product(product_data, forecast_days)

            results[product_code] = {
                'product_id': product_data['product_id'],
                'product_name': product_data['product_name'],
                'category_id': product_data['category_id'],
                'category_name': product_data['category_name'],
                'predictions': predictions
            }

            # 进度显示
            if idx % 20 == 0 or idx == total:
                print(f"  进度: {idx}/{total} ({idx * 100 // total}%)")

        print(f"[滚动预测] 完成，共预测 {len(results)} 个商品")

        return results

    # ==================== 数据库写入 ====================

    def save_predictions_to_db(self, results: dict, clear_existing: bool = False):
        """
        将预测结果写入数据库

        Args:
            results: 预测结果字典
            clear_existing: 是否清空现有预测结果（默认False，保留历史数据）

        核心修复：
        - 使用 INSERT ... ON DUPLICATE KEY UPDATE 语法
        - 利用唯一索引 uk_product_date (product_id, forecast_date)
        - 相同商品相同日期的预测会被更新，历史预测数据保留
        """
        if not results:
            print("[警告] 无预测结果，跳过写入")
            return

        # 确保数据库已连接（延迟连接）
        self._ensure_db_connection()

        print(f"\n[数据库写入] 开始写入预测结果")

        # 清空现有数据（可选，默认不清空）
        if clear_existing:
            self.db.execute("DELETE FROM forecast_result")
            print("  已清空现有预测结果")
        else:
            print("  保留历史预测数据，使用 UPSERT 模式")

        # 构造批量插入数据
        insert_data = []

        for product_code, data in results.items():
            product_id = data['product_id']
            product_name = data['product_name']
            category_id = data['category_id']
            category_name = data['category_name']

            for pred in data['predictions']:
                insert_data.append({
                    'forecast_date': pred['date'],
                    'product_id': product_id,
                    'product_code': product_code,
                    'product_name': product_name,
                    'category_id': category_id,
                    'category_name': category_name,
                    'predicted_quantity': pred['predicted'],
                })

        # 批量插入（使用 ON DUPLICATE KEY UPDATE 实现 UPSERT）
        if insert_data:
            sql = """
                INSERT INTO forecast_result
                (forecast_date, product_id, product_code, product_name,
                 category_id, category_name, predicted_quantity)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    product_code = VALUES(product_code),
                    product_name = VALUES(product_name),
                    category_id = VALUES(category_id),
                    category_name = VALUES(category_name),
                    predicted_quantity = VALUES(predicted_quantity),
                    update_time = NOW()
            """

            params_list = [
                (
                    d['forecast_date'], d['product_id'], d['product_code'],
                    d['product_name'], d['category_id'], d['category_name'],
                    d['predicted_quantity']
                )
                for d in insert_data
            ]

            self.db.executemany(sql, params_list)
            print(f"  已写入 {len(insert_data)} 条预测记录")

        # 验证写入结果
        result = self.db.query_one("SELECT COUNT(*) as cnt FROM forecast_result")
        db_count = result['cnt'] if result else 0
        print(f"  数据库总记录数: {db_count}")

    # ==================== 查询功能 ====================

    def get_predictions_from_db(
        self,
        forecast_date: str = None,
        product_id: int = None,
        category_id: int = None,
        days: int = 7
    ) -> list:
        """
        从数据库查询预测结果

        Args:
            forecast_date: 预测日期（可选，默认从最新销售日期开始）
            product_id: 商品ID（可选）
            category_id: 分类ID（可选）
            days: 查询天数

        Returns:
            预测结果列表
        """
        # 确保数据库已连接（延迟连接）
        self._ensure_db_connection()

        # 核心修复：默认日期应该是数据库中销售数据的最后一天
        # 而不是 datetime.now()，避免时空错乱
        if forecast_date is None:
            result = self.db.query_one("SELECT MAX(sale_date) as max_date FROM sales_order_item")
            if result and result['max_date']:
                forecast_date = str(result['max_date'])
            else:
                # 如果没有销售数据，才使用今天
                forecast_date = datetime.now().strftime('%Y-%m-%d')

        # 计算查询日期范围
        start_dt = datetime.strptime(forecast_date, '%Y-%m-%d')
        end_dt = start_dt + timedelta(days=days - 1)
        end_date = end_dt.strftime('%Y-%m-%d')

        # 构造查询SQL
        sql = """
            SELECT * FROM forecast_result
            WHERE forecast_date >= %s AND forecast_date <= %s
        """
        params = [forecast_date, end_date]

        if product_id:
            sql += " AND product_id = %s"
            params.append(product_id)

        if category_id:
            sql += " AND category_id = %s"
            params.append(category_id)

        sql += " ORDER BY category_name, product_name, forecast_date"

        result = self.db.query(sql, tuple(params))
        return result if result else []

    # ==================== 累计销量计算 ====================

    def calculate_cumulative_sales(self, predictions: list, target_days: int) -> int:
        """
        计算累计销量（用于1/3/7天累计）

        Args:
            predictions: 预测列表 [{'date': xxx, 'predicted': xxx}, ...]
            target_days: 目标天数 1/3/7

        Returns:
            累计销量
        """
        if not predictions:
            return 0

        # 取前N天的预测值求和
        daily_preds = [p['predicted'] for p in predictions[:target_days]]
        return sum(daily_preds)


# ==================== 一键预测函数 ====================

def run_full_prediction(base_date: str = None, forecast_days: int = 7, save_to_db: bool = True):
    """
    一键运行完整预测流程

    Args:
        base_date: 基准日期（预测起始日期）
        forecast_days: 预测天数
        save_to_db: 是否保存到数据库

    Returns:
        预测结果字典
    """
    # 设置基准日期
    if base_date:
        fe = FeatureEngineer()
        fe.set_base_date(base_date)
        forecast_start = base_date
        fe.close()
    else:
        forecast_start = datetime.now().strftime('%Y-%m-%d')

    print("=" * 60)
    print("销量预测系统 - 滚动预测")
    print("=" * 60)
    print(f"预测起始日期: {forecast_start}")
    print(f"预测天数: {forecast_days} 天")
    print("=" * 60)

    # 创建预测器
    predictor = RollingPredictor()

    try:
        # 执行预测
        results = predictor.predict_all_products(forecast_start, forecast_days)

        # 保存到数据库
        if save_to_db and results:
            predictor.save_predictions_to_db(results)

        # 打印统计
        print("\n" + "=" * 60)
        print("预测统计")
        print("=" * 60)

        total_predictions = sum(len(r['predictions']) for r in results.values())
        print(f"商品数量: {len(results)}")
        print(f"预测记录数: {total_predictions}")

        # 显示示例
        if results:
            first_code = list(results.keys())[0]
            first_product = results[first_code]
            print(f"\n示例商品: {first_code} - {first_product['product_name']}")
            for pred in first_product['predictions'][:3]:
                print(f"  {pred['date']}: 预测销量 {pred['predicted']}")

        return results

    finally:
        predictor.close()


# ==================== 测试代码 ====================

if __name__ == '__main__':
    print("=" * 60)
    print("滚动预测模块测试")
    print("=" * 60)

    # 运行预测
    results = run_full_prediction(base_date='2026-04-01', forecast_days=7, save_to_db=True)

    if results:
        print("\n" + "=" * 60)
        print("测试完成")
        print("=" * 60)