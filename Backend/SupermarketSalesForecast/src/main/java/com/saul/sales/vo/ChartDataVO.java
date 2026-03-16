package com.saul.sales.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用图表项数据 VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartDataVO {
    /**
     * 项名称 (如：分类名、状态名)
     */
    private String name;

    /**
     * 数值 (兼容 Long 和 BigDecimal)
     */
    private Object value;
}