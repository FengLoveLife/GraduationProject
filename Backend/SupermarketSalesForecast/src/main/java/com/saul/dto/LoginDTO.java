package com.saul.dto;

import lombok.Data;

/**
 * 登录请求 DTO（接收前端传参）。
 */
@Data
public class LoginDTO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
