package com.saul.forecast.dto;

import lombok.Data;

/**
 * 批量预测请求 DTO
 */
@Data
public class ForecastRunDTO {

    /** 预测起始日期，格式YYYY-MM-DD，默认为最新销售日期+1天 */
    private String forecastStart;

    /** 预测天数，1-30 */
    private Integer forecastDays = 7;
}