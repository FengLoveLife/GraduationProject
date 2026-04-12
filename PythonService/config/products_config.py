# -*- coding: utf-8 -*-
"""
销售预测服务 - 商品影响因子配置
用于销售数据生成和预测模型训练

项目：中小型超市日销量预测与智能进货系统
版本：最终版（用于生产部署）

模拟公式：
最终日销量 = 基础销量 × 星期因子 × 天气因子 × 节假日因子 × (1 + 随机波动扰动)

参数说明：
- base_sales: 基础日销量（单位见商品规格）
- weekday_factor: 星期因子 {1:周一, 2:周二, ..., 7:周日}
- weather_factor: 天气因子 {"晴", "多云", "小雨", "大雨", "雪", "炎热"}
- holiday_factor: 节假日因子 {0:普通日, 1:节假日}
- noise_range: 随机波动范围（默认±15%）
"""

# ==================== 101商品完整参数配置 ====================

PRODUCTS_CONFIG = {
    # =================== 【1】休闲零食类（22个）===================

    # [11] 膨化食品 - 周末偏好型
    "SP1101": {
        "name": "乐事薯片(原味)",
        "category_id": 11,
        "category_name": "膨化食品",
        "unit": "包",
        "base_sales": 20,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.10, 6: 1.40, 7: 1.40},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.95, "大雨": 0.90, "雪": 1.0, "炎热": 1.10},
        "holiday_factor": {0: 1.0, 1: 1.40},
        "noise_range": 0.15,
    },
    "SP1102": {
        "name": "上好佳日式大虾片",
        "category_id": 11,
        "category_name": "膨化食品",
        "unit": "包",
        "base_sales": 12,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.30, 7: 1.30},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.95, "大雨": 0.90, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
    "SP1103": {
        "name": "旺旺雪饼",
        "category_id": 11,
        "category_name": "膨化食品",
        "unit": "袋",
        "base_sales": 18,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.10, 6: 1.35, 7: 1.35},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.95, "大雨": 0.90, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.50},
        "noise_range": 0.15
    },
    "SP1104": {
        "name": "呀土豆(番茄味)",
        "category_id": 11,
        "category_name": "膨化食品",
        "unit": "包",
        "base_sales": 15,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.30, 7: 1.30},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.95, "大雨": 0.90, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.35},
        "noise_range": 0.15
    },
    "SP1105": {
        "name": "浪味仙(蔬菜味)",
        "category_id": 11,
        "category_name": "膨化食品",
        "unit": "包",
        "base_sales": 10,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.25, 7: 1.25},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.95, "大雨": 0.90, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.25},
        "noise_range": 0.15
    },
    "SP1106": {
        "name": "多力多滋(超浓芝士)",
        "category_id": 11,
        "category_name": "膨化食品",
        "unit": "包",
        "base_sales": 14,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.10, 6: 1.35, 7: 1.35},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.95, "大雨": 0.90, "雪": 1.0, "炎热": 1.05},
        "holiday_factor": {0: 1.0, 1: 1.40},
        "noise_range": 0.15
    },

    # [12] 饼干糕点 - 稳定消费型
    "SP1201": {
        "name": "奥利奥夹心饼干",
        "category_id": 12,
        "category_name": "饼干糕点",
        "unit": "包",
        "base_sales": 20,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.05, 6: 1.20, 7: 1.20},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.50},
        "noise_range": 0.15
    },
    "SP1202": {
        "name": "好丽友巧克力派",
        "category_id": 12,
        "category_name": "饼干糕点",
        "unit": "盒",
        "base_sales": 12,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.05, 6: 1.15, 7: 1.15},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 0.90},
        "holiday_factor": {0: 1.0, 1: 1.60},
        "noise_range": 0.15
    },
    "SP1203": {
        "name": "达利园瑞士卷",
        "category_id": 12,
        "category_name": "饼干糕点",
        "unit": "袋",
        "base_sales": 10,
        "weekday_factor": {1: 1.05, 2: 1.05, 3: 1.05, 4: 1.05, 5: 1.0, 6: 0.95, 7: 0.95},  # 工作日略高
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.40},
        "noise_range": 0.15
    },
    "SP1204": {
        "name": "康师傅3+2苏打夹心",
        "category_id": 12,
        "category_name": "饼干糕点",
        "unit": "包",
        "base_sales": 22,
        "weekday_factor": {1: 1.05, 2: 1.05, 3: 1.05, 4: 1.05, 5: 1.0, 6: 0.95, 7: 0.95},  # 工作日略高
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.35},
        "noise_range": 0.15
    },
    "SP1205": {
        "name": "嘉士利果酱夹心饼干",
        "category_id": 12,
        "category_name": "饼干糕点",
        "unit": "包",
        "base_sales": 12,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},  # 平稳
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
    "SP1206": {
        "name": "趣多多巧克力曲奇",
        "category_id": 12,
        "category_name": "饼干糕点",
        "unit": "包",
        "base_sales": 18,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.05, 6: 1.20, 7: 1.20},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 0.90},
        "holiday_factor": {0: 1.0, 1: 1.45},
        "noise_range": 0.15
    },

    # [13] 坚果炒货 - 节假日爆发型
    "SP1301": {
        "name": "洽洽香瓜子",
        "category_id": 13,
        "category_name": "坚果炒货",
        "unit": "包",
        "base_sales": 20,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.25, 7: 1.25},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.10, "炎热": 0.95},
        "holiday_factor": {0: 1.0, 1: 1.80},
        "noise_range": 0.15
    },
    "SP1302": {
        "name": "三只松鼠夏威夷果",
        "category_id": 13,
        "category_name": "坚果炒货",
        "unit": "袋",
        "base_sales": 8,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.20, 7: 1.20},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.10, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 2.00},
        "noise_range": 0.15
    },
    "SP1303": {
        "name": "百草味碧根果",
        "category_id": 13,
        "category_name": "坚果炒货",
        "unit": "袋",
        "base_sales": 6,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.20, 7: 1.20},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.10, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.80},
        "noise_range": 0.15
    },
    "SP1304": {
        "name": "老街口焦糖瓜子",
        "category_id": 13,
        "category_name": "坚果炒货",
        "unit": "包",
        "base_sales": 15,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.25, 7: 1.25},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.10, "炎热": 0.90},
        "holiday_factor": {0: 1.0, 1: 1.70},
        "noise_range": 0.15
    },
    "SP1305": {
        "name": "恒康水煮花生",
        "category_id": 13,
        "category_name": "坚果炒货",
        "unit": "包",
        "base_sales": 12,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.20, 7: 1.20},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.10, "炎热": 0.95},
        "holiday_factor": {0: 1.0, 1: 1.50},
        "noise_range": 0.15
    },

    # [14] 糖果巧克力 - 节庆敏感型
    "SP1401": {
        "name": "德芙牛奶巧克力",
        "category_id": 14,
        "category_name": "糖果巧克力",
        "unit": "块",
        "base_sales": 15,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.15, 7: 1.15},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.20, "炎热": 0.70},
        "holiday_factor": {0: 1.0, 1: 2.00},
        "noise_range": 0.15
    },
    "SP1402": {
        "name": "费列罗榛果威化",
        "category_id": 14,
        "category_name": "糖果巧克力",
        "unit": "条",
        "base_sales": 8,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.20, "炎热": 0.75},
        "holiday_factor": {0: 1.0, 1: 1.80},
        "noise_range": 0.15
    },
    "SP1403": {
        "name": "大白兔奶糖",
        "category_id": 14,
        "category_name": "糖果巧克力",
        "unit": "袋",
        "base_sales": 10,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.20, 7: 1.20},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.10, "炎热": 0.80},
        "holiday_factor": {0: 1.0, 1: 1.60},
        "noise_range": 0.15
    },
    "SP1404": {
        "name": "阿尔卑斯棒棒糖",
        "category_id": 14,
        "category_name": "糖果巧克力",
        "unit": "袋",
        "base_sales": 15,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.10, 6: 1.25, 7: 1.25},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.50},
        "noise_range": 0.15
    },
    "SP1405": {
        "name": "绿箭口香糖(薄荷)",
        "category_id": 14,
        "category_name": "糖果巧克力",
        "unit": "条",
        "base_sales": 28,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},  # 平稳
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.10},
        "noise_range": 0.15
    },

    # =================== 【2】酒水饮料类（29个）===================

    # [21] 碳酸饮料 - 天气敏感型
    "SP2101": {
        "name": "可口可乐(经典)",
        "category_id": 21,
        "category_name": "碳酸饮料",
        "unit": "听",
        "base_sales": 28,
        "weekday_factor": {1: 0.85, 2: 0.85, 3: 0.90, 4: 0.95, 5: 1.10, 6: 1.40, 7: 1.40},
        "weather_factor": {"晴": 1.0, "多云": 0.95, "小雨": 0.80, "大雨": 0.70, "雪": 0.90, "炎热": 1.80},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15,
    },
    "SP2102": {
        "name": "百事可乐",
        "category_id": 21,
        "category_name": "碳酸饮料",
        "unit": "听",
        "base_sales": 25,
        "weekday_factor": {1: 0.85, 2: 0.85, 3: 0.90, 4: 0.95, 5: 1.10, 6: 1.35, 7: 1.35},
        "weather_factor": {"晴": 1.0, "多云": 0.95, "小雨": 0.80, "大雨": 0.70, "雪": 0.90, "炎热": 1.80},
        "holiday_factor": {0: 1.0, 1: 1.25},
        "noise_range": 0.15
    },
    "SP2103": {
        "name": "雪碧(柠檬味)",
        "category_id": 21,
        "category_name": "碳酸饮料",
        "unit": "瓶",
        "base_sales": 22,
        "weekday_factor": {1: 0.85, 2: 0.85, 3: 0.90, 4: 0.95, 5: 1.10, 6: 1.40, 7: 1.40},
        "weather_factor": {"晴": 1.0, "多云": 0.95, "小雨": 0.80, "大雨": 0.70, "雪": 0.90, "炎热": 2.00},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
    "SP2104": {
        "name": "芬达(橙味)",
        "category_id": 21,
        "category_name": "碳酸饮料",
        "unit": "瓶",
        "base_sales": 15,
        "weekday_factor": {1: 0.85, 2: 0.85, 3: 0.90, 4: 0.95, 5: 1.10, 6: 1.30, 7: 1.30},
        "weather_factor": {"晴": 1.0, "多云": 0.95, "小雨": 0.80, "大雨": 0.70, "雪": 0.90, "炎热": 1.60},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP2105": {
        "name": "七喜",
        "category_id": 21,
        "category_name": "碳酸饮料",
        "unit": "听",
        "base_sales": 10,
        "weekday_factor": {1: 0.85, 2: 0.85, 3: 0.90, 4: 0.95, 5: 1.10, 6: 1.25, 7: 1.25},
        "weather_factor": {"晴": 1.0, "多云": 0.95, "小雨": 0.80, "大雨": 0.70, "雪": 0.90, "炎热": 1.70},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP2106": {
        "name": "可口可乐(零度)",
        "category_id": 21,
        "category_name": "碳酸饮料",
        "unit": "瓶",
        "base_sales": 8,
        "weekday_factor": {1: 0.85, 2: 0.85, 3: 0.90, 4: 0.95, 5: 1.05, 6: 1.20, 7: 1.20},
        "weather_factor": {"晴": 1.0, "多云": 0.95, "小雨": 0.80, "大雨": 0.70, "雪": 0.90, "炎热": 1.50},
        "holiday_factor": {0: 1.0, 1: 1.15},
        "noise_range": 0.15
    },

    # [22] 饮用水 - 天气敏感型
    "SP2201": {
        "name": "农夫山泉饮用天然水",
        "category_id": 22,
        "category_name": "饮用水",
        "unit": "瓶",
        "base_sales": 32,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.20, 7: 1.20},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.80, "雪": 0.90, "炎热": 1.60},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
    "SP2202": {
        "name": "怡宝纯净水",
        "category_id": 22,
        "category_name": "饮用水",
        "unit": "瓶",
        "base_sales": 28,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.15, 7: 1.15},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.80, "雪": 0.90, "炎热": 1.50},
        "holiday_factor": {0: 1.0, 1: 1.25},
        "noise_range": 0.15
    },
    "SP2203": {
        "name": "百岁山天然矿泉水",
        "category_id": 22,
        "category_name": "饮用水",
        "unit": "瓶",
        "base_sales": 12,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.80, "雪": 0.90, "炎热": 1.40},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP2204": {
        "name": "娃哈哈纯净水",
        "category_id": 22,
        "category_name": "饮用水",
        "unit": "瓶",
        "base_sales": 20,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.80, "雪": 0.90, "炎热": 1.50},
        "holiday_factor": {0: 1.0, 1: 1.15},
        "noise_range": 0.15
    },
    "SP2205": {
        "name": "恒大冰泉",
        "category_id": 22,
        "category_name": "饮用水",
        "unit": "瓶",
        "base_sales": 10,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.05, 7: 1.05},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.80, "雪": 0.90, "炎热": 1.35},
        "holiday_factor": {0: 1.0, 1: 1.10},
        "noise_range": 0.15
    },
    "SP2206": {
        "name": "农夫山泉(大包装)",
        "category_id": 22,
        "category_name": "饮用水",
        "unit": "桶",
        "base_sales": 8,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.05, 6: 1.15, 7: 1.15},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 1.30},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },

    # [23] 果汁茶饮 - 天气敏感型
    "SP2301": {
        "name": "康师傅冰红茶",
        "category_id": 23,
        "category_name": "果汁茶饮",
        "unit": "瓶",
        "base_sales": 22,
        "weekday_factor": {1: 0.85, 2: 0.85, 3: 0.90, 4: 0.95, 5: 1.10, 6: 1.30, 7: 1.30},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.85, "大雨": 0.75, "雪": 0.90, "炎热": 1.70},
        "holiday_factor": {0: 1.0, 1: 1.25},
        "noise_range": 0.15
    },
    "SP2302": {
        "name": "统一绿茶",
        "category_id": 23,
        "category_name": "果汁茶饮",
        "unit": "瓶",
        "base_sales": 18,
        "weekday_factor": {1: 0.85, 2: 0.85, 3: 0.90, 4: 0.95, 5: 1.10, 6: 1.20, 7: 1.20},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.80, "雪": 0.90, "炎热": 1.40},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP2303": {
        "name": "东方树叶(茉莉)",
        "category_id": 23,
        "category_name": "果汁茶饮",
        "unit": "瓶",
        "base_sales": 8,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 1.30},
        "holiday_factor": {0: 1.0, 1: 1.15},
        "noise_range": 0.15
    },
    "SP2304": {
        "name": "茶π(蜜桃乌龙)",
        "category_id": 23,
        "category_name": "果汁茶饮",
        "unit": "瓶",
        "base_sales": 14,
        "weekday_factor": {1: 0.85, 2: 0.85, 3: 0.90, 4: 0.95, 5: 1.10, 6: 1.25, 7: 1.25},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.80, "雪": 0.90, "炎热": 1.50},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP2305": {
        "name": "美汁源果粒橙",
        "category_id": 23,
        "category_name": "果汁茶饮",
        "unit": "瓶",
        "base_sales": 16,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.20, 7: 1.20},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.90, "雪": 1.0, "炎热": 1.40},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP2306": {
        "name": "汇源100%橙汁",
        "category_id": 23,
        "category_name": "果汁茶饮",
        "unit": "盒",
        "base_sales": 7,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.05, 6: 1.15, 7: 1.15},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 1.20},
        "holiday_factor": {0: 1.0, 1: 1.50},
        "noise_range": 0.15
    },

    # [24] 乳制品 - 晨间消费型
    "SP2401": {
        "name": "伊利纯牛奶",
        "category_id": 24,
        "category_name": "乳制品",
        "unit": "盒",
        "base_sales": 25,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},  # 平稳
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.95, "大雨": 0.90, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
    "SP2402": {
        "name": "蒙牛特仑苏",
        "category_id": 24,
        "category_name": "乳制品",
        "unit": "盒",
        "base_sales": 18,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.95, "大雨": 0.90, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.50},
        "noise_range": 0.15
    },
    "SP2403": {
        "name": "光明优倍鲜奶",
        "category_id": 24,
        "category_name": "乳制品",
        "unit": "盒",
        "base_sales": 6,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.85, "雪": 1.0, "炎热": 0.95},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP2404": {
        "name": "旺仔牛奶",
        "category_id": 24,
        "category_name": "乳制品",
        "unit": "罐",
        "base_sales": 16,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.05, 6: 1.15, 7: 1.15},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.25},
        "noise_range": 0.15
    },
    "SP2405": {
        "name": "安慕希希腊酸奶",
        "category_id": 24,
        "category_name": "乳制品",
        "unit": "盒",
        "base_sales": 18,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.05, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
    "SP2406": {
        "name": "纯甄酸牛奶",
        "category_id": 24,
        "category_name": "乳制品",
        "unit": "盒",
        "base_sales": 12,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.05, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.25},
        "noise_range": 0.15
    },

    # [25] 啤酒麦酒 - 夏季爆发型
    "SP2501": {
        "name": "雪花勇闯天涯",
        "category_id": 25,
        "category_name": "啤酒麦酒",
        "unit": "听",
        "base_sales": 18,
        "weekday_factor": {1: 0.70, 2: 0.70, 3: 0.80, 4: 0.90, 5: 1.20, 6: 1.60, 7: 1.60},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.80, "雪": 0.90, "炎热": 2.20},
        "holiday_factor": {0: 1.0, 1: 1.50},
        "noise_range": 0.15
    },
    "SP2502": {
        "name": "青岛啤酒(经典)",
        "category_id": 25,
        "category_name": "啤酒麦酒",
        "unit": "瓶",
        "base_sales": 16,
        "weekday_factor": {1: 0.70, 2: 0.70, 3: 0.80, 4: 0.90, 5: 1.15, 6: 1.50, 7: 1.50},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.80, "雪": 0.90, "炎热": 2.00},
        "holiday_factor": {0: 1.0, 1: 1.40},
        "noise_range": 0.15
    },
    "SP2503": {
        "name": "百威啤酒",
        "category_id": 25,
        "category_name": "啤酒麦酒",
        "unit": "听",
        "base_sales": 8,
        "weekday_factor": {1: 0.70, 2: 0.70, 3: 0.80, 4: 0.90, 5: 1.10, 6: 1.40, 7: 1.40},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.80, "雪": 0.90, "炎热": 1.80},
        "holiday_factor": {0: 1.0, 1: 1.35},
        "noise_range": 0.15
    },
    "SP2504": {
        "name": "燕京纯生",
        "category_id": 25,
        "category_name": "啤酒麦酒",
        "unit": "听",
        "base_sales": 10,
        "weekday_factor": {1: 0.70, 2: 0.70, 3: 0.80, 4: 0.90, 5: 1.15, 6: 1.45, 7: 1.45},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.80, "雪": 0.90, "炎热": 1.90},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
    "SP2505": {
        "name": "科罗娜啤酒",
        "category_id": 25,
        "category_name": "啤酒麦酒",
        "unit": "瓶",
        "base_sales": 6,
        "weekday_factor": {1: 0.70, 2: 0.70, 3: 0.80, 4: 0.90, 5: 1.10, 6: 1.30, 7: 1.30},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.80, "雪": 0.90, "炎热": 1.60},
        "holiday_factor": {0: 1.0, 1: 1.25},
        "noise_range": 0.15
    },

    # =================== 【3】生鲜果蔬类（17个）===================

    # [31] 新鲜水果 - 季节波动型
    "SP3101": {
        "name": "红富士苹果",
        "category_id": 31,
        "category_name": "新鲜水果",
        "unit": "kg",
        "base_sales": 25,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.20, 7: 1.20},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 0.90},
        "holiday_factor": {0: 1.0, 1: 1.40},
        "noise_range": 0.15
    },
    "SP3102": {
        "name": "进口香蕉",
        "category_id": 31,
        "category_name": "新鲜水果",
        "unit": "kg",
        "base_sales": 22,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.15, 7: 1.15},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 0.85},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP3103": {
        "name": "麒麟西瓜",
        "category_id": 31,
        "category_name": "新鲜水果",
        "unit": "个",
        "base_sales": 18,
        "weekday_factor": {1: 0.70, 2: 0.70, 3: 0.80, 4: 0.90, 5: 1.20, 6: 1.50, 7: 1.50},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.80, "大雨": 0.60, "雪": 0.50, "炎热": 2.50},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
    "SP3104": {
        "name": "巨峰葡萄",
        "category_id": 31,
        "category_name": "新鲜水果",
        "unit": "kg",
        "base_sales": 15,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.25, 7: 1.25},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.85, "雪": 0.90, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.35},
        "noise_range": 0.15
    },
    "SP3105": {
        "name": "砂糖橘",
        "category_id": 31,
        "category_name": "新鲜水果",
        "unit": "kg",
        "base_sales": 20,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.30, 7: 1.30},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.20, "炎热": 0.80},
        "holiday_factor": {0: 1.0, 1: 1.80},
        "noise_range": 0.15
    },
    "SP3106": {
        "name": "智利车厘子JJ级",
        "category_id": 31,
        "category_name": "新鲜水果",
        "unit": "盒",
        "base_sales": 8,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.10, "炎热": 0.90},
        "holiday_factor": {0: 1.0, 1: 2.00},
        "noise_range": 0.15
    },

    # [32] 时令蔬菜 - 晨间刚需型
    "SP3201": {
        "name": "大白菜",
        "category_id": 32,
        "category_name": "时令蔬菜",
        "unit": "kg",
        "base_sales": 30,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.75, "大雨": 0.60, "雪": 1.10, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.50},
        "noise_range": 0.15
    },
    "SP3202": {
        "name": "红干西红柿",
        "category_id": 32,
        "category_name": "时令蔬菜",
        "unit": "kg",
        "base_sales": 25,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.80, "大雨": 0.70, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP3203": {
        "name": "黄心土豆",
        "category_id": 32,
        "category_name": "时令蔬菜",
        "unit": "kg",
        "base_sales": 28,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.85, "大雨": 0.75, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.40},
        "noise_range": 0.15
    },
    "SP3204": {
        "name": "本地黄瓜",
        "category_id": 32,
        "category_name": "时令蔬菜",
        "unit": "kg",
        "base_sales": 20,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.90, "大雨": 0.80, "雪": 0.90, "炎热": 1.20},
        "holiday_factor": {0: 1.0, 1: 1.10},
        "noise_range": 0.15
    },
    "SP3205": {
        "name": "胡萝卜",
        "category_id": 32,
        "category_name": "时令蔬菜",
        "unit": "kg",
        "base_sales": 22,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.80, "大雨": 0.70, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
    "SP3206": {
        "name": "菠菜",
        "category_id": 32,
        "category_name": "时令蔬菜",
        "unit": "把",
        "base_sales": 20,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.70, "大雨": 0.50, "雪": 0.90, "炎热": 0.90},
        "holiday_factor": {0: 1.0, 1: 1.15},
        "noise_range": 0.15
    },

    # [33] 肉禽蛋品 - 节前囤货型
    "SP3301": {
        "name": "冷鲜五花肉",
        "category_id": 33,
        "category_name": "肉禽蛋品",
        "unit": "kg",
        "base_sales": 18,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.20, 7: 1.20},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.95, "大雨": 0.90, "雪": 1.10, "炎热": 0.90},
        "holiday_factor": {0: 1.0, 1: 2.00},
        "noise_range": 0.15
    },
    "SP3302": {
        "name": "鲜鸡琵琶腿",
        "category_id": 33,
        "category_name": "肉禽蛋品",
        "unit": "kg",
        "base_sales": 15,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.15, 7: 1.15},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.95, "大雨": 0.90, "雪": 1.0, "炎热": 0.95},
        "holiday_factor": {0: 1.0, 1: 1.60},
        "noise_range": 0.15
    },
    "SP3303": {
        "name": "农家散养土鸡蛋",
        "category_id": 33,
        "category_name": "肉禽蛋品",
        "unit": "盒",
        "base_sales": 30,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.85, "大雨": 0.80, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.80},
        "noise_range": 0.15
    },
    "SP3304": {
        "name": "精瘦肉馅",
        "category_id": 33,
        "category_name": "肉禽蛋品",
        "unit": "盒",
        "base_sales": 15,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 0.95, "大雨": 0.90, "雪": 1.0, "炎热": 0.90},
        "holiday_factor": {0: 1.0, 1: 1.50},
        "noise_range": 0.15
    },
    "SP3305": {
        "name": "高邮咸鸭蛋",
        "category_id": 33,
        "category_name": "肉禽蛋品",
        "unit": "盒",
        "base_sales": 12,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.05, 6: 1.15, 7: 1.15},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 0.95, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.50},
        "noise_range": 0.15
    },

    # =================== 【4】粮油调味类（16个）===================

    # [41] 米面杂粮 - 节前囤货型
    "SP4101": {
        "name": "金龙鱼东北大米",
        "category_id": 41,
        "category_name": "米面杂粮",
        "unit": "袋",
        "base_sales": 18,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.80},
        "noise_range": 0.15
    },
    "SP4102": {
        "name": "柴火大院五常大米",
        "category_id": 41,
        "category_name": "米面杂粮",
        "unit": "袋",
        "base_sales": 8,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.05, 7: 1.05},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 2.00},
        "noise_range": 0.15
    },
    "SP4103": {
        "name": "陈克明原味挂面",
        "category_id": 41,
        "category_name": "米面杂粮",
        "unit": "把",
        "base_sales": 20,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
    "SP4104": {
        "name": "桂格即食燕麦片",
        "category_id": 41,
        "category_name": "米面杂粮",
        "unit": "袋",
        "base_sales": 12,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP4105": {
        "name": "东北黄豆",
        "category_id": 41,
        "category_name": "米面杂粮",
        "unit": "kg",
        "base_sales": 10,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.25},
        "noise_range": 0.15
    },

    # [42] 食用油 - 节前囤货型
    "SP4201": {
        "name": "金龙鱼1:1:1调和油",
        "category_id": 42,
        "category_name": "食用油",
        "unit": "桶",
        "base_sales": 12,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.60},
        "noise_range": 0.15
    },
    "SP4202": {
        "name": "鲁花5S压榨花生油",
        "category_id": 42,
        "category_name": "食用油",
        "unit": "桶",
        "base_sales": 8,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.05, 7: 1.05},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 2.00},
        "noise_range": 0.15
    },
    "SP4203": {
        "name": "福临门大豆油",
        "category_id": 42,
        "category_name": "食用油",
        "unit": "桶",
        "base_sales": 8,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.05, 7: 1.05},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.50},
        "noise_range": 0.15
    },
    "SP4204": {
        "name": "多力葵花籽油",
        "category_id": 42,
        "category_name": "食用油",
        "unit": "桶",
        "base_sales": 6,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.05, 7: 1.05},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.40},
        "noise_range": 0.15
    },
    "SP4205": {
        "name": "欧丽薇兰特级初榨橄榄油",
        "category_id": 42,
        "category_name": "食用油",
        "unit": "瓶",
        "base_sales": 4,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.80},
        "noise_range": 0.15
    },

    # [43] 调味品 - 稳定刚需型
    "SP4301": {
        "name": "海天金标生抽",
        "category_id": 43,
        "category_name": "调味品",
        "unit": "瓶",
        "base_sales": 12,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP4302": {
        "name": "厨邦酱油",
        "category_id": 43,
        "category_name": "调味品",
        "unit": "瓶",
        "base_sales": 10,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.15},
        "noise_range": 0.15
    },
    "SP4303": {
        "name": "老干妈风味豆豉",
        "category_id": 43,
        "category_name": "调味品",
        "unit": "瓶",
        "base_sales": 8,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.05, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.25},
        "noise_range": 0.15
    },
    "SP4304": {
        "name": "恒顺镇江香醋",
        "category_id": 43,
        "category_name": "调味品",
        "unit": "瓶",
        "base_sales": 10,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.15},
        "noise_range": 0.15
    },
    "SP4305": {
        "name": "太太乐鸡精",
        "category_id": 43,
        "category_name": "调味品",
        "unit": "袋",
        "base_sales": 14,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP4306": {
        "name": "王守义十三香",
        "category_id": 43,
        "category_name": "调味品",
        "unit": "盒",
        "base_sales": 15,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },

    # =================== 【5】日用百货类（17个）===================

    # [51] 纸巾湿巾 - 稳定刚需型
    "SP5101": {
        "name": "清风原木纯品抽纸",
        "category_id": 51,
        "category_name": "纸巾湿巾",
        "unit": "提",
        "base_sales": 15,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.10},
        "noise_range": 0.15
    },
    "SP5102": {
        "name": "维达立体美卷纸",
        "category_id": 51,
        "category_name": "纸巾湿巾",
        "unit": "提",
        "base_sales": 10,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.05},
        "noise_range": 0.15
    },
    "SP5103": {
        "name": "心相印茶语手帕纸",
        "category_id": 51,
        "category_name": "纸巾湿巾",
        "unit": "条",
        "base_sales": 25,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.20, 7: 1.20},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.15},
        "noise_range": 0.15
    },
    "SP5104": {
        "name": "洁柔Face抽纸",
        "category_id": 51,
        "category_name": "纸巾湿巾",
        "unit": "提",
        "base_sales": 12,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.10},
        "noise_range": 0.15
    },
    "SP5105": {
        "name": "全棉时代纯棉柔巾",
        "category_id": 51,
        "category_name": "纸巾湿巾",
        "unit": "包",
        "base_sales": 5,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.10},
        "noise_range": 0.15
    },
    "SP5106": {
        "name": "舒洁除菌湿厕纸",
        "category_id": 51,
        "category_name": "纸巾湿巾",
        "unit": "包",
        "base_sales": 8,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.10},
        "noise_range": 0.15
    },

    # [52] 洗护清洁 - 周期购买型
    "SP5201": {
        "name": "海飞丝去屑洗发水",
        "category_id": 52,
        "category_name": "洗护清洁",
        "unit": "瓶",
        "base_sales": 8,
        "weekday_factor": {1: 0.90, 2: 0.90, 3: 0.95, 4: 0.95, 5: 1.05, 6: 1.15, 7: 1.15},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
    "SP5202": {
        "name": "舒肤佳清香沐浴露",
        "category_id": 52,
        "category_name": "洗护清洁",
        "unit": "瓶",
        "base_sales": 6,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 0.95, "炎热": 1.20},
        "holiday_factor": {0: 1.0, 1: 1.10},
        "noise_range": 0.15
    },
    "SP5203": {
        "name": "蓝月亮深层洁净洗衣液",
        "category_id": 52,
        "category_name": "洗护清洁",
        "unit": "瓶",
        "base_sales": 7,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.10, 7: 1.10},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.10, "大雨": 1.10, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.40},
        "noise_range": 0.15
    },
    "SP5204": {
        "name": "奥妙全自动洗衣粉",
        "category_id": 52,
        "category_name": "洗护清洁",
        "unit": "袋",
        "base_sales": 10,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
    "SP5205": {
        "name": "立白除菌洗洁精",
        "category_id": 52,
        "category_name": "洗护清洁",
        "unit": "瓶",
        "base_sales": 12,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP5206": {
        "name": "高露洁三重功效牙膏",
        "category_id": 52,
        "category_name": "洗护清洁",
        "unit": "支",
        "base_sales": 15,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.10},
        "noise_range": 0.15
    },

    # [53] 家居用品 - 低频稳定型
    "SP5301": {
        "name": "妙洁加厚垃圾袋",
        "category_id": 53,
        "category_name": "家居用品",
        "unit": "卷",
        "base_sales": 12,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.15},
        "noise_range": 0.15
    },
    "SP5302": {
        "name": "佳能PE保鲜膜",
        "category_id": 53,
        "category_name": "家居用品",
        "unit": "卷",
        "base_sales": 8,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
    "SP5303": {
        "name": "3M思高海绵百洁布",
        "category_id": 53,
        "category_name": "家居用品",
        "unit": "包",
        "base_sales": 10,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.25},
        "noise_range": 0.15
    },
    "SP5304": {
        "name": "洁丽雅纯棉毛巾",
        "category_id": 53,
        "category_name": "家居用品",
        "unit": "条",
        "base_sales": 6,
        "weekday_factor": {1: 0.95, 2: 0.95, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.15, 7: 1.15},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.20},
        "noise_range": 0.15
    },
    "SP5305": {
        "name": "滴露消毒液",
        "category_id": 53,
        "category_name": "家居用品",
        "unit": "瓶",
        "base_sales": 4,
        "weekday_factor": {1: 1.0, 2: 1.0, 3: 1.0, 4: 1.0, 5: 1.0, 6: 1.0, 7: 1.0},
        "weather_factor": {"晴": 1.0, "多云": 1.0, "小雨": 1.0, "大雨": 1.0, "雪": 1.0, "炎热": 1.0},
        "holiday_factor": {0: 1.0, 1: 1.30},
        "noise_range": 0.15
    },
}

# ==================== 工具函数 ====================

def get_product_config(product_code):
    """获取单个商品配置"""
    return PRODUCTS_CONFIG.get(product_code)

def get_all_products():
    """获取所有商品配置"""
    return PRODUCTS_CONFIG

def get_products_by_category(category_id):
    """按分类获取商品"""
    return {code: config for code, config in PRODUCTS_CONFIG.items()
            if config["category_id"] == category_id}