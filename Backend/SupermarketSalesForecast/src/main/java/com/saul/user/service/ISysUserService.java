package com.saul.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.saul.user.dto.UpdatePasswordDTO;
import com.saul.user.dto.UpdateProfileDTO;
import com.saul.user.entity.SysUser;
import com.saul.user.vo.UserProfileVO;

/**
 * SysUser 业务接口
 */
public interface ISysUserService extends IService<SysUser> {

    /**
     * 获取当前用户信息
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 更新个人信息
     */
    void updateProfile(Long userId, UpdateProfileDTO dto);

    /**
     * 修改密码
     */
    void updatePassword(Long userId, UpdatePasswordDTO dto);

    /**
     * 更新最后登录时间
     */
    void updateLastLoginTime(Long userId);
}
