package com.saul.restocking.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 进货建议VO（返回给前端）
 */
@Data
public class PurchaseSuggestionVO {

    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 进货单价
     */
    private BigDecimal purchasePrice;

    /**
     * 预测销量
     */
    private Integer predictedQuantity;

    /**
     * 日均销量（平滑后）
     */
    private BigDecimal dailySales;

    /**
     * 当前库存
     */
    private Integer currentStock;

    /**
     * 安全库存
     */
    private Integer safetyStock;

    /**
     * 目标库存
     */
    private Integer targetStock;

    /**
     * 灯位状态：1-红灯，2-黄灯
     */
    private Integer lightStatus;

    /**
     * 灯位文本：红灯/黄灯
     */
    private String lightStatusText;

    /**
     * 系统建议进货量
     */
    private Integer suggestedQuantity;

    /**
     * 用户调整后的进货量
     */
    private Integer adjustedQuantity;

    /**
     * 最终进货量
     */
    private Integer finalQuantity;

    /**
     * 状态：0-待处理，1-已生成进货单，2-已忽略
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
     * 创建时间
     */
    private LocalDateTime createTime;
}