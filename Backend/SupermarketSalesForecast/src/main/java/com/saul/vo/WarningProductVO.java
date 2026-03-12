package com.saul.vo;

import lombok.Data;

/**
 * 预警商品明细 VO
 */
@Data
public class WarningProductVO {
    private Long id;
    private String imageUrl;
    private String productCode;
    private String name;
    private String categoryName; // 所属分类名称
    private Integer stock;       // 当前库存
    private Integer safetyStock; // 安全库存
    private Integer gapAmount;   // 安全缺口 (stock - safetyStock)
}
