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
 * 进货建议实体（对应表：purchase_suggestion）
 */
@Data
@TableName("purchase_suggestion")
public class PurchaseSuggestion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 商品编码（冗余）
     */
    @TableField("product_code")
    private String productCode;

    /**
     * 商品名称（冗余）
     */
    @TableField("product_name")
    private String productName;

    /**
     * 分类名称（冗余）
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * 进货单价（冗余）
     */
    @TableField("purchase_price")
    private BigDecimal purchasePrice;

    /**
     * 预测销量
     */
    @TableField("predicted_quantity")
    private Integer predictedQuantity;

    /**
     * 日均销量（平滑后）
     */
    @TableField("daily_sales")
    private BigDecimal dailySales;

    /**
     * 当前库存
     */
    @TableField("current_stock")
    private Integer currentStock;

    /**
     * 安全库存
     */
    @TableField("safety_stock")
    private Integer safetyStock;

    /**
     * 目标库存
     */
    @TableField("target_stock")
    private Integer targetStock;

    /**
     * 灯位状态：1-红灯（必须补货），2-黄灯（顺带补货）
     */
    @TableField("light_status")
    private Integer lightStatus;

    /**
     * 系统建议进货量
     */
    @TableField("suggested_quantity")
    private Integer suggestedQuantity;

    /**
     * 用户调整后的进货量
     */
    @TableField("adjusted_quantity")
    private Integer adjustedQuantity;

    /**
     * 最终进货量
     */
    @TableField("final_quantity")
    private Integer finalQuantity;

    /**
     * 状态：0-待处理，1-已生成进货单，2-已忽略
     */
    @TableField("status")
    private Integer status;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}