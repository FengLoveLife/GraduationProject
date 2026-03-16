package com.saul.sales.entity;

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
 * 销售订单主表实体
 */
@Data
@TableName("sales_order")
public class SalesOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 订单总销售金额 */
    private BigDecimal totalAmount;

    /** 商品总数量 */
    private Integer totalQuantity;

    /** 支付方式：1现金 2微信 3支付宝 */
    private Integer paymentType;

    /** 销售日期 */
    private LocalDate saleDate;

    /** 销售时间 */
    private LocalDateTime saleTime;

    /** 操作员/收银员 */
    private String operator;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
