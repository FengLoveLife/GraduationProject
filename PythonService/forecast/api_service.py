# -*- coding: utf-8 -*-
"""
FastAPI 服务模块
提供销量预测的 HTTP API 接口

API 列表：
- POST /forecast/run          触发批量预测
- GET  /forecast/results      查询预测结果
- GET  /forecast/status       查询服务状态
"""

import sys
import os
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI, Query, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, List
import uvicorn

from forecast.predictor import RollingPredictor, run_full_prediction
from forecast.model_trainer import SalesPredictor, train_and_save_model
from data_generator.db_manager import DatabaseManager


# ==================== FastAPI 应用 ====================

app = FastAPI(
    title="销量预测服务",
    description="中小型超市日销量预测 API",
    version="1.0.0"
)

# 跨域配置,简单说就是允许跨域访问
app.add_middleware(
    CORSMiddleware,
    #允许任意域名访问
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ==================== 响应模型 ====================

class PredictionResult(BaseModel):
    """预测结果"""
    forecast_date: str
    product_id: int
    product_code: str
    product_name: str
    category_id: int
    category_name: str
    predicted_quantity: int


class ForecastResponse(BaseModel):
    """批量预测响应"""
    success: bool
    message: str
    product_count: int
    record_count: int
    forecast_start: str
    forecast_days: int


class StatusResponse(BaseModel):
    """服务状态响应"""
    status: str
    model_loaded: bool
    model_info: Optional[dict] = None
    server_time: str


class TrainResponse(BaseModel):
    """训练响应"""
    success: bool
    message: str
    mae: Optional[float] = None
    mape: Optional[float] = None


# ==================== 全局变量 ====================

# 预测器实例（只加载模型，不连接数据库）
# 核心修复：避免长期持有数据库连接导致超时
_predictor = None


def get_predictor():
    """
    获取预测器实例（单例，只加载模型）

    若模型文件不存在，自动使用最新销售数据训练后再加载。
    注意：此预测器不持有数据库连接，数据库连接在每次请求时单独创建
    """
    global _predictor
    if _predictor is None:
        # 尝试直接加载
        try:
            _predictor = RollingPredictor.load_model_only()
        except (ValueError, FileNotFoundError):
            # 模型文件不存在或加载失败 → 自动训练
            print("[预测服务] 未找到模型文件，开始自动训练...")
            try:
                train_and_save_model()
                _predictor = RollingPredictor.load_model_only()
                print("[预测服务] 自动训练完成，模型已就绪")
            except Exception as train_err:
                raise HTTPException(status_code=500, detail=f"自动训练失败: {str(train_err)}")
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"模型加载失败: {str(e)}")
    return _predictor


def get_db():
    """获取数据库连接（每次请求创建新连接）"""
    return DatabaseManager()


# ==================== API 路由 ====================

@app.get("/", tags=["根路由"])
async def root():
    """根路由"""
    return {
        "service": "销量预测服务",
        "version": "1.0.0",
        "endpoints": [
            "GET  /forecast/status - 查询服务状态",
            "POST /forecast/run   - 触发批量预测",
            "GET  /forecast/results - 查询预测结果",
            "POST /forecast/train - 重新训练模型"
        ]
    }


@app.get("/forecast/status", response_model=StatusResponse, tags=["预测服务"])
async def get_status():
    """
    查询服务状态

    返回模型加载状态和基本信息
    """
    try:
        predictor = get_predictor()

        model_info = None
        if predictor.predictor.training_stats:
            model_info = {
                "training_time": predictor.predictor.training_stats.get("training_time", "N/A"),
                "mae": predictor.predictor.training_stats.get("mae", 0),
                "mape": predictor.predictor.training_stats.get("mape", 0),
                "r2": predictor.predictor.training_stats.get("r2", 0),
            }

        return StatusResponse(
            status="running",
            model_loaded=True,
            model_info=model_info,
            server_time=datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        )
    except Exception as e:
        return StatusResponse(
            status="error",
            model_loaded=False,
            model_info={"error": str(e)},
            server_time=datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        )


