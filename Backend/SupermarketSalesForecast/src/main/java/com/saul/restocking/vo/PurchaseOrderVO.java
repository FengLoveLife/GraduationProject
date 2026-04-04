package com.saul.restocking.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 进货单VO
 */
@Data
public class PurchaseOrderVO {

    private Long id;

    /**
     * 进货单号
     */
    private String orderNo;

    /**
     * 商品总数量
     */
    private Integer totalQuantity;

    /**
     * 进货总金额
     */
    private BigDecimal totalAmount;

    /**
     * 下单日期
     */
    private LocalDate orderDate;

    /**
     * 预计到货日期
     */
    private LocalDate expectedDate;

    /**
     * 实际到货日期
     */
    private LocalDate actualArrivalDate;

    /**
     * 状态：0-待确认，1-已下单，2-已完成，3-已取消
     */
    private Integer status;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 进货明细列表
     */
    private List<OrderItemVO> items;

    /**
     * 进货明细VO
     */
    @Data
    public static class OrderItemVO {
        private Long id;
        private Long productId;
        private String productCode;
        private String productName;
        private String categoryName;
        private BigDecimal purchasePrice;
        private Integer quantity;
        private BigDecimal subtotalAmount;
    }
}