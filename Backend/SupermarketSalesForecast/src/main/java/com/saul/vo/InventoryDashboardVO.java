package com.saul.vo;

import lombok.Data;
import java.util.List;

/**
 * 库存看板聚合数据 VO
 */
@Data
public class InventoryDashboardVO {
    /**
     * 顶部 KPI 统计
     */
    private KpiVO kpi;

    /**
     * 库存健康度分布 (饼图数据)
     */
    private List<ChartDataVO> healthData;

    /**
     * 各分类缺货排行 (柱状图数据)
     */
    private List<ChartDataVO> categoryWarningData;
}
