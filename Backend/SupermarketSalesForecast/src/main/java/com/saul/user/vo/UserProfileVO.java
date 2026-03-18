package com.saul.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息 VO（返回给前端）
 */
@Data
public class UserProfileVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 账号状态：ACTIVE / INACTIVE
     */
    private String status;

    /**
     * 状态文本：正常 / 停用
     */
    private String statusText;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}