package com.saul.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 商品销量排行 VO
 */
@Data
public class TopProductVO {
    /** 商品名称 */
    private String productName;
    
    /** 销售数量 */
    private Integer quantity;
    
    /** 销售额 */
    private BigDecimal amount;
}
