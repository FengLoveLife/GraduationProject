# -*- coding: utf-8 -*-
"""
历史日期批量预测脚本
对指定日期范围分批运行预测，将结果写入 forecast_result 表。
用于填充历史预测数据，支持前端"预测 vs 实际"对比分析。

前提条件：
  1. MySQL 已启动，sales_order_item 中已有对应日期的销售数据
  2. calendar_factor 表已包含目标日期范围的数据
  3. 模型文件 forecast/model/sales_forecast_model.pkl 已存在

使用方法：
    cd D:\\GraduationProject\\PythonService
    python run_historical_forecast.py
"""

import sys
import os
from datetime import datetime, timedelta

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from forecast.predictor import RollingPredictor

# ==================== 配置区（按需修改）====================

START_DATE = '2025-12-01'   # 历史预测起始日期
END_DATE   = '2026-04-26'   # 历史预测结束日期
BATCH_DAYS = 7              # 每批预测天数 = 模型训练时的预测窗口，避免滚动误差累积

# ===========================================================


def run_historical_forecasts(start_date: str, end_date: str, batch_days: int = 30):
    """
    批量生成历史日期的预测结果，分批写入 forecast_result 表。

    Args:
        start_date: 起始日期 'YYYY-MM-DD'
        end_date:   结束日期 'YYYY-MM-DD'
        batch_days: 每批预测天数
    """
    start_dt   = datetime.strptime(start_date, '%Y-%m-%d')
    end_dt     = datetime.strptime(end_date,   '%Y-%m-%d')
    total_days = (end_dt - start_dt).days + 1
    total_batches = (total_days + batch_days - 1) // batch_days

    print('=' * 60)
    print('  历史销量批量预测')
    print('=' * 60)
    print(f'预测范围 : {start_date} ~ {end_date}（共 {total_days} 天）')
    print(f'每批天数 : {batch_days} 天，共 {total_batches} 批次')
    print('=' * 60)

    predictor = RollingPredictor()

    try:
        current   = start_dt
        batch_num = 0

        while current <= end_dt:
            batch_end      = min(current + timedelta(days=batch_days - 1), end_dt)
            days           = (batch_end - current).days + 1
            forecast_start = current.strftime('%Y-%m-%d')
            batch_end_str  = batch_end.strftime('%Y-%m-%d')
            batch_num     += 1

            print(f'\n[{batch_num}/{total_batches}] {forecast_start} ~ {batch_end_str}（{days} 天）')

            results = predictor.predict_all_products(forecast_start, days)

            if results:
                predictor.save_predictions_to_db(results)
                record_count = sum(len(r['predictions']) for r in results.values())
                print(f'  写入完成：{len(results)} 个商品 × {days} 天 = {record_count} 条记录')
            else:
                print(f'  [警告] 无预测结果，请检查该日期段的 calendar_factor 数据是否完整')

            current = batch_end + timedelta(days=1)

    finally:
        predictor.close()

    print(f'\n{"=" * 60}')
    print(f'  全部完成！共 {batch_num} 批次')
    print(f'  历史预测数据已写入 forecast_result 表')
    print(f'  现在可以在前端查看"预测 vs 实际"对比图')
    print(f'{"=" * 60}')


if __name__ == '__main__':
    run_historical_forecasts(START_DATE, END_DATE, BATCH_DAYS)
