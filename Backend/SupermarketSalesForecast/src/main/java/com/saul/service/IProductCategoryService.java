package com.saul.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.saul.dto.CategoryAddDTO;
import com.saul.dto.CategoryUpdateDTO;
import com.saul.entity.ProductCategory;
import com.saul.vo.CategoryTreeVO;

import java.util.List;

/**
 * 商品分类业务接口。
 */
public interface IProductCategoryService extends IService<ProductCategory> {

    /**
     * 查询分类树形列表（一次查库，内存组装）
     */
    List<CategoryTreeVO> getCategoryTree();

    /**
     * 新增商品分类（含默认值、同级防重、动态层级）
     */
    void addCategory(CategoryAddDTO dto);

    /**
     * 修改商品分类（含存在性、自环防御、同级防重、动态层级、按需更新）
     */
    void updateCategory(CategoryUpdateDTO dto);

    /**
     * 删除商品分类（含子节点与关联数据校验）
     */
    void deleteCategory(Long id);
}

