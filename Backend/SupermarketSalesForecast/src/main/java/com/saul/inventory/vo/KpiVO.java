package com.saul.inventory.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 库存核心 KPI 指标 VO
 */
@Data
public class KpiVO {
    private Integer totalSku;    // 在售商品总数
    private Integer warningSku;  // 库存告急 SKU 数 (0 < stock < safetyStock)
    private Integer soldOutSku;  // 已售罄 SKU 数 (stock == 0)
    private BigDecimal totalValue; // 当前库存总货值
}
