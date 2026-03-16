package com.saul.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 手工盘点调整 DTO
 */
@Data
public class InventoryAdjustDTO {

    /** 商品ID */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 变动类型：3-损耗盘亏, 4-手工调整 */
    @NotNull(message = "变动类型不能为空")
    private Integer type;

    /** 变动数量 (正数增加，负数减少) */
    @NotNull(message = "变动数量不能为空")
    private Integer changeAmount;

    /** 调整备注说明 */
    @NotBlank(message = "调整备注不能为空")
    private String remark;
}
