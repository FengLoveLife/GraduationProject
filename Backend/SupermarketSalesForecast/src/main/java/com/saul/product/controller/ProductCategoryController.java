package com.saul.product.controller;

import com.saul.common.Result;
import com.saul.product.dto.CategoryAddDTO;
import com.saul.product.dto.CategoryUpdateDTO;
import com.saul.product.service.IProductCategoryService;
import com.saul.product.vo.CategoryTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类相关接口。
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final IProductCategoryService productCategoryService;

    /**
     * 查询分类树形列表：GET /api/category/tree
     */
    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> tree() {
        List<CategoryTreeVO> tree = productCategoryService.getCategoryTree();
        return Result.success(tree);
    }

    /**
     * 新增分类：POST /api/category
     */
    @PostMapping
    public Result<String> add(@Validated @RequestBody CategoryAddDTO dto) {
        productCategoryService.addCategory(dto);
        return Result.<String>success("添加分类成功", null);
    }

    /**
     * 修改分类：PUT /api/category
     */
    @PutMapping
    public Result<String> update(@Validated @RequestBody CategoryUpdateDTO dto) {
        productCategoryService.updateCategory(dto);
        return Result.<String>success("修改分类成功", null);
    }

    /**
     * 删除分类：DELETE /api/category/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable("id") Long id) {
        productCategoryService.deleteCategory(id);
        return Result.<String>success("删除分类成功", null);
    }
}
