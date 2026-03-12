package com.saul.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saul.entity.InventoryLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存流水 Mapper 接口
 */
@Mapper
public interface InventoryLogMapper extends BaseMapper<InventoryLog> {
}
