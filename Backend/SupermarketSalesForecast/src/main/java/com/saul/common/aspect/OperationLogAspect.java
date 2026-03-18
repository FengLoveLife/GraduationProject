package com.saul.common.aspect;

import com.saul.common.annotation.OperationLog;
import com.saul.user.log.service.ISysOperationLogService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 * <p>
 * 自动拦截带有 @OperationLog 注解的方法，记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final ISysOperationLogService logService;

    @Around("@annotation(com.saul.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 执行目标方法
        Object result = point.proceed();

        try {
            // 记录日志（只在方法执行成功后记录）
            recordLog(point);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }

        return result;
    }

    private void recordLog(ProceedingJoinPoint point) {
        // 获取注解信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        if (annotation == null) {
            return;
        }

        String operationType = annotation.type();
        String operationDesc = annotation.desc();

        // 尝试从参数中提取更详细的描述
        String detailDesc = extractDetailDesc(point, operationDesc);
        if (StringUtils.hasText(detailDesc)) {
            operationDesc = detailDesc;
        }

        // 获取请求信息
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }

        HttpServletRequest request = attrs.getRequest();

        // 获取当前用户信息
        Long userId = null;
        String userName = "未知用户";

        Object claimsObj = request.getAttribute("jwtClaims");
        if (claimsObj instanceof Claims claims) {
            Object userIdObj = claims.get("userId");
            if (userIdObj != null) {
                userId = ((Number) userIdObj).longValue();
            }
            Object realNameObj = claims.get("realName");
            if (realNameObj != null) {
                userName = realNameObj.toString();
            }
        }

        // 获取客户端IP
        String ipAddress = getClientIp(request);

        // 记录日志
        logService.recordLog(userId, userName, operationType, operationDesc, ipAddress);
    }

    /**
     * 从方法参数中提取更详细的描述
     */
    private String extractDetailDesc(ProceedingJoinPoint point, String baseDesc) {
        Object[] args = point.getArgs();
        if (args == null || args.length == 0) {
            return baseDesc;
        }

        // 遍历参数，尝试提取有用信息
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }

            // 跳过常见的不需要的参数类型
            if (arg instanceof MultipartFile ||
                arg instanceof HttpServletRequest ||
                arg instanceof Long ||
                arg instanceof Integer) {
                continue;
            }

            // 尝试通过反射获取 name 或 productName 字段
            try {
                Class<?> clazz = arg.getClass();

                // 尝试获取 name 字段
                try {
                    java.lang.reflect.Field nameField = clazz.getDeclaredField("name");
                    nameField.setAccessible(true);
                    Object nameValue = nameField.get(arg);
                    if (nameValue != null && StringUtils.hasText(nameValue.toString())) {
                        return baseDesc + "：" + nameValue;
                    }
                } catch (NoSuchFieldException ignored) {
                }

                // 尝试获取 productName 字段
                try {
                    java.lang.reflect.Field productNameField = clazz.getDeclaredField("productName");
                    productNameField.setAccessible(true);
                    Object productNameValue = productNameField.get(arg);
                    if (productNameValue != null && StringUtils.hasText(productNameValue.toString())) {
                        return baseDesc + "：" + productNameValue;
                    }
                } catch (NoSuchFieldException ignored) {
                }

            } catch (Exception e) {
                log.debug("提取参数信息失败", e);
            }
        }

        return baseDesc;
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