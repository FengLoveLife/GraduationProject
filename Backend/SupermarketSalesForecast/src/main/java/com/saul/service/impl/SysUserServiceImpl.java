package com.saul.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.entity.SysUser;
import com.saul.mapper.SysUserMapper;
import com.saul.service.ISysUserService;
import org.springframework.stereotype.Service;

/**
 * SysUser 业务实现。
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
}
