package com.saul.forecast.vo;

import lombok.Data;

/**
 * 预测 vs 实际 对比 VO
 */
@Data
public class ForecastCompareVO {

    /** 日期 */
    private String date;

    /** 商品 ID */
    private Long productId;

    /** 商品编码 */
    private String productCode;

    /** 商品名称 */
    private String productName;

    /** 分类 ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 预测销量 */
    private Integer predictedQuantity;

    /** 实际销量 */
    private Integer actualQuantity;

    /** 差异（预测 - 实际） */
    private Integer variance;

    /** 误差率（%） */
    private Double errorRate;
}
