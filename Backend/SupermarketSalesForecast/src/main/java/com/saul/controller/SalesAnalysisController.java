package com.saul.controller;

import com.saul.common.Result;
import com.saul.dto.SalesAnalysisQueryDTO;
import com.saul.service.ISalesAnalysisService;
import com.saul.vo.ChartDataVO;
import com.saul.vo.SalesKpiVO;
import com.saul.vo.TopProductVO;
import com.saul.vo.TrendChartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 销售统计分析 Controller
 */
@RestController
@RequestMapping("/api/sales-analysis")
@RequiredArgsConstructor
public class SalesAnalysisController {

    private final ISalesAnalysisService salesAnalysisService;

    /**
     * 获取核心 KPI 指标
     */
    @GetMapping("/kpi")
    public Result<SalesKpiVO> getKpi(@Validated SalesAnalysisQueryDTO queryDTO) {
        return Result.success(salesAnalysisService.getKpi(queryDTO));
    }

    /**
     * 获取销售趋势图表
     */
    @GetMapping("/trend")
    public Result<TrendChartVO> getTrend(@Validated SalesAnalysisQueryDTO queryDTO) {
        return Result.success(salesAnalysisService.getTrend(queryDTO));
    }

    /**
     * 获取商品销量排行 TOP10
     */
    @GetMapping("/top-products")
    public Result<List<TopProductVO>> getTopProducts(@Validated SalesAnalysisQueryDTO queryDTO) {
        return Result.success(salesAnalysisService.getTopProducts(queryDTO));
    }

    /**
     * 获取分类销售占比饼图
     */
    @GetMapping("/category-pie")
    public Result<List<ChartDataVO>> getCategoryPie(@Validated SalesAnalysisQueryDTO queryDTO) {
        return Result.success(salesAnalysisService.getCategoryPie(queryDTO));
    }

    /**
     * 获取支付方式占比饼图
     */
    @GetMapping("/payment-pie")
    public Result<List<ChartDataVO>> getPaymentPie(@Validated SalesAnalysisQueryDTO queryDTO) {
        return Result.success(salesAnalysisService.getPaymentPie(queryDTO));
    }
}
