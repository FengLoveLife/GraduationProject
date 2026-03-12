package com.saul.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理员用户实体（对应数据库表：users 或 sys_user）。
 * <p>
 *
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键（自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 唯一用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码（当前项目可先明文/MD5，后续建议改为 BCrypt）
     */
    @TableField("password")
    private String password;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 账号状态：ACTIVE / INACTIVE
     */
    @TableField("status")
    private String status;
}
