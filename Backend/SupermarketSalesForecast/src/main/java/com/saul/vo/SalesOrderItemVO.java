package com.saul.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 销售订单明细 VO
 */
@Data
public class SalesOrderItemVO {
    private Long id;
    private Long orderId;
    private String productCode;
    private String productName;
    private String categoryName;
    private BigDecimal purchasePrice;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotalAmount;
    private BigDecimal subtotalProfit;
    private Integer isPromotion;
}
