package com.saul.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.saul.dto.ProductAddDTO;
import com.saul.dto.ProductQueryDTO;
import com.saul.dto.ProductUpdateDTO;
import com.saul.entity.Product;
import com.saul.vo.ProductVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 商品 Service 接口
 */
public interface IProductService extends IService<Product> {

    /**
     * 分页查询商品
     */
    Page<ProductVO> queryPage(ProductQueryDTO queryDTO);

    /**
     * 导出商品 Excel
     */
    void exportProduct(ProductQueryDTO queryDTO, String fields, HttpServletResponse response);

    /**
     * 根据ID获取商品详情
     */
    ProductVO getDetailById(Long id);

    /**
     * 新增商品
     */
    void addProduct(ProductAddDTO addDTO);

    /**
     * 修改商品
     */
    void updateProduct(ProductUpdateDTO updateDTO);

    /**
     * 删除商品
     */
    void deleteProduct(Long id);
}
