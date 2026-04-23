package com.saul.forecast.vo;

import lombok.Data;

import java.math.BigDecimal;
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

    /** 商品零售价（用于前端计算预测销售额） */
    private BigDecimal sellingPrice;

    /** 商品进货价（用于前端计算预测毛利） */
    private BigDecimal purchasePrice;
}