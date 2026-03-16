package com.saul.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新增商品 DTO
 */
@Data
public class ProductAddDTO {

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotBlank(message = "商品编码不能为空")
    private String productCode;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String specification;

    @NotBlank(message = "计价单位不能为空")
    private String unit;

    private String imageUrl;

    @NotNull(message = "进货价不能为空")
    @Min(value = 0, message = "进货价不能为负数")
    private BigDecimal purchasePrice;

    @NotNull(message = "零售价不能为空")
    @Min(value = 0, message = "零售价不能为负数")
    private BigDecimal salePrice;

    @NotNull(message = "库存量不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    @Min(value = 0, message = "安全库存不能为负数")
    private Integer safetyStock;

    private Integer status;
}
