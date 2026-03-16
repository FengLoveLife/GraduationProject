package com.saul.inventory.dto;

import lombok.Data;

/**
 * 库存流水查询 DTO
 */
@Data
public class InventoryLogQueryDTO {
    /** 当前页码 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    /** 搜索关键词（商品名称或编码） */
    private String keyword;

    /** 变动类型 */
    private Integer type;

    /** 开始日期 yyyy-MM-dd */
    private String startDate;

    /** 结束日期 yyyy-MM-dd */
    private String endDate;
}
