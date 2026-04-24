package com.saul.sales.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * POS 机销售订单推送 DTO
 * <p>
 * 用于接收 POS/收银系统实时推送的单笔销售数据。
 * 数据落库后与 Excel 日结导入共用同一张 sales_order 表，通过 operator=POS-XX 区分来源。
 */
@Data
public class PosOrderDTO {

    /** POS 机编号（如 POS-01），最终写入 sales_order.operator */
    private String posId;

    /** POS 端订单号（幂等键，重复推送会被忽略） */
    private String orderNo;

    /** 销售日期 yyyy-MM-dd */
    private String saleDate;

    /** 销售时间 yyyy-MM-dd HH:mm:ss */
    private String saleTime;

    /** 支付方式：1=现金 2=微信 3=支付宝 */
    private Integer paymentType;

    /** 商品明细列表 */
    private List<PosOrderItemDTO> items;

    /**
     * 订单明细子对象
     */
    @Data
    public static class PosOrderItemDTO {
        /** 商品编码（用于和系统商品表匹配） */
        private String productCode;

        /** 实际售价 */
        private BigDecimal unitPrice;

        /** 销售数量 */
        private Integer quantity;

        /** 是否促销：0=否 1=是 */
        private Integer isPromotion;
    }
}
