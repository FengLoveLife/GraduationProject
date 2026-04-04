package com.saul.forecast.controller;

import com.saul.common.Result;
import com.saul.forecast.dto.ForecastQueryDTO;
import com.saul.forecast.dto.ForecastRunDTO;
import com.saul.forecast.service.IForecastResultService;
import com.saul.forecast.vo.ForecastCompareVO;
import com.saul.forecast.vo.ForecastResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 销量预测 Controller
 */
@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final IForecastResultService forecastResultService;

    /**
     * 查询预测服务状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        return Result.success(forecastResultService.getForecastServiceStatus());
    }

    /**
     * 触发批量预测
     */
    @PostMapping("/run")
    public Result<Map<String, Object>> runForecast(@RequestBody(required = false) ForecastRunDTO dto) {
        if (dto == null) {
            dto = new ForecastRunDTO();
        }
        Map<String, Object> result = forecastResultService.runBatchForecast(dto);
        return Result.success("预测完成", result);
    }

    /**
     * 查询预测结果列表
     */
    @GetMapping("/results")
    public Result<List<ForecastResultVO>> getResults(ForecastQueryDTO queryDTO) {
        if (queryDTO.getForecastDate() == null) {
            queryDTO.setForecastDate(getLatestForecastDate());
        }
        List<ForecastResultVO> results = forecastResultService.getForecastResults(queryDTO);
        return Result.success(results);
    }

    /**
     * 查询预测汇总统计
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> getSummary(
            @RequestParam(required = false) String forecastDate) {
        return Result.success(forecastResultService.getForecastSummary(forecastDate));
    }

    /**
     * 查询预测 vs 实际销量对比数据
     * @param startDate 开始日期 (YYYY-MM-DD)
     * @param endDate 结束日期 (YYYY-MM-DD)
     * @param productId 商品 ID（可选）
     * @param categoryId 分类 ID（可选）
     */
    @GetMapping("/compare")
    public Result<List<ForecastCompareVO>> getForecastVsActual(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long categoryId) {
        return Result.success(forecastResultService.getForecastVsActual(startDate, endDate, productId, categoryId));
    }

    /**
     * 查询预测 vs 实际销量趋势数据（按日期汇总）
     * @param startDate 开始日期 (YYYY-MM-DD)
     * @param endDate 结束日期 (YYYY-MM-DD)
     */
    @GetMapping("/trend-daily")
    public Result<List<Map<String, Object>>> getForecastTrend(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return Result.success(forecastResultService.getForecastTrend(startDate, endDate));
    }

    /**
     * 获取最新预测日期
     */
    private java.time.LocalDate getLatestForecastDate() {
        Map<String, Object> summary = forecastResultService.getForecastSummary(null);
        Object date = summary.get("forecastDate");
        if (date != null) {
            return java.time.LocalDate.parse(date.toString());
        }
        return java.time.LocalDate.now().plusDays(1);
    }

    }
