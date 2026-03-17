package com.saul.sales.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 日历因子VO（返回给前端）
 */
@Data
public class CalendarFactorVO {

    private Long id;

    /** 日期 */
    private LocalDate date;

    /** 星期几 1-7(1=周一) */
    private Integer dayOfWeek;

    /** 星期文本：周一、周二... */
    private String dayOfWeekText;

    /** 是否周末: 0否 1是 */
    private Integer isWeekend;

    /** 是否节假日: 0否 1是 */
    private Integer isHoliday;

    /** 节假日名称 */
    private String holidayName;

    /** 天气 */
    private String weather;

    /** 天气图标 */
    private String weatherIcon;
}