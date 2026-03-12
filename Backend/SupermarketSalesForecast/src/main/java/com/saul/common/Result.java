package com.saul.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一返回结果封装。
 * <p>
 * 企业项目中建议所有接口都返回统一结构，便于前端统一处理：
 * code：业务状态码；msg：提示信息；data：返回数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /**
     * 状态码：
     * - 200：成功
     * - 401：未登录/Token 无效
     * - 500：服务器内部错误
     */
    private Integer code;

    /**
     * 提示信息（可直接展示给用户或用于前端 toast）
     */
    private String msg;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 成功（不返回数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    /**
     * 成功（返回数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * 成功（自定义提示信息 + 返回数据）
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    /**
     * 失败（默认 500）
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }

    /**
     * 失败（自定义状态码与提示信息）
     */
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}
