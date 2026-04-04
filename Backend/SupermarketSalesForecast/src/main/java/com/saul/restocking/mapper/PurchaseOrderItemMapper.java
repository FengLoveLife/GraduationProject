package com.saul.restocking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saul.restocking.entity.PurchaseOrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 进货单明细 Mapper
 */
@Mapper
public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {
}