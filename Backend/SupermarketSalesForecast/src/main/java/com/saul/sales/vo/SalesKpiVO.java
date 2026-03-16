package com.saul.sales.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 销售分析核心 KPI VO
 */
@Data
public class SalesKpiVO {
    /** 总销售额 */
    private BigDecimal totalAmount;
    
    /** 总毛利润 */
    private BigDecimal totalProfit;
    
    /** 总订单数 */
    private Integer orderCount;
    
    /** 商品总销量 */
    private Integer totalQuantity;
    
    /** 客单价 (销售额/订单数) */
    private BigDecimal customerPrice;
    
    /** 毛利率 (毛利润/销售额) */
    private BigDecimal profitRate;
}
