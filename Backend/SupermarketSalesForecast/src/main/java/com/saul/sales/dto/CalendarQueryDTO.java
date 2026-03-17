package com.saul.sales.dto;

import lombok.Data;

/**
 * 日历查询参数
 */
@Data
public class CalendarQueryDTO {

    private Integer year;

    private Integer month;
}