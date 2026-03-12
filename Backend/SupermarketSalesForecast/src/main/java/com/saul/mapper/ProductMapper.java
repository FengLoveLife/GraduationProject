package com.saul.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saul.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品 Mapper 接口
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
