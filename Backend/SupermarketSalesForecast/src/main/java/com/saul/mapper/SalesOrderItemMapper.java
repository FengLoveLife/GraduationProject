package com.saul.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saul.entity.SalesOrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售订单明细表 Mapper
 */
@Mapper
public interface SalesOrderItemMapper extends BaseMapper<SalesOrderItem> {
}
