package com.saul.controller;

import com.saul.common.Result;
import com.saul.dto.LoginDTO;
import com.saul.entity.SysUser;
import com.saul.service.ISysUserService;
import com.saul.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员用户相关接口。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SysUserController {
    @Autowired
    private ISysUserService sysUserService;
    private final JwtUtils jwtUtils;

    /**
     * 登录接口：POST /api/login
     * <p>
     * 登录逻辑：
     * 1. 根据 username 查询用户
     * 2. 校验密码（当前支持明文/MD5；后续建议替换为 BCrypt）
     * 3. 校验账号状态必须为 ACTIVE
     * 4. 生成 JWT Token，并返回 token + 用户基本信息（不返回密码）
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO dto) {
        try {
            if (dto == null || !StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
                return Result.error(400, "用户名或密码不能为空");
            }

            SysUser user = sysUserService.lambdaQuery()
                    .eq(SysUser::getUsername, dto.getUsername().trim())
                    .one();

            if (user == null) {
                return Result.error(401, "用户名或密码错误");
            }

            if (!passwordMatches(dto.getPassword(), user.getPassword())) {
                return Result.error(401, "用户名或密码错误");
            }

            if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                return Result.error(403, "账号已被停用");
            }

            String token = jwtUtils.generateToken(user);

            // 返回给前端的用户信息：排除 password
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("realName", user.getRealName());
            userInfo.put("status", user.getStatus());

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", userInfo);

            return Result.success("登录成功", data);
        } catch (Exception ex) {
            // 兜底异常捕获：避免异常栈直接暴露给前端
            return Result.error(500, "登录失败，请稍后重试");
        }
    }

    /**
     * 密码匹配：
     * - 兼容明文存储（开发阶段）
     * - 兼容 MD5 存储（部分项目早期会这么做）
     * <p>
     * 重要：生产环境强烈建议改用 BCrypt（Spring Security 的 PasswordEncoder）。
     */
    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(storedPassword)) {
            return false;
        }

        String raw = rawPassword.trim();
        String stored = storedPassword.trim();

        // 1) 明文比对（仅演示用）
        if (stored.equals(raw)) {
            return true;
        }

        // 2) MD5 比对（仅演示用：MD5 不安全，生产环境不建议）
        String md5 = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
        return stored.equalsIgnoreCase(md5);
    }
}
