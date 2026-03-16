package com.saul.sales.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 销售趋势图表 VO
 */
@Data
public class TrendChartVO {
    /** 日期数组 ["03-01", "03-02"] */
    private List<String> dates;
    
    /** 每日销售额数组 */
    private List<BigDecimal> amounts;
    
    /** 每日毛利数组 */
    private List<BigDecimal> profits;
}
