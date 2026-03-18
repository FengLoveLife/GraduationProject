package com.saul.user.log.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 操作日志查询 DTO
 */
@Data
public class LogQueryDTO {

    /**
     * 当前页
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 操作类型：LOGIN/PASSWORD/PRODUCT/SALES/INVENTORY
     */
    private String type;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;
}