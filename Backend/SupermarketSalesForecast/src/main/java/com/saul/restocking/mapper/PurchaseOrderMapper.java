package com.saul.restocking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saul.restocking.entity.PurchaseOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 进货单 Mapper
 */
@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {
}