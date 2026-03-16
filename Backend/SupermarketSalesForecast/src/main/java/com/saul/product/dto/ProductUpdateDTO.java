package com.saul.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 修改商品 DTO
 * 注意：为了保证库存安全，该 DTO 移除了 stock 字段，禁止通过编辑接口直接修改当前库存。
 */
@Data
public class ProductUpdateDTO {

    @NotNull(message = "商品ID不能为空")
    private Long id;

    private Long categoryId;

    private String productCode;

    private String name;

    private String specification;

    private String unit;

    private String imageUrl;

    @Min(value = 0, message = "进货价不能为负数")
    private BigDecimal purchasePrice;

    @Min(value = 0, message = "零售价不能为负数")
    private BigDecimal salePrice;

    @Min(value = 0, message = "安全库存不能为负数")
    private Integer safetyStock;

    private Integer status;
}
