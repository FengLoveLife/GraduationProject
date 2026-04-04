package com.saul.restocking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 进货单主表实体
 */
@Data
@TableName("purchase_order")
public class PurchaseOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 进货单号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 商品总数量
     */
    @TableField("total_quantity")
    private Integer totalQuantity;

    /**
     * 进货总金额
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 下单日期
     */
    @TableField("order_date")
    private LocalDate orderDate;

    /**
     * 预计到货日期
     */
    @TableField("expected_date")
    private LocalDate expectedDate;

    /**
     * 实际到货日期
     */
    @TableField("actual_arrival_date")
    private LocalDate actualArrivalDate;

    /**
     * 状态：0-待确认，1-已下单，2-已完成，3-已取消
     */
    @TableField("status")
    private Integer status;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 操作人
     */
    @TableField("operator")
    private String operator;

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