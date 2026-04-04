package com.saul.forecast.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 预测结果查询条件 DTO
 */
@Data
public class ForecastQueryDTO {

    /** 预测日期 */
    private LocalDate forecastDate;

    /** 商品ID */
    private Long productId;

    /** 分类ID */
    private Long categoryId;

    /** 商品名称（模糊搜索） */
    private String productName;

    /** 查询天数（从forecastDate开始） */
    private Integer days;

    /** 库存状态筛选：sufficient / warning / needPurchase */
    private String stockStatus;
}