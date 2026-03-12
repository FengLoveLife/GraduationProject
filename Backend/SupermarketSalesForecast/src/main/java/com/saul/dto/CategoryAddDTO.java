package com.saul.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增商品分类 DTO（接收前端 JSON）。
 */
@Data
public class CategoryAddDTO {

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /**
     * 父级分类ID（0表示顶级）
     */
    private Long parentId;

    /**
     * 层级（可不传，由后端根据 parentId 动态计算）
     */
    private Integer level;

    /**
     * 排序权重（越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;
}

