package com.saul.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.user.entity.SysUser;
import com.saul.user.mapper.SysUserMapper;
import com.saul.user.service.ISysUserService;
import org.springframework.stereotype.Service;

/**
 * SysUser 业务实现。
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
}
