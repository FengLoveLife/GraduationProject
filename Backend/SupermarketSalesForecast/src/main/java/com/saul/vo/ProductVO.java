package com.saul.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品信息视图对象
 */
@Data
public class ProductVO {

    private Long id;
    private Long categoryId;
    private String productCode;
    private String name;
    private String specification;
    private String unit;
    private String imageUrl;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private Integer stock;
    private Integer safetyStock;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;
}
