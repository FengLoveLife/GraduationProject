package com.saul.user.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saul.user.log.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
}