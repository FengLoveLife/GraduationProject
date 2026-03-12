package com.saul.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售订单明细表实体
 */
@Data
@TableName("sales_order_item")
public class SalesOrderItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联订单主表ID */
    private Long orderId;

    /** 订单编号(冗余) */
    private String orderNo;

    /** 商品ID */
    private Long productId;

    /** 商品编码(冗余) */
    private String productCode;

    /** 商品名称(冗余) */
    private String productName;

    /** 分类ID(冗余) */
    private Long categoryId;

    /** 分类名称(冗余) */
    private String categoryName;

    /** 进货单价/成本价(快照) */
    private BigDecimal purchasePrice;

    /** 实际销售单价 */
    private BigDecimal unitPrice;

    /** 销售数量 */
    private Integer quantity;

    /** 小计销售额 (单价*数量) */
    private BigDecimal subtotalAmount;

    /** 小计毛利润 ((售价-成本)*数量) */
    private BigDecimal subtotalProfit;

    /** 是否促销特价: 0否, 1是 */
    private Integer isPromotion;

    /** 销售日期(冗余,便于AI聚合) */
    private LocalDate saleDate;

    /** 创建时间 */
    private LocalDateTime createTime;
}
