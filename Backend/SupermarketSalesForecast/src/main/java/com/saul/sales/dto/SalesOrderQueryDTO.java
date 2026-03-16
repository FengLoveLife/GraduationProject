package com.saul.sales.dto;

import lombok.Data;

/**
 * 销售订单查询 DTO
 */
@Data
public class SalesOrderQueryDTO {
    /** 当前页码 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    /** 订单编号（模糊搜索） */
    private String orderNo;

    /** 开始日期 yyyy-MM-dd */
    private String startDate;

    /** 结束日期 yyyy-MM-dd */
    private String endDate;
}
