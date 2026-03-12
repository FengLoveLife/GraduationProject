package com.saul.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saul.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售订单主表 Mapper
 */
@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {
}
