package com.saul.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类树返回 VO。
 * <p>
 * 说明：实体类不建议直接返回给前端；并且实体类也不应该额外新增 children 字段，
 * 因此单独定义 VO 用于树形结构输出。
 */
@Data
public class CategoryTreeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private Integer sortOrder;
    private Integer status;

    /**
     * 子分类列表（树形结构）
     */
    private List<CategoryTreeVO> children = new ArrayList<>();
}

