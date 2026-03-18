package com.saul.user.log.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.saul.user.log.dto.LogQueryDTO;
import com.saul.user.log.entity.SysOperationLog;
import com.saul.user.log.vo.OperationLogVO;

/**
 * 操作日志 Service 接口
 */
public interface ISysOperationLogService extends IService<SysOperationLog> {

    /**
     * 分页查询操作日志
     */
    Page<OperationLogVO> queryPage(LogQueryDTO query);

    /**
     * 记录操作日志
     */
    void recordLog(String operationType, String operationDesc);

    /**
     * 记录操作日志（带用户信息）
     */
    void recordLog(Long userId, String userName, String operationType, String operationDesc, String ipAddress);
}