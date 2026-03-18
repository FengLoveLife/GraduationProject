package com.saul.user.controller;

import com.saul.common.Result;
import com.saul.common.annotation.OperationLog;
import com.saul.user.dto.LoginDTO;
import com.saul.user.dto.UpdatePasswordDTO;
import com.saul.user.dto.UpdateProfileDTO;
import com.saul.user.entity.SysUser;
import com.saul.user.log.service.ISysOperationLogService;
import com.saul.user.service.ISysUserService;
import com.saul.common.utils.JwtUtils;
import com.saul.user.vo.UserProfileVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户相关接口
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SysUserController {

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ISysOperationLogService logService;

    private final JwtUtils jwtUtils;

    /**
     * 登录接口（特殊接口，手动记录日志，不使用AOP）
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO dto, HttpServletRequest request) {
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

            // 更新最后登录时间
            sysUserService.updateLastLoginTime(user.getId());

            // 手动记录登录日志（登录接口特殊，此时还没有Token，AOP切面无法获取用户信息）
            String ipAddress = getClientIp(request);
            logService.recordLog(user.getId(), user.getRealName(), "LOGIN", "登录系统成功", ipAddress);

            // 返回用户信息（不包含密码）
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
            return Result.error(500, "登录失败，请稍后重试");
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user/profile")
    public Result<UserProfileVO> getProfile(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        UserProfileVO profile = sysUserService.getProfile(userId);
        return Result.success(profile);
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/user/profile")
    @OperationLog(type = "PASSWORD", desc = "修改个人信息")
    public Result<Void> updateProfile(HttpServletRequest request, @Valid @RequestBody UpdateProfileDTO dto) {
        Long userId = getCurrentUserId(request);
        sysUserService.updateProfile(userId, dto);

        // 更新成功后，同步更新 localStorage 中的用户名（前端处理）
        return Result.success("修改成功", null);
    }

    /**
     * 修改密码
     */
    @PutMapping("/user/password")
    @OperationLog(type = "PASSWORD", desc = "修改密码成功")
    public Result<Void> updatePassword(HttpServletRequest request, @Valid @RequestBody UpdatePasswordDTO dto) {
        Long userId = getCurrentUserId(request);
        sysUserService.updatePassword(userId, dto);
        return Result.success("密码修改成功，请重新登录", null);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Claims claims = (Claims) request.getAttribute("jwtClaims");
        if (claims == null) {
            throw new RuntimeException("未登录");
        }
        Object userIdObj = claims.get("userId");
        if (userIdObj == null) {
            throw new RuntimeException("用户信息异常");
        }
        return ((Number) userIdObj).longValue();
    }

    /**
     * 密码匹配
     */
    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(storedPassword)) {
            return false;
        }

        String raw = rawPassword.trim();
        String stored = storedPassword.trim();

        // 明文比对
        if (stored.equals(raw)) {
            return true;
        }

        // MD5 比对
        String md5 = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
        return stored.equalsIgnoreCase(md5);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
