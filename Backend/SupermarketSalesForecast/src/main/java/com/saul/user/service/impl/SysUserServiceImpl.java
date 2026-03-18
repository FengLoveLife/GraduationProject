package com.saul.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.user.dto.UpdatePasswordDTO;
import com.saul.user.dto.UpdateProfileDTO;
import com.saul.user.entity.SysUser;
import com.saul.user.mapper.SysUserMapper;
import com.saul.user.service.ISysUserService;
import com.saul.user.vo.UserProfileVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * SysUser 业务实现
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Override
    public UserProfileVO getProfile(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(user, vo);

        // 状态文本转换
        vo.setStatusText("ACTIVE".equals(user.getStatus()) ? "正常" : "停用");

        return vo;
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查用户名是否被其他用户占用
        if (!user.getUsername().equals(dto.getUsername())) {
            SysUser existUser = this.lambdaQuery()
                    .eq(SysUser::getUsername, dto.getUsername())
                    .ne(SysUser::getId, userId)
                    .one();
            if (existUser != null) {
                throw new RuntimeException("用户名已被占用");
            }
        }

        user.setUsername(dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setUpdateTime(LocalDateTime.now());

        this.updateById(user);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordDTO dto) {
        // 校验新密码和确认密码一致
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("新密码与确认密码不一致");
        }

        SysUser user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 校验原密码
        if (!passwordMatches(dto.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 新密码不能与原密码相同
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new RuntimeException("新密码不能与原密码相同");
        }

        // 更新密码（使用 MD5 加密）
        String newPassword = DigestUtils.md5DigestAsHex(dto.getNewPassword().getBytes(StandardCharsets.UTF_8));
        user.setPassword(newPassword);
        user.setUpdateTime(LocalDateTime.now());

        this.updateById(user);
    }

    @Override
    public void updateLastLoginTime(Long userId) {
        SysUser user = this.getById(userId);
        if (user != null) {
            user.setLastLoginTime(LocalDateTime.now());
            this.updateById(user);
        }
    }

    /**
     * 密码匹配：支持明文和 MD5
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
}
