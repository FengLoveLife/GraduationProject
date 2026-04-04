# -*- coding: utf-8 -*-
"""
模型训练模块
使用LightGBM训练销量预测模型

核心功能：
1. 训练单日预测模型
2. 模型保存与加载
3. 模型评估（MAE、RMSE等）
"""

import sys
import os
import joblib
from datetime import datetime
import numpy as np
import pandas as pd
import lightgbm as lgb
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from forecast.feature_engineering import FeatureEngineer


class SalesPredictor:
    """销量预测模型训练器"""

    # 模型保存路径
    MODEL_DIR = os.path.join(os.path.dirname(__file__), 'model')
    MODEL_FILE = 'sales_forecast_model.pkl'

    # LightGBM 模型参数
    MODEL_PARAMS = {
        'objective': 'regression',  # 回归任务
        'metric': 'mae',            # 评估指标：平均绝对误差
        'boosting_type': 'gbdt',    # 梯度提升决策树
        'num_leaves': 31,           # 叶子节点数
        'learning_rate': 0.05,      # 学习率
        'feature_fraction': 0.8,    # 特征采样比例
        'bagging_fraction': 0.8,    # 数据采样比例
        'bagging_freq': 5,          # 采样频率
        'verbose': -1,              # 不输出训练日志
        'n_jobs': -1,               # 使用所有CPU核心
    }

    # 训练参数
    TRAIN_PARAMS = {
        'num_boost_round': 500,     # 最大迭代次数
        'early_stopping_rounds': 50,  # 早停轮数
    }

    # 早停回调
    EARLY_STOPPING_CALLBACKS = None  # 将在训练时动态创建

    def __init__(self):
        """初始化训练器"""
        self.model = None
        self.feature_cols = None
        self.training_stats = None

        # 确保模型目录存在
        os.makedirs(self.MODEL_DIR, exist_ok=True)

    # ==================== 模型训练 ====================

    def train(self, X: pd.DataFrame, y: pd.Series, valid_ratio: float = 0.2) -> dict:
        """
        训练模型

        Args:
            X: 特征矩阵
            y: 目标销量
            valid_ratio: 验证集比例

        Returns:
            训练统计信息
        """
        print("\n" + "=" * 50)
        print("开始训练 LightGBM 模型")
        print("=" * 50)

        # 记录特征列
        self.feature_cols = list(X.columns)

        # 划分训练集和验证集
        X_train, X_valid, y_train, y_valid = train_test_split(
            X, y, test_size=valid_ratio, random_state=42
        )

        print(f"训练集大小: {len(X_train)}")
        print(f"验证集大小: {len(X_valid)}")

        # ========== 核心修复：声明类别特征 ==========
        # product_id 和 category_id 是类别特征，需要告诉 LightGBM
        # LightGBM 会自动处理类别特征，无需手动编码
        categorical_features = ['product_id', 'category_id']
        print(f"类别特征: {categorical_features}")

        # ========== 优化：显式转换为 category 类型 ==========
        # 避免 LightGBM 内部的类型推断开销，提升训练速度约10%
        X_train['product_id'] = X_train['product_id'].astype('category')
        X_train['category_id'] = X_train['category_id'].astype('category')
        X_valid['product_id'] = X_valid['product_id'].astype('category')
        X_valid['category_id'] = X_valid['category_id'].astype('category')

        # 创建 LightGBM 数据集
        train_data = lgb.Dataset(X_train, label=y_train, categorical_feature=categorical_features)
        valid_data = lgb.Dataset(X_valid, label=y_valid, reference=train_data, categorical_feature=categorical_features)

        # 训练模型
        print("\n训练中...")
        self.model = lgb.train(
            self.MODEL_PARAMS,
            train_data,
            valid_sets=[valid_data],
            valid_names=['valid'],
            num_boost_round=self.TRAIN_PARAMS['num_boost_round'],
            callbacks=[lgb.early_stopping(self.TRAIN_PARAMS['early_stopping_rounds'], verbose=False)]
        )

        # 评估模型
        y_pred = self.model.predict(X_valid, num_iteration=self.model.best_iteration)

        mae = mean_absolute_error(y_valid, y_pred)
        rmse = np.sqrt(mean_squared_error(y_valid, y_pred))
        r2 = r2_score(y_valid, y_pred)

        # 计算平均百分比误差（MAPE，对销量预测更有意义）
        # 避免0值导致除零错误
        y_valid_nonzero = y_valid[y_valid > 0]
        y_pred_nonzero = y_pred[y_valid > 0]
        mape = np.mean(np.abs((y_valid_nonzero - y_pred_nonzero) / y_valid_nonzero)) * 100 if len(y_valid_nonzero) > 0 else 0

        self.training_stats = {
            'best_iteration': self.model.best_iteration,
            'mae': mae,
            'rmse': rmse,
            'r2': r2,
            'mape': mape,
            'train_size': len(X_train),
            'valid_size': len(X_valid),
            'feature_count': len(self.feature_cols),
            'training_time': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        }

        print("\n" + "=" * 50)
        print("训练完成")
        print("=" * 50)
        print(f"最佳迭代轮数: {self.model.best_iteration}")
        print(f"MAE (平均绝对误差): {mae:.2f}")
        print(f"RMSE (均方根误差): {rmse:.2f}")
        print(f"R² (拟合优度): {r2:.4f}")
        print(f"MAPE (平均百分比误差): {mape:.2f}%")
        print("=" * 50)

        return self.training_stats

    # ==================== 模型保存与加载 ====================

    def save_model(self, filepath: str = None) -> str:
        """
        保存模型到文件

        Args:
            filepath: 保存路径（可选，默认使用 MODEL_FILE）

        Returns:
            实际保存路径
        """
        if self.model is None:
            raise ValueError("模型尚未训练，无法保存")

        filepath = filepath or os.path.join(self.MODEL_DIR, self.MODEL_FILE)

        # 保存模型和元数据
        save_data = {
            'model': self.model,
            'feature_cols': self.feature_cols,
            'training_stats': self.training_stats,
            'model_params': self.MODEL_PARAMS,
        }

        joblib.dump(save_data, filepath)
        print(f"\n[模型保存] 已保存到: {filepath}")

        return filepath

    def load_model(self, filepath: str = None) -> bool:
        """
        从文件加载模型

        Args:
            filepath: 模型文件路径（可选，默认使用 MODEL_FILE）

        Returns:
            是否加载成功
        """
        filepath = filepath or os.path.join(self.MODEL_DIR, self.MODEL_FILE)

        if not os.path.exists(filepath):
            print(f"[警告] 模型文件不存在: {filepath}")
            return False

        try:
            save_data = joblib.load(filepath)
            self.model = save_data['model']
            self.feature_cols = save_data['feature_cols']
            self.training_stats = save_data.get('training_stats', {})
            self.MODEL_PARAMS = save_data.get('model_params', self.MODEL_PARAMS)

            print(f"\n[模型加载] 已加载: {filepath}")
            if self.training_stats:
                print(f"  训练时间: {self.training_stats.get('training_time', 'N/A')}")
                print(f"  MAE: {self.training_stats.get('mae', 'N/A'):.2f}")
                print(f"  MAPE: {self.training_stats.get('mape', 'N/A'):.2f}%")

            return True

        except Exception as e:
            print(f"[错误] 模型加载失败: {e}")
            return False

    # ==================== 预测功能 ====================

    def predict_single_day(self, features: dict) -> int:
        """
        预测单日销量

        Args:
            features: 特征字典（需包含所有 feature_cols）

        Returns:
            预测销量（整数）
        """
        if self.model is None:
            raise ValueError("模型尚未加载")

        # 构造特征向量（按 feature_cols 顺序）
        X = np.array([[features.get(col, 0) for col in self.feature_cols]])

        # 预测
        pred = self.model.predict(X, num_iteration=self.model.best_iteration)[0]

        # 返回整数销量（销量必须是正整数）
        return max(0, int(round(pred)))

    def predict_features_df(self, X: pd.DataFrame) -> np.ndarray:
        """
        批量预测

        Args:
            X: 特征矩阵 DataFrame

        Returns:
            预测结果数组
        """
        if self.model is None:
            raise ValueError("模型尚未加载")

        preds = self.model.predict(X, num_iteration=self.model.best_iteration)

        # 转为正整数
        return np.array([max(0, int(round(p))) for p in preds])

    # ==================== 特征重要性 ====================

    def get_feature_importance(self) -> pd.DataFrame:
        """
        获取特征重要性排名

        Returns:
            DataFrame: columns=[feature, importance]
        """
        if self.model is None:
            raise ValueError("模型尚未加载")

        importance = self.model.feature_importance(importance_type='gain')

        df = pd.DataFrame({
            'feature': self.feature_cols,
            'importance': importance
        })

        df = df.sort_values('importance', ascending=False)

        return df

    def print_feature_importance(self, top_n: int = 10):
        """打印特征重要性排名"""
        df = self.get_feature_importance()

        print("\n" + "=" * 50)
        print(f"特征重要性排名 (Top {top_n})")
        print("=" * 50)

        for i, row in df.head(top_n).iterrows():
            print(f"{row['feature']:<20} {row['importance']:>10.2f}")

        print("=" * 50)


