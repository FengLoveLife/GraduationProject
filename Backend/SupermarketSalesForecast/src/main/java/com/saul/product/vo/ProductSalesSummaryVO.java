package com.saul.product.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 商品销售概况视图对象
 */
@Data
public class ProductSalesSummaryVO {

    // ========== 基础汇总 ==========

    /** 总销量（件） */
    private Integer totalQuantity;

    /** 总销售额（元） */
    private BigDecimal totalAmount;

    /** 总毛利（元） */
    private BigDecimal totalProfit;

    /** 毛利率（百分比，如 0.25 表示 25%） */
    private BigDecimal profitRate;

    /** 平均售价（元） */
    private BigDecimal avgUnitPrice;

    // ========== 时间维度 ==========

    /** 首次销售日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDate firstSaleDate;

    /** 最近销售日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private LocalDate lastSaleDate;

    /** 销售天数（有销售记录的天数） */
    private Integer saleDays;

    /** 日均销量 */
    private BigDecimal dailyAvgQuantity;

    /** 日均销售额 */
    private BigDecimal dailyAvgAmount;

    // ========== 近30天趋势 ==========

    /** 近30天日期列表 */
    private List<String> recentDates;

    /** 近30天每日销量 */
    private List<Integer> recentQuantities;

    /** 近30天每日销售额 */
    private List<BigDecimal> recentAmounts;

    /** 近30天总销量 */
    private Integer recent30Quantity;

    /** 近30天总销售额 */
    private BigDecimal recent30Amount;

    // ========== 对比分析 ==========

    /** 近30天日均 vs 历史日均 增长率 */
    private BigDecimal recentVsHistoryRate;
}