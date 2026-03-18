package com.saul.user.log.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志 VO（返回给前端）
 */
@Data
public class OperationLogVO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 操作用户名
     */
    private String userName;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作类型文本
     */
    private String operationTypeText;

    /**
     * 操作描述
     */
    private String operationDesc;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 操作时间
     */
    private LocalDateTime createTime;
}