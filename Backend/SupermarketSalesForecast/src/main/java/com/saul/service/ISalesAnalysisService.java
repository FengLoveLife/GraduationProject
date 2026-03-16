package com.saul.service;

import com.saul.dto.SalesAnalysisQueryDTO;
import com.saul.vo.ChartDataVO;
import com.saul.vo.SalesKpiVO;
import com.saul.vo.TopProductVO;
import com.saul.vo.TrendChartVO;

import java.util.List;

/**
 * 销售统计分析 Service 接口
 */
public interface ISalesAnalysisService {

    /**
     * 获取核心 KPI 指标
     */
    SalesKpiVO getKpi(SalesAnalysisQueryDTO queryDTO);

    /**
     * 获取销售趋势图表数据
     */
    TrendChartVO getTrend(SalesAnalysisQueryDTO queryDTO);

    /**
     * 获取商品销量排行 TOP10
     */
    List<TopProductVO> getTopProducts(SalesAnalysisQueryDTO queryDTO);

    /**
     * 获取分类销售占比饼图
     */
    List<ChartDataVO> getCategoryPie(SalesAnalysisQueryDTO queryDTO);

    /**
     * 获取支付方式占比饼图
     */
    List<ChartDataVO> getPaymentPie(SalesAnalysisQueryDTO queryDTO);
}
