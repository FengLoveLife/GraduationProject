package com.saul.controller;

import com.saul.common.Result;
import com.saul.dto.CategoryAddDTO;
import com.saul.dto.CategoryUpdateDTO;
import com.saul.service.IProductCategoryService;
import com.saul.vo.CategoryTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
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
    public Result<String> add(@Valid @RequestBody CategoryAddDTO dto) {
        try {
            productCategoryService.addCategory(dto);
            return Result.<String>success("添加分类成功", null);
        } catch (Exception ex) {
            return Result.error(ex.getMessage() == null ? "添加分类失败" : ex.getMessage());
        }
    }

    /**
     * 修改分类：PUT /api/category
     */
    @PutMapping
    public Result<String> update(@RequestBody @Validated CategoryUpdateDTO dto) {
        try {
            productCategoryService.updateCategory(dto);
            return Result.<String>success("修改分类成功", null);
        } catch (Exception ex) {
            return Result.error(ex.getMessage() == null ? "修改分类失败" : ex.getMessage());
        }
    }

    /**
     * 删除分类：DELETE /api/category/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable("id") Long id) {
        try {
            productCategoryService.deleteCategory(id);
            return Result.<String>success("删除分类成功", null);
        } catch (Exception ex) {
            return Result.error(ex.getMessage() == null ? "删除分类失败" : ex.getMessage());
        }
    }

    /**
     * 参数校验异常：将校验错误转换为统一 Result 返回，便于前端直接展示。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidException(MethodArgumentNotValidException ex) {
        FieldError fe = ex.getBindingResult().getFieldError();
        String msg = (fe == null) ? "参数校验失败" : fe.getDefaultMessage();
        return Result.error(400, msg);
    }
}

