package com.saul.forecast.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.saul.forecast.dto.ForecastQueryDTO;
import com.saul.forecast.dto.ForecastRunDTO;
import com.saul.forecast.entity.ForecastResult;
import com.saul.forecast.vo.ForecastCompareVO;
import com.saul.forecast.vo.ForecastResultVO;

import java.util.List;
import java.util.Map;

/**
 * 销量预测结果 Service 接口
 */
public interface IForecastResultService extends IService<ForecastResult> {

    /**
     * 查询预测结果列表
     *
     * @param queryDTO 查询条件
     * @return 预测结果列表（含库存信息）
     */
    List<ForecastResultVO> getForecastResults(ForecastQueryDTO queryDTO);

    /**
     * 触发批量预测
     * 调用 Python 预测服务，将结果存入数据库
     *
     * @param dto 预测参数
     * @return 预测结果摘要
     */
    Map<String, Object> runBatchForecast(ForecastRunDTO dto);

    /**
     * 查询预测服务状态
     *
     * @return 服务状态信息
     */
    Map<String, Object> getForecastServiceStatus();

    /**
     * 获取预测汇总统计
     *
     * @param forecastDate 预测日期
     * @return 汇总统计
     */
    Map<String, Object> getForecastSummary(String forecastDate);

    /**
     * 查询预测 vs 实际销量对比数据
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param productId 商品 ID（可选）
     * @param categoryId 分类 ID（可选）
     * @return 对比数据列表
     */
    List<ForecastCompareVO> getForecastVsActual(String startDate, String endDate, Long productId, Long categoryId);

    /**
     * 获取预测 vs 实际销量趋势数据（按日期汇总）
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 按日期汇总的对比数据 { date, predictedQuantity, actualQuantity }
     */
    List<Map<String, Object>> getForecastTrend(String startDate, String endDate);

    }
