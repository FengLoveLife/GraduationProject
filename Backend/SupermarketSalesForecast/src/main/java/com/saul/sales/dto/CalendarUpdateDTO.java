package com.saul.sales.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 日历因子更新参数
 */
@Data
public class CalendarUpdateDTO {

    @NotNull(message = "ID不能为空")
    private Long id;

    /** 是否节假日: 0否 1是 */
    private Integer isHoliday;

    /** 节假日名称 */
    private String holidayName;

    /** 天气: 晴/多云/雨/雪等 */
    private String weather;
}