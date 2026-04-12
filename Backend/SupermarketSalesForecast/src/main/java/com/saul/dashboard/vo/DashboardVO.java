package com.saul.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 首页控制台聚合数据 VO
 */
@Data
public class DashboardVO {

    // ========== KPI 数据 ==========

    /** 今日销售额 */
    private BigDecimal todaySales;

    /** 销售额环比增长率 (%) */
    private BigDecimal salesGrowth;

    /** 今日订单数 */
    private Integer todayOrders;

    /** 订单数环比增长率 (%) */
    private BigDecimal orderGrowth;

    /** 库存告急预警商品数 */
    private Integer stockWarning;

    /** 待处理进货单数 */
    private Integer pendingPurchase;

    // ========== 图表数据 ==========

    /** 近7天销售趋势数据 */
    private List<TrendDataVO> salesTrend;

    /** 今日品类销量占比 */
    private List<CategoryPieVO> categoryPie;

    /**
     * 销售趋势数据项
     */
    @Data
    public static class TrendDataVO {
        /** 日期 (MM-DD格式) */
        private String date;
        /** 实际销售额 */
        private BigDecimal actualAmount;
        /** 预测销售额 (仅未来日期有值) */
        private BigDecimal predictedAmount;
        /** 是否为预测数据 */
        private Boolean isPrediction;
    }

    /**
     * 品类饼图数据项
     */
    @Data
    public static class CategoryPieVO {
        /** 品类名称 */
        private String name;
        /** 销量 */
        private Integer value;
        /** 数据来源标签：今日 / 昨日 / 近7天（仅第一条携带，供前端读取） */
        private String label;
    }
}