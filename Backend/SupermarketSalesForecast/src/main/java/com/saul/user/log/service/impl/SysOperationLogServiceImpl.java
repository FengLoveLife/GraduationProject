package com.saul.user.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.saul.user.log.dto.LogQueryDTO;
import com.saul.user.log.entity.SysOperationLog;
import com.saul.user.log.mapper.SysOperationLogMapper;
import com.saul.user.log.service.ISysOperationLogService;
import com.saul.user.log.vo.OperationLogVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志 Service 实现
 */
@Service
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog> implements ISysOperationLogService {

    /**
     * 操作类型映射文本
     */
    private static final Map<String, String> TYPE_TEXT_MAP = new HashMap<>();

    static {
        TYPE_TEXT_MAP.put("LOGIN", "登录");
        TYPE_TEXT_MAP.put("PASSWORD", "密码");
        TYPE_TEXT_MAP.put("PRODUCT", "商品");
        TYPE_TEXT_MAP.put("SALES", "销售");
        TYPE_TEXT_MAP.put("INVENTORY", "库存");
    }

    @Override
    public Page<OperationLogVO> queryPage(LogQueryDTO query) {
        Page<SysOperationLog> page = new Page<>(query.getPage(), query.getSize());

        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();

        // 操作类型筛选
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(SysOperationLog::getOperationType, query.getType());
        }

        // 日期范围筛选
        if (query.getStartDate() != null) {
            wrapper.ge(SysOperationLog::getCreateTime, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.le(SysOperationLog::getCreateTime, query.getEndDate().plusDays(1).atStartOfDay());
        }

        // 按时间倒序
        wrapper.orderByDesc(SysOperationLog::getCreateTime);

        Page<SysOperationLog> result = this.page(page, wrapper);

        // 转换为 VO
        Page<OperationLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());

        return voPage;
    }

    @Override
    public void recordLog(String operationType, String operationDesc) {
        // 从当前请求上下文获取用户信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }

        HttpServletRequest request = attrs.getRequest();

        // 从 request 的 attribute 中获取 jwtClaims（由 JwtInterceptor 注入）
        var claims = request.getAttribute("jwtClaims");
        Long userId = null;
        String userName = "未知用户";

        if (claims instanceof io.jsonwebtoken.Claims jwtClaims) {
            Object userIdObj = jwtClaims.get("userId");
            if (userIdObj != null) {
                userId = ((Number) userIdObj).longValue();
            }
            Object realNameObj = jwtClaims.get("realName");
            if (realNameObj != null) {
                userName = realNameObj.toString();
            }
        }

        String ipAddress = getClientIp(request);

        recordLog(userId, userName, operationType, operationDesc, ipAddress);
    }

    @Override
    public void recordLog(Long userId, String userName, String operationType, String operationDesc, String ipAddress) {
        SysOperationLog log = new SysOperationLog();
        log.setUserId(userId);
        log.setUserName(userName);
        log.setOperationType(operationType);
        log.setOperationDesc(operationDesc);
        log.setIpAddress(ipAddress);
        log.setCreateTime(LocalDateTime.now());

        this.save(log);
    }

    /**
     * 获取客户端 IP 地址
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
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 实体转 VO
     */
    private OperationLogVO toVO(SysOperationLog entity) {
        OperationLogVO vo = new OperationLogVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setOperationTypeText(TYPE_TEXT_MAP.getOrDefault(entity.getOperationType(), entity.getOperationType()));
        return vo;
    }
}