@app.post("/forecast/run", response_model=ForecastResponse, tags=["预测服务"])
async def run_forecast(
    forecast_start: str = Query(None, description="预测起始日期，格式YYYY-MM-DD，默认为最新销售日期+1天"),
    forecast_days: int = Query(7, description="预测天数", ge=1, le=30)
):
    """
    触发批量预测

    - forecast_start: 预测起始日期，不传则使用最新销售日期+1天
    - forecast_days: 预测天数，1-30天
    """
    try:
        # 核心修复：默认预测日期 = 数据库中销售数据的最后一天 + 1天
        # 而不是 datetime.now()，避免时空错乱
        if forecast_start is None:
            db = get_db()
            db.connect()
            try:
                result = db.query_one("SELECT MAX(sale_date) as max_date FROM sales_order_item")
                if result and result['max_date']:
                    from datetime import datetime, timedelta
                    max_date = result['max_date']
                    # 预测起始日期 = 最后销售日期 + 1天
                    forecast_start = (max_date + timedelta(days=1)).strftime("%Y-%m-%d")
                else:
                    # 如果没有销售数据，才使用今天
                    forecast_start = datetime.now().strftime("%Y-%m-%d")
            finally:
                db.close()

        # 执行预测
        results = run_full_prediction(
            base_date=forecast_start,
            forecast_days=forecast_days,
            save_to_db=True
        )

        if not results:
            return ForecastResponse(
                success=False,
                message="预测失败，无结果",
                product_count=0,
                record_count=0,
                forecast_start=forecast_start,
                forecast_days=forecast_days
            )

        # 统计
        record_count = sum(len(r["predictions"]) for r in results.values())

        return ForecastResponse(
            success=True,
            message=f"预测完成，共预测 {len(results)} 个商品",
            product_count=len(results),
            record_count=record_count,
            forecast_start=forecast_start,
            forecast_days=forecast_days
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"预测失败: {str(e)}")


@app.get("/forecast/results", tags=["预测服务"])
async def get_results(
    forecast_date: Optional[str] = Query(None, description="查询起始日期，默认最新销售日期"),
    product_id: Optional[int] = Query(None, description="商品ID"),
    category_id: Optional[int] = Query(None, description="分类ID"),
    days: int = Query(7, description="查询天数", ge=1, le=30)
):
    """
    查询预测结果

    从数据库查询已保存的预测结果，支持筛选条件
    """
    try:
        predictor = get_predictor()

        # 注意：predictor 会延迟连接数据库，查询完成后需要关闭
        try:
            results = predictor.get_predictions_from_db(
                forecast_date=forecast_date,
                product_id=product_id,
                category_id=category_id,
                days=days
            )
        finally:
            predictor.close()

        return {
            "success": True,
            "count": len(results),
            "data": results
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"查询失败: {str(e)}")


@app.get("/forecast/summary", tags=["预测服务"])
async def get_summary(
    forecast_date: Optional[str] = Query(None, description="预测日期，默认最新销售日期"),
    days: int = Query(1, description="累计天数 1/3/7", ge=1, le=7)
):
    """
    查询预测汇总（按商品）

    返回指定天数的累计预测销量
    """
    try:
        predictor = get_predictor()

        # 查询数据库（默认日期在 get_predictions_from_db 中处理）
        try:
            results = predictor.get_predictions_from_db(
                forecast_date=forecast_date,
                days=days
            )
        finally:
            predictor.close()

        if not results:
            return {
                "success": True,
                "count": 0,
                "data": []
            }

        # 按商品汇总
        from collections import defaultdict
        summary = defaultdict(lambda: {
            "product_id": 0,
            "product_code": "",
            "product_name": "",
            "category_id": 0,
            "category_name": "",
            "total_predicted": 0,
            "daily_details": []
        })

        for row in results:
            pid = row["product_id"]
            summary[pid]["product_id"] = pid
            summary[pid]["product_code"] = row["product_code"]
            summary[pid]["product_name"] = row["product_name"]
            summary[pid]["category_id"] = row["category_id"]
            summary[pid]["category_name"] = row["category_name"]
            summary[pid]["total_predicted"] += row["predicted_quantity"]
            summary[pid]["daily_details"].append({
                "date": str(row["forecast_date"]),
                "predicted": row["predicted_quantity"]
            })

        return {
            "success": True,
            "count": len(summary),
            "forecast_date": forecast_date,
            "days": days,
            "data": list(summary.values())
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"查询失败: {str(e)}")


@app.post("/forecast/train", response_model=TrainResponse, tags=["模型管理"])
async def train_model(
    base_date: str = Query(None, description="训练基准日期，默认使用配置中的日期")
):
    """
    重新训练模型

    使用数据库中的历史销量数据重新训练LightGBM模型
    """
    try:
        stats = train_and_save_model(base_date)

        if stats is None:
            return TrainResponse(
                success=False,
                message="训练失败，请检查数据"
            )

        # 重置预测器
        global _predictor
        _predictor = None

        return TrainResponse(
            success=True,
            message="模型训练完成",
            mae=stats.get("mae", 0),
            mape=stats.get("mape", 0)
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"训练失败: {str(e)}")


# ==================== 主入口 ====================

if __name__ == "__main__":
    print("=" * 60)
    print("销量预测服务启动")
    print("=" * 60)
    print("API 文档: http://localhost:8000/docs")
    print("=" * 60)

    uvicorn.run(
        "api_service:app",
        host="0.0.0.0",
        port=8000,
        reload=False
    )