# ==================== 便捷训练函数 ====================

def train_and_save_model(base_date: str = None) -> dict:
    """
    一键训练并保存模型

    Args:
        base_date: 基准日期（可选，默认使用 FeatureEngineer.BASE_DATE）

    Returns:
        训练统计信息
    """
    # 特征工程
    fe = FeatureEngineer()

    if base_date:
        fe.set_base_date(base_date)

    try:
        # 构造训练数据
        X, y, product_codes, dates = fe.build_training_features()

        if X is None:
            print("[错误] 无法构造训练数据")
            return None

        # 训练模型
        predictor = SalesPredictor()
        stats = predictor.train(X, y)

        # 打印特征重要性
        predictor.print_feature_importance()

        # 保存模型
        predictor.save_model()

        return stats

    finally:
        fe.close()


# ==================== 测试代码 ====================

if __name__ == '__main__':
    print("=" * 60)
    print("模型训练模块测试")
    print("=" * 60)

    # 训练模型
    stats = train_and_save_model()

    if stats:
        print("\n训练统计:")
        for key, value in stats.items():
            print(f"  {key}: {value}")

        # 测试加载
        print("\n【测试】加载模型")
        predictor = SalesPredictor()
        if predictor.load_model():
            print("模型加载成功!")

            # 测试单日预测
            print("\n【测试】单日预测")
            test_features = {
                'day_of_week': 3,
                'is_weekend': 0,
                'is_holiday': 0,
                'weather_sunny': 0,
                'weather_cloudy': 1,
                'weather_rain': 0,
                'weather_hot': 0,
                'sales_1d_ago': 25,
                'sales_2d_ago': 22,
                'sales_3d_ago': 21,
                'sales_7d_ago': 33,
                'sales_7d_avg': 25.0,
                'sales_7d_max': 33,
                'sales_7d_min': 21,
            }

            pred = predictor.predict_single_day(test_features)
            print(f"预测销量: {pred}")

    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)