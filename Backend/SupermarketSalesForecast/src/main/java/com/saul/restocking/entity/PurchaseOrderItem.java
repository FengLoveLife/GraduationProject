package com.saul.restocking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 进货单明细实体
 */
@Data
@TableName("purchase_order_item")
public class PurchaseOrderItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 进货单ID
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 进货单号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 商品ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 商品编码
     */
    @TableField("product_code")
    private String productCode;

    /**
     * 商品名称
     */
    @TableField("product_name")
    private String productName;

    /**
     * 分类名称
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * 进货单价
     */
    @TableField("purchase_price")
    private BigDecimal purchasePrice;

    /**
     * 进货数量
     */
    @TableField("quantity")
    private Integer quantity;

    /**
     * 小计金额
     */
    @TableField("subtotal_amount")
    private BigDecimal subtotalAmount;

    /**
     * 关联的进货建议ID
     */
    @TableField("suggestion_id")
    private Long suggestionId;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}