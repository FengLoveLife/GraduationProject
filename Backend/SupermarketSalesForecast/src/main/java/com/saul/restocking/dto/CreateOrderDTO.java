package com.saul.restocking.dto;

import lombok.Data;

import java.util.List;

/**
 * 创建进货单请求 DTO
 */
@Data
public class CreateOrderDTO {

    /**
     * 预计到货日期
     */
    private String expectedDate;

    /**
     * 备注
     */
    private String remark;

    /**
     * 进货建议ID列表（要生成进货单的建议）
     */
    private List<Long> suggestionIds;

    /**
     * 商品进货数量列表（可调整）
     */
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        /**
         * 进货建议ID
         */
        private Long suggestionId;

        /**
         * 商品ID
         */
        private Long productId;

        /**
         * 进货数量
         */
        private Integer quantity;
    }
}