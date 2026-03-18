package com.saul.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人信息 DTO
 */
@Data
public class UpdateProfileDTO {

    /**
     * 用户名（可修改）
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需在3-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /**
     * 联系电话（可选）
     */
    @Size(max = 20, message = "电话号码不能超过20个字符")
    private String phone;
}