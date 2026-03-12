package com.saul.dto;

import lombok.Data;

/**
 * 商品分页查询 DTO
 */
@Data
public class ProductQueryDTO {

    /**
     * 当前页码（默认1）
     */
    private Integer page = 1;

    /**
     * 每页条数（默认10）
     */
    private Integer pageSize = 5;

    /**
     * 搜索关键词（用于模糊匹配名称 name 或 编码 productCode）
     */
    private String keyword;

    /**
     * 分类ID（精确匹配）
     */
    private Long categoryId;

    /**
     * 状态（精确匹配: 1-上架, 0-下架）
     */
    private Integer status;
}
