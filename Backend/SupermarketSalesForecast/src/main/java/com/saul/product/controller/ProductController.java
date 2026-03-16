package com.saul.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saul.common.Result;
import com.saul.product.dto.ProductAddDTO;
import com.saul.product.dto.ProductQueryDTO;
import com.saul.product.dto.ProductUpdateDTO;
import com.saul.product.service.IProductService;
import com.saul.product.vo.ProductVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 商品管理 Controller
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;

    /**
     * 导出商品 Excel
     */
    @GetMapping("/export")
    public void export(ProductQueryDTO queryDTO, @RequestParam(value = "fields", required = false) String fields, HttpServletResponse response) {
        productService.exportProduct(queryDTO, fields, response);
    }

    /**
     * 分页查询商品
     */
    @GetMapping("/page")
    public Result<Page<ProductVO>> page(ProductQueryDTO queryDTO) {
        Page<ProductVO> result = productService.queryPage(queryDTO);
        return Result.success(result);
    }

    /**
     * 根据ID获取详情
     */
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        ProductVO detail = productService.getDetailById(id);
        return Result.success(detail);
    }

    /**
     * 新增商品
     */
    @PostMapping("/")
    public Result<Void> add(@Validated @RequestBody ProductAddDTO addDTO) {
        productService.addProduct(addDTO);
        return Result.success();
    }

    /**
     * 修改商品
     */
    @PutMapping("/")
    public Result<Void> update(@Validated @RequestBody ProductUpdateDTO updateDTO) {
        productService.updateProduct(updateDTO);
        return Result.success();
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }
}
