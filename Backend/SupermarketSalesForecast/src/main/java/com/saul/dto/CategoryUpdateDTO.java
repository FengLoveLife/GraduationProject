package com.saul.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改商品分类 DTO（接收前端 JSON）。
 */
@Data
public class CategoryUpdateDTO {

    /**
     * 分类ID（必填）
     */
    @NotNull(message = "分类ID不能为空")
    private Long id;

    /**
     * 分类名称（可选）
     */
    private String name;

    /**
     * 父级分类ID（可选，0表示顶级）
     */
    private Long parentId;

    /**
     * 排序权重（可选）
     */
    private Integer sortOrder;

    /**
     * 状态：1-启用，0-禁用（可选）
     */
    private Integer status;
}

