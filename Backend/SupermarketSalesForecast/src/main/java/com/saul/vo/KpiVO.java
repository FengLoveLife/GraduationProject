package com.saul.vo;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 库存核心 KPI 指标 VO
 */
@Data
public class KpiVO {
    /**
     * 在售商品总数 (SKU)
     */
    private Integer totalSku;

    /**
     * 库存告急 SKU 数 (0 < stock < safetyStock)
     */
    private Integer warningSku;

    /**
     * 已售罄 SKU 数 (stock == 0)
     */
    private Integer soldOutSku;

    /**
     * 当前库存总货值 (Σ purchasePrice * stock)
     */
    private BigDecimal totalValue;
}
