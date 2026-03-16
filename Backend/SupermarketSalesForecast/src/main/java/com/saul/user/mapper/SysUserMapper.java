package com.saul.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saul.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * SysUser 数据访问层。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
