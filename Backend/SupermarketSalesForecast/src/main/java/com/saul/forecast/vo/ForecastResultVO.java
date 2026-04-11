package com.saul.forecast.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 预测结果VO（返回给前端）
 */
@Data
public class ForecastResultVO {

    private Long id;

    /** 预测的目标日期 */
    private LocalDate forecastDate;

    /** 商品ID */
    private Long productId;

    /** 商品编码 */
    private String productCode;

    /** 商品名称 */
    private String productName;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 预测销量 */
    private Integer predictedQuantity;



    /** 当前库存（来自商品表） */
    private Integer currentStock;

    /** 安全库存（来自商品表） */
    private Integer safetyStock;

    /** 建议进货量 = 预测销量 + 安全库存 - 当前库存 */
    private Integer suggestedPurchase;

    /** 状态：sufficient(充足) / warning(紧张) / needPurchase(需补货) */
    private String stockStatus;

    /** 补货周期（天），来自分类表 restock_cycle_days */
    private Integer restockCycleDays;

    /** 历史30天日均销量，用于中/长周期进货量计算 */
    private Double historicalDailyAvg;
}